package fr.free.nrw.commons.upload;

import static fr.free.nrw.commons.di.NetworkingModule.NAMED_COMMONS_CSRF;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;
import com.google.gson.Gson;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.contributions.ChunkInfo;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.upload.UploadService.NotificationUpdateProgressListener;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.mwapi.MwException;
import timber.log.Timber;

@Singleton
public class UploadClient {

  private final int CHUNK_SIZE = 1024 * 1024; // 256 KB

  //This is maximum duration for which a stash is persisted on MediaWiki
  // https://www.mediawiki.org/wiki/Manual:$wgUploadStashMaxAge
  private final int MAX_CHUNK_AGE = 6 * 3600 * 1000; // 6 hours

  private final UploadInterface uploadInterface;
  private final CsrfTokenClient csrfTokenClient;
  private final PageContentsCreator pageContentsCreator;
  private final FileUtilsWrapper fileUtilsWrapper;
  private final Gson gson;

  private Map<String, Boolean> pauseUploads;

  private final CompositeDisposable compositeDisposable = new CompositeDisposable();

  @Inject
  public UploadClient(final UploadInterface uploadInterface,
      @Named(NAMED_COMMONS_CSRF) final CsrfTokenClient csrfTokenClient,
      final PageContentsCreator pageContentsCreator,
      final FileUtilsWrapper fileUtilsWrapper, final Gson gson) {
    this.uploadInterface = uploadInterface;
    this.csrfTokenClient = csrfTokenClient;
    this.pageContentsCreator = pageContentsCreator;
    this.fileUtilsWrapper = fileUtilsWrapper;
    this.gson = gson;
    this.pauseUploads = new HashMap<>();
  }

  /**
   * Upload file to stash in chunks of specified size. Uploading files in chunks will make handling
   * of large files easier. Also, it will be useful in supporting pause/resume of uploads
   */
  Observable<StashUploadResult> uploadFileToStash(
      final Context context, final String filename, final Contribution contribution,
      final NotificationUpdateProgressListener notificationUpdater) throws IOException {
    if (contribution.getChunkInfo() != null
        && contribution.getChunkInfo().getTotalChunks() == contribution.getChunkInfo()
        .getIndexOfNextChunkToUpload()) {
      return Observable.just(new StashUploadResult(StashUploadState.SUCCESS,
          contribution.getChunkInfo().getUploadResult().getFilekey()));
    }

    pauseUploads.put(contribution.getPageId(), false);

    final File file = new File(contribution.getLocalUri().getPath());
    final List<File> fileChunks = fileUtilsWrapper.getFileChunks(context, file, CHUNK_SIZE);

    final int totalChunks = fileChunks.size();

    final MediaType mediaType = MediaType
        .parse(FileUtils.getMimeType(context, Uri.parse(file.getPath())));

    final AtomicReference<ChunkInfo> chunkInfo = new AtomicReference<>();
    if (isStashValid(contribution)) {
      chunkInfo.set(contribution.getChunkInfo());

      Timber.d("Chunk: Next Chunk: %s, Total Chunks: %s",
          contribution.getChunkInfo().getIndexOfNextChunkToUpload(),
          contribution.getChunkInfo().getTotalChunks());
    }

    final AtomicInteger index = new AtomicInteger();
    final AtomicBoolean failures = new AtomicBoolean();

    compositeDisposable.add(Observable.fromIterable(fileChunks).forEach(chunkFile -> {
      if (pauseUploads.get(contribution.getPageId()) || failures.get()) {
        return;
      }

      if (chunkInfo.get() != null && index.get() < chunkInfo.get().getIndexOfNextChunkToUpload()) {
        index.incrementAndGet();
        Timber.d("Chunk: Increment and return: %s", index.get());
        return;
      }
      index.getAndIncrement();
      final int offset =
          chunkInfo.get() != null ? chunkInfo.get().getUploadResult().getOffset() : 0;

      Timber.d("Chunk: Sending Chunk number: %s, offset: %s", index.get(), offset);
      final String filekey =
          chunkInfo.get() != null ? chunkInfo.get().getUploadResult().getFilekey() : null;

      final RequestBody requestBody = RequestBody
          .create(mediaType, chunkFile);
      final CountingRequestBody countingRequestBody = new CountingRequestBody(requestBody,
          notificationUpdater::onProgress, offset,
          file.length());

      compositeDisposable.add(uploadChunkToStash(filename,
          file.length(),
          offset,
          filekey,
          countingRequestBody).subscribe(uploadResult -> {
        Timber.d("Chunk: Received Chunk number: %s, offset: %s", index.get(),
            uploadResult.getOffset());
        chunkInfo.set(
            new ChunkInfo(uploadResult, index.get(), totalChunks));
        notificationUpdater.onChunkUploaded(contribution, chunkInfo.get());
      }, throwable -> {
            Timber.e(throwable, "Received error in chunk upload");
        failures.set(true);
      }));
    }));

    if (pauseUploads.get(contribution.getPageId())) {
      Timber.d("Upload stash paused %s", contribution.getPageId());
      return Observable.just(new StashUploadResult(StashUploadState.PAUSED, null));
    } else if (failures.get()) {
      Timber.d("Upload stash contains failures %s", contribution.getPageId());
      return Observable.just(new StashUploadResult(StashUploadState.FAILED, null));
    } else if (chunkInfo.get() != null) {
      Timber.d("Upload stash success %s", contribution.getPageId());
      return Observable.just(new StashUploadResult(StashUploadState.SUCCESS,
          chunkInfo.get().getUploadResult().getFilekey()));
    } else {
      Timber.d("Upload stash failed %s", contribution.getPageId());
      return Observable.just(new StashUploadResult(StashUploadState.FAILED, null));
    }
  }

