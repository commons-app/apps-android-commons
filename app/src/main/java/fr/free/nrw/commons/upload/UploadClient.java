package fr.free.nrw.commons.upload;

import static fr.free.nrw.commons.di.NetworkingModule.NAMED_COMMONS_CSRF;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.contributions.ChunkInfo;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.upload.UploadService.NotificationUpdateProgressListener;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.wikipedia.csrf.CsrfTokenClient;
import timber.log.Timber;

@Singleton
public class UploadClient {

  private final int CHUNK_SIZE = 256 * 1024; // 256 KB

  //This is maximum duration for which a stash is persisted on MediaWiki
  // https://www.mediawiki.org/wiki/Manual:$wgUploadStashMaxAge
  private final int MAX_CHUNK_AGE = 6 * 3600 * 1000; // 6 hours

  private final UploadInterface uploadInterface;
  private final CsrfTokenClient csrfTokenClient;
  private final PageContentsCreator pageContentsCreator;
  private final FileUtilsWrapper fileUtilsWrapper;

  private final CompositeDisposable compositeDisposable = new CompositeDisposable();

  @Inject
  public UploadClient(final UploadInterface uploadInterface,
      @Named(NAMED_COMMONS_CSRF) final CsrfTokenClient csrfTokenClient,
      final PageContentsCreator pageContentsCreator,
      final FileUtilsWrapper fileUtilsWrapper) {
    this.uploadInterface = uploadInterface;
    this.csrfTokenClient = csrfTokenClient;
    this.pageContentsCreator = pageContentsCreator;
    this.fileUtilsWrapper = fileUtilsWrapper;
  }

  /**
   * Upload file to stash in chunks of specified size. Uploading files in chunks will make handling
   * of large files easier. Also, it will be useful in supporting pause/resume of uploads
   */
  Observable<UploadResult> uploadFileToStash(
      final Context context, final String filename, final Contribution contribution,
      final NotificationUpdateProgressListener notificationUpdater) throws IOException {
    File file = new File(contribution.getLocalUri().getPath());
    final Observable<File> fileChunks = fileUtilsWrapper.getFileChunks(context, file, CHUNK_SIZE);
    final MediaType mediaType = MediaType
        .parse(FileUtils.getMimeType(context, Uri.parse(file.getPath())));

    final AtomicInteger index = new AtomicInteger();
    final AtomicReference<ChunkInfo> chunkInfo = new AtomicReference<>();
    if (contribution.getChunkInfo() != null && contribution.getDateCreated()
        .after(new Date(System.currentTimeMillis() - MAX_CHUNK_AGE))) {
      chunkInfo.set(contribution.getChunkInfo());
    }
    compositeDisposable.add(fileChunks.forEach(chunkFile -> {
      if (chunkInfo.get() != null && index.get() < chunkInfo.get().getLastChunkIndex()) {
        index.getAndIncrement();
        return;
      }
      final int offset =
          chunkInfo.get() != null ? chunkInfo.get().getUploadResult().getOffset() : 0;
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
        chunkInfo.set(new ChunkInfo(uploadResult, index.incrementAndGet()));
        notificationUpdater.onChunkUploaded(contribution, chunkInfo.get());
      }, throwable -> {
        Timber.e(throwable, "Error occurred in uploading chunk");
      }));
    }));
    if (chunkInfo.get() != null) {
      return Observable.just(chunkInfo.get().getUploadResult());
    } else {
      return Observable.error(new Error("Error occurred in uploading file to stash"));
    }

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

  public void pauseUpload() {
    Timber.d("Disposing current upload");
    compositeDisposable.dispose();
  }

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
              fileKey).map(UploadResponse::getUpload);
    } catch (final Throwable throwable) {
      throwable.printStackTrace();
      return Observable.error(throwable);
    }
  }
}
