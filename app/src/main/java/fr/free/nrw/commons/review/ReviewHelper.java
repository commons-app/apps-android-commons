package fr.free.nrw.commons.review;

import android.util.Pair;

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

    Single<Media> getRandomMedia() {
        return okHttpJsonApiClient.getRecentFileChanges()
                .map(RecentChangesImageUtils::findImageInRecentChanges)
                .flatMap(title -> mediaWikiApi.pageExists("Commons:Deletion_requests/" + title)
                        .map(pageExists -> new Pair<>(title, pageExists)))
                .map((Pair<String, Boolean> pair) -> {
                    if (!pair.second) {
                        return new Media(pair.first.replace("File:", ""));
                    }
                    throw new Exception("Page does not exist");
                }).retry(MAX_RANDOM_TRIES);
    }

    Single<MwQueryPage.Revision> getFirstRevisionOfFile(String fileName) {
        return okHttpJsonApiClient.getFirstRevisionOfFile(fileName);
    }
}
