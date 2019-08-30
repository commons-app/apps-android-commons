package fr.free.nrw.commons.wikidata;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.upload.WikiBaseInterface;
import fr.free.nrw.commons.utils.ConfigUtils;
import io.reactivex.Observable;

@Singleton
public class WikiBaseClient {
    private final WikiBaseInterface wikiBaseInterface;

    @Inject
    public WikiBaseClient(WikiBaseInterface wikiBaseInterface) {
        this.wikiBaseInterface = wikiBaseInterface;
    }

    public Observable<Boolean> postEditEntity(String fileEntityId, String editToken, String data) {
        try {
            if (ConfigUtils.isBetaFlavour()) {
                editToken = "+\\";
            }
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