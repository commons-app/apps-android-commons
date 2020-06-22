package fr.free.nrw.commons.upload;

import static fr.free.nrw.commons.di.NetworkingModule.NAMED_COMMONS_CSRF;

import android.content.Context;
import android.net.Uri;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.upload.UploadService.NotificationUpdateProgressListener;
import io.reactivex.Observable;
import java.io.File;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.wikipedia.csrf.CsrfTokenClient;

@Singleton
public class UploadClient {

    private final UploadInterface uploadInterface;
    private final CsrfTokenClient csrfTokenClient;
    private final PageContentsCreator pageContentsCreator;

    @Inject
    public UploadClient(UploadInterface uploadInterface,
        @Named(NAMED_COMMONS_CSRF) CsrfTokenClient csrfTokenClient,
        PageContentsCreator pageContentsCreator) {
        this.uploadInterface = uploadInterface;
        this.csrfTokenClient = csrfTokenClient;
        this.pageContentsCreator = pageContentsCreator;
    }

    Observable<UploadResult> uploadFileToStash(Context context, String filename, File file,
            NotificationUpdateProgressListener notificationUpdater) {
        RequestBody requestBody = RequestBody
                .create(MediaType.parse(FileUtils.getMimeType(context, Uri.parse(file.getPath()))), file);

        CountingRequestBody countingRequestBody = new CountingRequestBody(requestBody,
                (bytesWritten, contentLength) -> notificationUpdater
                        .onProgress(bytesWritten, contentLength));

        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", filename, countingRequestBody);
        RequestBody fileNameRequestBody = RequestBody.create(okhttp3.MultipartBody.FORM, filename);
        RequestBody tokenRequestBody;
        try {
            tokenRequestBody = RequestBody.create(MultipartBody.FORM, csrfTokenClient.getTokenBlocking());
            return uploadInterface.uploadFileToStash(fileNameRequestBody, tokenRequestBody, filePart)
                    .map(stashUploadResponse -> stashUploadResponse.getUpload());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return Observable.error(throwable);
        }
    }

    Observable<UploadResult> uploadFileFromStash(Contribution contribution,
        String uniqueFileName,
        String fileKey) {
        try {
            return uploadInterface
                    .uploadFileFromStash(csrfTokenClient.getTokenBlocking(),
                            pageContentsCreator.createFrom(contribution),
                            CommonsApplication.DEFAULT_EDIT_SUMMARY,
                            uniqueFileName,
                            fileKey).map(uploadResponse -> uploadResponse.getUpload());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return Observable.error(throwable);
        }
    }
}
