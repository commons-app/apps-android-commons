package fr.free.nrw.commons.review;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.media.RecentChangesImageUtils;
import fr.free.nrw.commons.media.model.MwQueryPage;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.Single;

@Singleton
public class ReviewHelper {
    private static final int MAX_RANDOM_TRIES = 5;

    private final OkHttpJsonApiClient okHttpJsonApiClient;
    private final MediaWikiApi mediaWikiApi;

    @Inject
    public ReviewHelper(OkHttpJsonApiClient okHttpJsonApiClient, MediaWikiApi mediaWikiApi) {
        this.okHttpJsonApiClient = okHttpJsonApiClient;
        this.mediaWikiApi = mediaWikiApi;
    }

    public Single<Media> getRandomMedia() {
        return okHttpJsonApiClient.getRecentFileChanges()
                .map(RecentChangesImageUtils::findImageInRecentChanges)
                .map(title -> {
                    boolean pageExists = mediaWikiApi.pageExists("Commons:Deletion_requests/" + title);
                    if (!pageExists) {
                        title = title.replace("File:", "");
                        return new Media(title);
                    }
                    throw new Exception("Page does not exist");
                }).retry(MAX_RANDOM_TRIES);
    }

    public Single<MwQueryPage.Revision> getFirstRevisionOfFile(String fileName) {
        return okHttpJsonApiClient.getFirstRevisionOfFile(fileName);
    }
}
