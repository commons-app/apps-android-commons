package fr.free.nrw.commons.wikidata;

import static fr.free.nrw.commons.di.NetworkingModule.NAMED_COMMONS_CSRF;
import static fr.free.nrw.commons.media.MediaClientKt.PAGE_ID_PREFIX;

import fr.free.nrw.commons.upload.UploadResult;
import fr.free.nrw.commons.upload.WikiBaseInterface;
import io.reactivex.Observable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.mwapi.MwPostResponse;
import timber.log.Timber;

/**
 * Wikibase Client for calling WikiBase APIs
 */
@Singleton
public class WikiBaseClient {

    private final WikiBaseInterface wikiBaseInterface;
    private final CsrfTokenClient csrfTokenClient;

    @Inject
    public WikiBaseClient(WikiBaseInterface wikiBaseInterface,
        @Named(NAMED_COMMONS_CSRF) CsrfTokenClient csrfTokenClient) {
        this.wikiBaseInterface = wikiBaseInterface;
        this.csrfTokenClient = csrfTokenClient;
    }

    public Observable<Boolean> postEditEntity(String fileEntityId, String data) {
        return csrfToken()
            .switchMap(editToken -> wikiBaseInterface.postEditEntity(fileEntityId, editToken, data)
                .map(response -> (response.getSuccessVal() == 1)));
    }

    /**
     * Makes the server call for posting new depicts
     *
     * @param filename name of the file
     * @param data data of the depicts to be uploaded
     * @return Observable<Boolean>
     */
    public Observable<Boolean> postEditEntityByFilename(final String filename, final String data) {
        return csrfToken()
            .switchMap(editToken -> wikiBaseInterface.postEditEntityByFilename(filename,
                editToken, data)
                .map(response -> (response.getSuccessVal() == 1)));
    }

    public Observable<Long> getFileEntityId(UploadResult uploadResult) {
        return wikiBaseInterface.getFileEntityId(uploadResult.createCanonicalFileName())
            .map(response -> (long) (response.query().pages().get(0).pageId()));
    }

    public Observable<MwPostResponse> addLabelstoWikidata(long fileEntityId,
        String languageCode, String captionValue) {
        return csrfToken()
            .switchMap(editToken -> wikiBaseInterface
                .addLabelstoWikidata(PAGE_ID_PREFIX + fileEntityId, editToken, languageCode,
                    captionValue));

    }

    private Observable<String> csrfToken() {
        return Observable.fromCallable(() -> {
            try {
                return csrfTokenClient.getTokenBlocking();
            } catch (Throwable throwable) {
                Timber.e(throwable);
                return "";
            }
        });
    }
}
