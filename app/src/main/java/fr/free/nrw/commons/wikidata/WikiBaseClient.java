package fr.free.nrw.commons.wikidata;

import static fr.free.nrw.commons.di.NetworkingModule.NAMED_COMMONS_CSRF;

import fr.free.nrw.commons.upload.UploadResult;
import fr.free.nrw.commons.upload.WikiBaseInterface;
import io.reactivex.Observable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.wikipedia.csrf.CsrfTokenClient;

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
        try {
            return wikiBaseInterface.postEditEntity(fileEntityId, csrfTokenClient.getTokenBlocking(), data)
                    .map(response -> (response.getSuccessVal() == 1));
        } catch (Throwable throwable) {
            return Observable.just(false);
        }
    }

    public Observable<Long> getFileEntityId(UploadResult uploadResult) {
        return wikiBaseInterface.getFileEntityId(uploadResult.createCanonicalFileName())
                .map(response -> (long) (response.query().pages().get(0).pageId()));
    }
}
