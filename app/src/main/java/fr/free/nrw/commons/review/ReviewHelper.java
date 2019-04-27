package fr.free.nrw.commons.review;


import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.RecentChange;
import org.wikipedia.util.DateUtil;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.Observable;
import io.reactivex.Single;

@Singleton
public class ReviewHelper {
    private static final int MAX_RANDOM_TRIES = 5;

    private static final String[] imageExtensions = new String[]{".jpg", ".jpeg", ".png"};

    private final OkHttpJsonApiClient okHttpJsonApiClient;
    private final MediaWikiApi mediaWikiApi;
    private final ReviewInterface reviewInterface;

    @Inject
    public ReviewHelper(OkHttpJsonApiClient okHttpJsonApiClient,
                        MediaWikiApi mediaWikiApi,
                        ReviewInterface reviewInterface) {
        this.okHttpJsonApiClient = okHttpJsonApiClient;
        this.mediaWikiApi = mediaWikiApi;
        this.reviewInterface = reviewInterface;
    }

    private Observable<List<RecentChange>> getRecentChanges() {
        final int RANDOM_SECONDS = 60 * 60 * 24 * 30;
        Random r = new Random();
        Date now = new Date();
        Date startDate = new Date(now.getTime() - r.nextInt(RANDOM_SECONDS) * 1000L);

        String rcStart = DateUtil.iso8601DateFormat(startDate);
        return reviewInterface.getRecentChanges(rcStart).map(mwQueryResponse -> mwQueryResponse.query().getRecentChanges())
                .map(recentChanges -> {
                    Collections.shuffle(recentChanges);
                    return recentChanges;
                });
    }

    /**
     * Gets a random file change for review.
     * - Picks the most recent changes in the last 30 day window
     * - Picks a random file from those changes
     * - Checks if the file is nominated for deletion
     * - Retries upto 5 times for getting a file which is not nominated for deletion
     *
     * @return Random file change
     */
    public Single<Media> getRandomMedia() {
        return getRecentChanges()
                .flatMapIterable(changes -> changes)
                .filter(this::isChangeReviewable)
                .flatMapSingle(change -> mediaWikiApi.pageExists("Commons:Deletion_requests/" + change.getTitle())
                        .map(exists -> {
                            if (exists) {
                                throw new RuntimeException("Already nominated for deletion");
                            }
                            return change.getTitle();
                        }))
                .flatMapSingle(fileName -> okHttpJsonApiClient.getMedia(fileName, false)
                        .map(media -> {
                            if (media == null) {
                                throw new NullPointerException("Media is null");
                            }
                            return media;
                        }))
                .retry(MAX_RANDOM_TRIES)
                .firstOrError();
    }

    Observable<MwQueryPage.Revision> getFirstRevisionOfFile(String filename) {
        return reviewInterface.getFirstRevisionOfFile(filename)
                .map(response -> response.query().firstPage().revisions().get(0));
    }

    private boolean isChangeReviewable(RecentChange recentChange) {
        if (recentChange.getType().equals("log") && !(recentChange.getOldRevisionId() == 0)) {
            return false;
        }

        for (String extension : imageExtensions) {
            if (recentChange.getTitle().endsWith(extension)) {
                return true;
            }
        }

        return false;
    }
}
