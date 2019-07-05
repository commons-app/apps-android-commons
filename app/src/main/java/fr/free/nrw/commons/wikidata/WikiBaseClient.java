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

    @Inject
    public WikiBaseClient(WikiBaseInterface wikiBaseInterface) {
        this.wikiBaseInterface = wikiBaseInterface;
    }

    public Observable<Boolean> postEditEntity(String fileEntityId, String data, String editToken) {
        try {
        return wikiBaseInterface.postEditEntity(editToken, fileEntityId, data)
                .map(response -> response.success());
        } catch (Throwable throwable) {
            return Observable.just(false);
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

}