package fr.free.nrw.commons.media;


import androidx.annotation.Nullable;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.mwapi.MwQueryResult;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.utils.CommonsDateUtil;
import io.reactivex.Single;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

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
     * Fetches Media object from the imageInfo API
     *
     * @param titles the tiles to be searched for. Can be filename or template name
     * @return
     */
    public Single<Media> getMedia(String titles) {
        return mediaInterface.getMedia(titles)
                .map(MwQueryResponse::query)
                .map(MwQueryResult::firstPage)
                .map(Media::from)
                .single(Media.EMPTY);
    }

    /**
     * The method returns the picture of the day
     *
     * @return Media object corresponding to the picture of the day
     */
    @Nullable
    public Single<Media> getPictureOfTheDay() {
        String date = CommonsDateUtil.getIso8601DateFormatShort().format(new Date());
        Timber.d("Current date is %s", date);
        String template = "Template:Potd/" + date;
        return mediaInterface.getMediaWithGenerator(template)
                .map(MwQueryResponse::query)
                .map(MwQueryResult::firstPage)
                .map(Media::from)
                .single(Media.EMPTY);
    }
}
