package fr.free.nrw.commons.review;


import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.media.MediaClient;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.Collections;
import java.util.Date;
import java.util.Random;
import javax.inject.Inject;
import javax.inject.Singleton;
import kotlin.jvm.Volatile;
import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.RecentChange;
import timber.log.Timber;

@Singleton
public class ReviewHelper {

    private static final String[] imageExtensions = new String[]{".jpg", ".jpeg", ".png"};

    private final MediaClient mediaClient;
    private final ReviewInterface reviewInterface;

    @Volatile
    static Boolean isReviewed = false;
    @Inject
    ReviewDao dao;

    @Inject
    public ReviewHelper(MediaClient mediaClient, ReviewInterface reviewInterface) {
        this.mediaClient = mediaClient;
        this.reviewInterface = reviewInterface;
    }

    /**
     * Fetches recent changes from MediaWiki API
     * Calls the API to get the latest 20 changes
     * When more results are available, the query gets continued beyond this range
     * The query uses the default value of rccontinue
     *
     * @return
     */
    private Observable<RecentChange> getRecentChanges() {
        return reviewInterface.getRecentChanges()
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
                .filter(media -> !StringUtils.isBlank(media.getFilename())
                   && !isShownAlready(media.getPageId())    // Check if the image has already been shown to the user
                )
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
                        return Single.error(new Exception(recentChange.getTitle() + " is deleted"));
                    }
                    return mediaClient.getMedia(recentChange.getTitle());
                });

    }

    /**
     * Returns the review status of the image
     *
     * @param image
     * @return
     */
    private boolean isShownAlready(String image){
        getReviewStatus(image).subscribe(
            isShown -> isReviewed = isShown
        );
        return isReviewed;
    }

    /**
     * Checks if the image exists in the reviewed images entity
     *
     * @param image
     * @return
     */
    private Observable<Boolean> getReviewStatus(String image){
        Observable<Boolean> reviewObservable = Observable.fromCallable(()->
                                                dao.isReviewedAlready(image))
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread());
        return reviewObservable.observeOn(AndroidSchedulers.mainThread());
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

    /**
     * Adds reviewed/skipped images to the database
     *
     * @param imageId
     */
    public void addViewedImagesToDB(String imageId) {
        Completable.fromAction(() -> dao.insert(new ReviewEntity(imageId)))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(() -> {
                // Inserted successfully
                },
                throwable -> {
                    Timber.e("Image not inserted into the reviewed images database");
                }
            );
    }
}
