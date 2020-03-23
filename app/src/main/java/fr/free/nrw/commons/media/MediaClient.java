package fr.free.nrw.commons.media;


import androidx.annotation.NonNull;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.utils.CommonsDateUtil;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import timber.log.Timber;

/**
 * Media Client to handle custom calls to Commons MediaWiki APIs
 */
@Singleton
public class MediaClient {

    private final MediaInterface mediaInterface;

    //OkHttpJsonApiClient used JsonKvStore for this. I don't know why.
    private Map<String, Map<String, String>> continuationStore;

    @Inject
    public MediaClient(MediaInterface mediaInterface) {
        this.mediaInterface = mediaInterface;
        this.continuationStore = new HashMap<>();
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
                        .query().firstPage().pageId() > 0)
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
                        .query().allImages().size() > 0)
                .singleOrError();
    }

    /**
     * This method takes the category as input and returns a list of  Media objects filtered using image generator query
     * It uses the generator query API to get the images searched using a query, 10 at a time.
     *
     * @param category the search category. Must start with "Category:"
     * @return
     */
    public Single<List<Media>> getMediaListFromCategory(String category) {
        return responseToMediaList(
                continuationStore.containsKey("category_" + category) ?
                        mediaInterface.getMediaListFromCategory(category, 10, continuationStore.get("category_" + category)) : //if true
                        mediaInterface.getMediaListFromCategory(category, 10, Collections.emptyMap()),
                "category_" + category); //if false

    }

    /**
     * This method takes the userName as input and returns a list of  Media objects filtered using
     * allimages query It uses the allimages query API to get the images contributed by the userName,
     * 10 at a time.
     *
     * @param userName the username
     * @return
     */
    public Single<List<Media>> getMediaListForUser(String userName) {
        return responseToMediaList(
            continuationStore.containsKey("user_" + userName) ?
                mediaInterface.getMediaListFromCategory(userName, 10,
                    continuationStore.get("user_" + userName)) : //if true
                mediaInterface.getMediaListFromCategory(userName, 10, Collections.emptyMap()),
            "user_" + userName); //if false

    }

    /**
     * This method takes a keyword as input and returns a list of  Media objects filtered using image generator query
     * It uses the generator query API to get the images searched using a query, 10 at a time.
     *
     * @param keyword the search keyword
     * @return
     */
    public Single<List<Media>> getMediaListFromSearch(String keyword) {
        return responseToMediaList(
                continuationStore.containsKey("search_" + keyword) ?
                        mediaInterface.getMediaListFromSearch(keyword, 10, continuationStore.get("search_" + keyword)) : //if true
                        mediaInterface.getMediaListFromSearch(keyword, 10, Collections.emptyMap()), //if false
                "search_" + keyword);

    }

    private Single<List<Media>> responseToMediaList(Observable<MwQueryResponse> response, String key) {
        return response.flatMap(mwQueryResponse -> {
            if (null == mwQueryResponse
                    || null == mwQueryResponse.query()
                    || null == mwQueryResponse.query().pages()) {
                return Observable.empty();
            }
            continuationStore.put(key, mwQueryResponse.continuation());
            return Observable.fromIterable(mwQueryResponse.query().pages());
        })
                .map(Media::from)
                .collect(ArrayList<Media>::new, List::add);
    }
  
     /**
     * Fetches Media object from the imageInfo API
     *
     * @param titles the tiles to be searched for. Can be filename or template name
     * @return
     */
    public Single<Media> getMedia(String titles) {
        return mediaInterface.getMedia(titles)
                .flatMap(mwQueryResponse -> {
                    if (null == mwQueryResponse
                            || null == mwQueryResponse.query()
                            || null == mwQueryResponse.query().firstPage()) {
                        return Observable.empty();
                    }
                    return Observable.just(mwQueryResponse.query().firstPage());
                })
                .map(Media::from)
                .single(Media.EMPTY);
    }

    /**
     * The method returns the picture of the day
     *
     * @return Media object corresponding to the picture of the day
     */
    @NonNull
    public Single<Media> getPictureOfTheDay() {
        String date = CommonsDateUtil.getIso8601DateFormatShort().format(new Date());
        Timber.d("Current date is %s", date);
        String template = "Template:Potd/" + date;
        return mediaInterface.getMediaWithGenerator(template)
                .flatMap(mwQueryResponse -> {
                    if (null == mwQueryResponse
                            || null == mwQueryResponse.query()
                            || null == mwQueryResponse.query().firstPage()) {
                        return Observable.empty();
                    }
                    return Observable.just(mwQueryResponse.query().firstPage());
                })
                .map(Media::from)
                .single(Media.EMPTY);
    }

    @NonNull
    public Single<String> getPageHtml(String title){
        return mediaInterface.getPageHtml(title)
                .filter(MwParseResponse::success)
                .map(MwParseResponse::parse)
                .map(MwParseResult::text)
                .first("");
    }
}
