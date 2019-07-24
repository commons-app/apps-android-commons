package fr.free.nrw.commons;

import androidx.core.text.HtmlCompat;


import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.media.MediaClient;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.Single;
import timber.log.Timber;

/**
 * Fetch additional media data from the network that we don't store locally.
 *
 * This includes things like category lists and multilingual descriptions,
 * which are not intrinsic to the media and may change due to editing.
 */
@Singleton
public class MediaDataExtractor {
    private final MediaWikiApi mediaWikiApi;
    private final OkHttpJsonApiClient okHttpJsonApiClient;
    private final MediaClient mediaClient;
    private String depiction, caption;

    @Inject
    public MediaDataExtractor(MediaWikiApi mwApi,
                              OkHttpJsonApiClient okHttpJsonApiClient,
                              MediaClient mediaClient) {
        this.okHttpJsonApiClient = okHttpJsonApiClient;
        this.mediaWikiApi = mwApi;
        this.mediaClient = mediaClient;
    }

    /**
     * Simplified method to extract all details required to show media details.
     * It fetches media object, deletion status and talk page and caption for the filename
     * @param filename for which the details are to be fetched
     * @return full Media object with all details including deletion status and talk page
     */
    public Single<Media> fetchMediaDetails(String filename) {
        Single<Media> mediaSingle = getMediaFromFileName(filename);
        Single<Boolean> pageExistsSingle = mediaClient.checkPageExistsUsingTitle("Commons:Deletion_requests/" + filename);
        Single<String> discussionSingle = getDiscussion(filename);
        Single<Map<String, String>> captionAndDepictionMap = getCaptionAndDepictions(filename);
        return Single.zip(mediaSingle, pageExistsSingle, discussionSingle, captionAndDepictionMap, (media, deletionStatus, discussion, caption) -> {
            media.setDiscussion(discussion);
            media.setCaption(caption.get("Caption"));
            media.setDepiction(caption.get("Depiction"));
            if (deletionStatus) {
                media.setRequestedDeletion();
            }
            return media;
        });
    }

    /**
     * Fetch caption from the MediaWiki API
     * @param filename the filename we will return the caption for
     * @return a single with caption string (an empty string if no caption)
     */
    private Single<Map <String, String>> getCaptionAndDepictions(String filename)  {
         return mediaClient.getCaptionAndDepictions(filename)
                .map(mediaResponse -> {
                    return mediaResponse;
                }).doOnError(throwable -> {
                    Timber.e("eror while fetching captions");
                 });

    }

    private void setDepiction(String depiction) {
        this.depiction = depiction;
    }

    private void setCaption(String caption) {
        this.caption = caption;
    }

    /**
     * Method can be used to fetch media for a given filename
     * @param filename Eg. File:Test.jpg
     * @return return data rich Media object
     */
    public Single<Media> getMediaFromFileName(String filename) {
        return mediaClient.getMedia(filename);
    }

    /**
     * Fetch talk page from the MediaWiki API
     * @param filename
     * @return
     */
    private Single<String> getDiscussion(String filename) {
        return mediaWikiApi.fetchMediaByFilename(filename.replace("File", "File talk"))
                .flatMap(mediaResult -> mediaWikiApi.parseWikicode(mediaResult.getWikiSource()))
                .map(discussion -> HtmlCompat.fromHtml(discussion, HtmlCompat.FROM_HTML_MODE_LEGACY).toString())
                .onErrorReturn(throwable -> {
                    Timber.e(throwable, "Error occurred while fetching discussion");
                    return "";
                });
    }
}