  /**
   * Stash is valid for 6 hours. This function checks the validity of stash
   * @param contribution
   * @return
   */
  private boolean isStashValid(Contribution contribution) {
    return contribution.getChunkInfo() != null &&
        contribution.getDateModified()
            .after(new Date(System.currentTimeMillis() - MAX_CHUNK_AGE));
  }

  /**
   * Uploads a file chunk to stash
   *
   * @param filename            The name of the file being uploaded
   * @param fileSize            The total size of the file
   * @param offset              The offset returned by the previous chunk upload
   * @param fileKey             The filekey returned by the previous chunk upload
   * @param countingRequestBody Request body with chunk file
   * @return
   */
  Observable<UploadResult> uploadChunkToStash(final String filename,
      final long fileSize,
      final long offset,
      final String fileKey,
      final CountingRequestBody countingRequestBody) {
    final MultipartBody.Part filePart = MultipartBody.Part
        .createFormData("chunk", filename, countingRequestBody);
    try {
      return uploadInterface.uploadFileToStash(toRequestBody(filename),
          toRequestBody(String.valueOf(fileSize)),
          toRequestBody(String.valueOf(offset)),
          toRequestBody(fileKey),
          toRequestBody(csrfTokenClient.getTokenBlocking()),
          filePart)
          .map(UploadResponse::getUpload);
    } catch (final Throwable throwable) {
      Timber.e(throwable, "Failed to upload chunk to stash");
      return Observable.error(throwable);
    }
  }

  /**
   * Dispose the active disposable and sets the pause variable
   * @param pageId
   */
  public void pauseUpload(String pageId) {
    pauseUploads.put(pageId, true);
    if (!compositeDisposable.isDisposed()) {
      compositeDisposable.dispose();
    }
    compositeDisposable.clear();
  }

  /**
   * Converts string value to request body
   */
  @Nullable
  private RequestBody toRequestBody(@Nullable final String value) {
    return value == null ? null : RequestBody.create(okhttp3.MultipartBody.FORM, value);
  }


  Observable<UploadResult> uploadFileFromStash(final Context context,
      final Contribution contribution,
      final String uniqueFileName,
      final String fileKey) {
    try {
      return uploadInterface
          .uploadFileFromStash(csrfTokenClient.getTokenBlocking(),
              pageContentsCreator.createFrom(contribution),
              CommonsApplication.DEFAULT_EDIT_SUMMARY,
              uniqueFileName,
              fileKey).map(uploadResponse -> {
            UploadResponse uploadResult = gson.fromJson(uploadResponse, UploadResponse.class);
            if (uploadResult.getUpload() == null) {
              final MwException exception = gson.fromJson(uploadResponse, MwException.class);
              Timber.e(exception, "Error in uploading file from stash");
              throw new RuntimeException(exception.getErrorCode());
            }
            return uploadResult.getUpload();
          });
    } catch (final Throwable throwable) {
      Timber.e(throwable, "Exception occurred in uploading file from stash");
      return Observable.error(throwable);
    }
  }
}
