package fr.free.nrw.commons.wikidata;

import org.wikipedia.csrf.CsrfTokenClient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import fr.free.nrw.commons.upload.WikiBaseInterface;
import io.reactivex.Observable;

@Singleton
public class WikiBaseClient {

    private final WikiBaseInterface wikiBaseInterface;
    private final CsrfTokenClient csrfTokenClient;

    @Inject
    public WikiBaseClient(WikiBaseInterface wikiBaseInterface,
                          @Named("commons-csrf") CsrfTokenClient csrfTokenClient) {
        this.wikiBaseInterface = wikiBaseInterface;
        this.csrfTokenClient = csrfTokenClient;
    }

    public Observable<Long> postEditEntity(String fileEntityId, String data, String editToken) {
        try {
        //return wikiBaseInterface.postEditEntity(csrfTokenClient.getTokenBlocking(), fileEntityId, data)
        return wikiBaseInterface.postEditEntity(editToken, fileEntityId, data)
                .map(response -> (long)(response.query().pages().get(0).pageId()));
        } catch (Throwable throwable) {
            return Observable.just(-1L);
        }
    }

    public Observable<Long> getFileEntityId(String fileName) {
        try {
            return wikiBaseInterface.getFileEntityId(fileName)
                    .map(response -> (long)(response.query().pages().get(0).pageId()));
        } catch (Throwable throwable) {
            return Observable.just(-1L);
        }
    }

    public Observable<String> getEditToken() {
        try {
            return wikiBaseInterface.getEditToken()
                    .map(response -> (response.query().loginToken().toString()));
        } catch (Throwable throwable) {
            return Observable.just("");
        }
    }

}