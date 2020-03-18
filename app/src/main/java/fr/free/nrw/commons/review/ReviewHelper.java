package fr.free.nrw.commons.review;


import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.RecentChange;
import org.wikipedia.util.DateUtil;

import java.util.Collections;
import java.util.Date;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.media.MediaClient;
import io.reactivex.Observable;
import io.reactivex.Single;

@Singleton
public class ReviewHelper {

    private static final String[] imageExtensions = new String[]{".jpg", ".jpeg", ".png"};

    private final MediaClient mediaClient;
    private final ReviewInterface reviewInterface;

    @Inject
    public ReviewHelper(MediaClient mediaClient, ReviewInterface reviewInterface) {
        this.mediaClient = mediaClient;
        this.reviewInterface = reviewInterface;
    }

    /**
     * Fetches recent changes from MediaWiki API
     * Calls the API to get 10 changes in the last 1 hour
     * Earlier we were getting changes for the last 30 days but as the API returns just 10 results
     * its best to fetch for just last 1 hour.
     *
     * @return
     */
    private Observable<RecentChange> getRecentChanges() {
        final int RANDOM_SECONDS = 60 * 60;
        Random r = new Random();
        Date now = new Date();
        Date startDate = new Date(now.getTime() - r.nextInt(RANDOM_SECONDS) * 1000L);

        String rcStart = DateUtil.iso8601DateFormat(startDate);
        return reviewInterface.getRecentChanges(rcStart)
                .map(mwQueryResponse -> mwQueryResponse.query().getRecentChanges())
                .map(recentChanges -> {
                    Collections.shuffle(recentChanges);
                    return recentChanges;
                })
                .flatMapIterable(changes -> changes)
                .filter(recentChange -> isChangeReviewable(recentChange));
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
                .flatMapSingle(change -> getRandomMediaFromRecentChange(change))
                .onExceptionResumeNext(Observable.just(new Media("")))
                .filter(media -> !StringUtils.isBlank(media.getFilename()))
                .firstOrError();
    }

    /**
     * Returns a proper Media object if the file is not already nominated for deletion
     * Else it returns an empty Media object
     *
     * @param recentChange
     * @return
     */
    private Single<Media> getRandomMediaFromRecentChange(RecentChange recentChange) {
        return Single.just(recentChange)
                .flatMap(change -> mediaClient.checkPageExistsUsingTitle("Commons:Deletion_requests/" + change.getTitle()))
                .flatMap(isDeleted -> {
                    if (isDeleted) {
                        return Single.just(new Media(""));
                    }
                    return mediaClient.getMedia(recentChange.getTitle());
                });

    }

    /**
     * Gets the first revision of the file from filename
     *
     * @param filename
     * @return
     */
    Observable<MwQueryPage.Revision> getFirstRevisionOfFile(String filename) {
        return reviewInterface.getFirstRevisionOfFile(filename)
                .map(response -> response.query().firstPage().revisions().get(0));
    }

    /**
     * Checks if the change is reviewable or not.
     * - checks the type and revisionId of the change
     * - checks supported image extensions
     *
     * @param recentChange
     * @return
     */
    private boolean isChangeReviewable(RecentChange recentChange) {
        if ((recentChange.getType().equals("log") && !(recentChange.getOldRevisionId() == 0))
                || !recentChange.getType().equals("log")) {
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
