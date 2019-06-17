package fr.free.nrw.commons.media;


import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;

/**
 * Media Client to handle custom calls to Commons MediaWiki APIs
 */
@Singleton
public class MediaClient {

    private final MediaInterface mediaInterface;

    @Inject
    public MediaClient(MediaInterface mediaInterface) {
        this.mediaInterface = mediaInterface;
    }

    /**
     * Checks if a page exists on Commons
     * The same method can be used to check for file or talk page
     *
     * @param title File:Test.jpg or Commons:Deletion_requests/File:Test1.jpeg
     */
    public Single<Boolean> checkPageExistsUsingTitle(String title) {
        return mediaInterface.checkPageExistsUsingTitle(title)
                .map(mwQueryResponse -> mwQueryResponse
                        .query().firstPage() != null)
                .singleOrError();
    }

    /**
     * Take the fileSha and returns whether a file with a matching SHA exists or not
     *
     * @param fileSha SHA of the file to be checked
     */
    public Single<Boolean> checkFileExistsUsingSha(String fileSha) {
        return mediaInterface.checkFileExistsUsingSha(fileSha)
                .map(mwQueryResponse -> mwQueryResponse
                        .query().firstPage().allImages().size() > 0)
                .singleOrError();
    }
}
