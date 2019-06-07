package fr.free.nrw.commons.media;


import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;

@Singleton
public class MediaClient {

    private final MediaInterface mediaInterface;

    @Inject
    public MediaClient(MediaInterface mediaInterface) {
        this.mediaInterface = mediaInterface;
    }

    public Single<Boolean> doesPageExist(String title) {
        return mediaInterface.doesPageExist(title)
                .map(mwQueryResponse -> mwQueryResponse.query().firstPage() != null)
                .singleOrError();
    }

    public Single<Boolean> doesFileExist(String fileSha) {
        return mediaInterface.doesFileExist(fileSha)
                .map(mwQueryResponse -> mwQueryResponse.query().firstPage().allImages().size() > 0)
                .singleOrError();
    }
}
