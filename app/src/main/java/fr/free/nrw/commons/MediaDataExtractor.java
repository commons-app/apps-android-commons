package fr.free.nrw.commons;

import android.util.Log;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.core.text.HtmlCompat;
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


    @Inject
    public MediaDataExtractor(MediaWikiApi mwApi,
                              OkHttpJsonApiClient okHttpJsonApiClient) {
        this.okHttpJsonApiClient = okHttpJsonApiClient;
        this.mediaWikiApi = mwApi;

    }

    /**
     * Simplified method to extract all details required to show media details.
     * It fetches media object, deletion status, talk page and caption for the filename
     * @param filename for which the details are to be fetched
     * @return full Media object with all details including deletion status and talk page
     */
    public Single<Media> fetchMediaDetails(String filename) {
        Single<Media> mediaSingle = getMediaFromFileName(filename);
        Single<Boolean> pageExistsSingle = mediaWikiApi.pageExists("Commons:Deletion_requests/" + filename);
        Single<String> discussionSingle = getDiscussion(filename);
        Single<String> captionSingle = getCaption(filename);

        return Single.zip(mediaSingle, pageExistsSingle, discussionSingle, captionSingle, (media, deletionStatus, discussion, caption) -> {
            media.setDiscussion(discussion);
            media.setCaption(caption);
            if (deletionStatus) {
                media.setRequestedDeletion();
            }
            return media;
        });
    }

    /**
     * Method can be used to fetch media for a given filename
     * @param filename Eg. File:Test.jpg
     * @return return data rich Media object
     */
    public Single<Media> getMediaFromFileName(String filename) {
        return okHttpJsonApiClient.getMedia(filename, false);
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

    /**
     * Fetch caption from the MediaWiki API
     * @param filename the filename we will return the caption for
     * @return a single with caption string (an empty string if no caption)
     */
    private Single<String> getCaption(String filename)  {
        return mediaWikiApi.fetchCaptionByFilename(filename)
                .onErrorReturn(throwable -> {
                    Timber.e(throwable, "Error occurred while fetching caption");
                    return "";
                });
    }
}
