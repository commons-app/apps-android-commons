package fr.free.nrw.commons;

import android.text.Html;

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
     * It fetches media object, deletion status and talk page for the filename
     * @param filename for which the details are to be fetched
     * @return full Media object with all details including deletion status and talk page
     */
    public Single<Media> fetchMediaDetails(String filename) {
        Single<Media> mediaSingle = okHttpJsonApiClient.getMedia(filename, false);
        Single<Boolean> pageExistsSingle = mediaWikiApi.pageExists("Commons:Deletion_requests/" + filename);
        Single<String> discussionSingle = getDiscussion(filename);
        return Single.zip(mediaSingle, pageExistsSingle, discussionSingle, (media, deletionStatus, discussion) -> {
            media.setDiscussion(discussion);
            if (deletionStatus) {
                media.setRequestedDeletion();
            }
            return media;
        });
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
