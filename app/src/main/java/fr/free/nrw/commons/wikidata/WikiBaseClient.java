package fr.free.nrw.commons.wikidata;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import javax.inject.Inject;
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
            return wikiBaseInterface.postEditEntity(fileEntityId, data, editToken)
                    .map(MwQueryResponse::success);
        } catch (Throwable throwable) {
            return Observable.just(false);
        }
    }

    public Observable<Long> getFileEntityId(String fileName) {
            return wikiBaseInterface.getFileEntityId(fileName)
                    .map(response -> (long)(response.query().pages().get(0).pageId()));
    }

}