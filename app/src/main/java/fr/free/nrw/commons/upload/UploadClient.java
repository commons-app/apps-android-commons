package fr.free.nrw.commons.upload;

import android.content.Context;
import android.net.Uri;

import org.wikipedia.csrf.CsrfTokenClient;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.upload.UploadService.NotificationUpdateProgressListener;
import io.reactivex.Observable;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import static fr.free.nrw.commons.di.NetworkingModule.NAMED_COMMONS_CSRF;

@Singleton
public class UploadClient {

    private final UploadInterface uploadInterface;
    private final CsrfTokenClient csrfTokenClient;

    @Inject
    public UploadClient(UploadInterface uploadInterface, @Named(NAMED_COMMONS_CSRF) CsrfTokenClient csrfTokenClient) {
        this.uploadInterface = uploadInterface;
        this.csrfTokenClient = csrfTokenClient;
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

    Observable<UploadResult> uploadFileFromStash(Context context,
                                                 Contribution contribution,
                                                 String uniqueFileName,
                                                 String fileKey) {
        try {
            return uploadInterface
                    .uploadFileFromStash(csrfTokenClient.getTokenBlocking(),
                            contribution.getPageContents(context),
                            contribution.getEditSummary(),
                            uniqueFileName,
                            fileKey).map(uploadResponse -> uploadResponse.getUpload());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return Observable.error(throwable);
        }
    }
}
