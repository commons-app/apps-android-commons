package fr.free.nrw.commons.wikidata;

import org.wikipedia.csrf.CsrfTokenClient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.upload.WikiBaseInterface;
import fr.free.nrw.commons.utils.ConfigUtils;
import io.reactivex.Observable;

import static fr.free.nrw.commons.di.NetworkingModule.NAMED_WIKI_DATA_CSRF;

/**
 * Wikibase Client for calling WikiBase APIs
 */
@Singleton
public class WikiBaseClient {

    private final WikiBaseInterface wikiBaseInterface;
    private final CsrfTokenClient csrfTokenClient;

    @Inject
    public WikiBaseClient(WikiBaseInterface wikiBaseInterface,
                          @Named(NAMED_WIKI_DATA_CSRF) CsrfTokenClient csrfTokenClient) {
        this.wikiBaseInterface = wikiBaseInterface;
        this.csrfTokenClient = csrfTokenClient;
    }

    public Observable<Boolean> postEditEntity(String fileEntityId, String editToken, String data) {
        try {
            /*editToken = csrfTokenClient.getTokenBlocking();
            if (ConfigUtils.isBetaFlavour()) {
                editToken = "+\\";
            }*/
            return wikiBaseInterface.postEditEntity(fileEntityId, editToken, data)
                    .map(response -> (response.getSuccessVal() == 1));
        } catch (Throwable throwable) {
            return Observable.just(false);
        }
    }

    public Observable<Long> getFileEntityId(String fileName) {
        return wikiBaseInterface.getFileEntityId(fileName)
                .map(response -> (long)(response.query().pages().get(0).pageId()));
    }
}