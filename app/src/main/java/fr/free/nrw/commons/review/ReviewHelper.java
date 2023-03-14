package fr.free.nrw.commons.review;


import androidx.annotation.VisibleForTesting;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.media.MediaClient;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.Collections;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import timber.log.Timber;

@Singleton
public class ReviewHelper {

    private static final String[] imageExtensions = new String[]{".jpg", ".jpeg", ".png"};

    private final MediaClient mediaClient;
    private final ReviewInterface reviewInterface;

    @Inject
    ReviewDao dao;

    @Inject
    public ReviewHelper(MediaClient mediaClient, ReviewInterface reviewInterface) {
        this.mediaClient = mediaClient;
        this.reviewInterface = reviewInterface;
    }

    /**
     * Fetches recent changes from MediaWiki API
     * Calls the API to get the latest 50 changes
     * When more results are available, the query gets continued beyond this range
     *
     * @return
     */
    private Observable<MwQueryPage> getRecentChanges() {
        return reviewInterface.getRecentChanges()
                .map(mwQueryResponse -> mwQueryResponse.query().pages())
                .map(recentChanges -> {
                    Collections.shuffle(recentChanges);
                    return recentChanges;
                })
                .flatMapIterable(changes -> changes)
                .filter(recentChange -> isChangeReviewable(recentChange));
    }

    /**
     * Gets a random file change for review.
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
                   && !getReviewStatus(media.getPageId())    // Check if the image has already been shown to the user
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
    private Single<Media> getRandomMediaFromRecentChange(MwQueryPage recentChange) {
        return Single.just(recentChange)
                .flatMap(change -> mediaClient.checkPageExistsUsingTitle("Commons:Deletion_requests/" + change.title()))
                .flatMap(isDeleted -> {
                    if (isDeleted) {
                        return Single.error(new Exception(recentChange.title() + " is deleted"));
                    }
                    return mediaClient.getMedia(recentChange.title());
                });

    }

    /**
     * Checks if the image exists in the reviewed images entity
     *
     * @param image
     * @return
     */
    @VisibleForTesting
    Boolean getReviewStatus(String image){
        if(dao == null){
            return false;
        }
        return Observable.fromCallable(()-> dao.isReviewedAlready(image))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).blockingSingle();
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
     * Checks Whether Given File is used in any Wiki page or not
     * by calling api for given file
     *
     * @param filename
     * @return
     */
     Observable<Boolean> checkFileUsage(final String filename) {
         return reviewInterface.getGlobalUsageInfo(filename)
             .map(mwQueryResponse -> mwQueryResponse.query().firstPage()
                 .checkWhetherFileIsUsedInWikis());
     }

    /**
     * Checks if the change is reviewable or not.
     * - checks the type and revisionId of the change
     * - checks supported image extensions
     *
     * @param recentChange
     * @return
     */
    private boolean isChangeReviewable(MwQueryPage recentChange) {
        for (String extension : imageExtensions) {
            if (recentChange.title().endsWith(extension)) {
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
                Timber.i("Image inserted successfully.");
                },
                throwable -> {
                    Timber.e("Image not inserted into the reviewed images database");
                }
            );
    }
}
