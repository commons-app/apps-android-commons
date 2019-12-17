package fr.free.nrw.commons;

import androidx.core.text.HtmlCompat;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.media.MediaClient;
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
    private final MediaClient mediaClient;

    @Inject
    public MediaDataExtractor(MediaClient mediaClient) {
        this.mediaClient = mediaClient;
    }

    /**
     * Simplified method to extract all details required to show media details.
     * It fetches media object, deletion status and talk page for the filename
     * @param filename for which the details are to be fetched
     * @return full Media object with all details including deletion status and talk page
     */
    public Single<Media> fetchMediaDetails(String filename) {
        Single<Media> mediaSingle = getMediaFromFileName(filename);
        Single<Boolean> pageExistsSingle = mediaClient.checkPageExistsUsingTitle("Commons:Deletion_requests/" + filename);
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
        return mediaClient.getPageHtml(filename.replace("File", "File talk"))
                .map(discussion -> HtmlCompat.fromHtml(discussion, HtmlCompat.FROM_HTML_MODE_LEGACY).toString())
                .onErrorReturn(throwable -> {
                    Timber.e(throwable, "Error occurred while fetching discussion");
                    return "";
                });
    }
}
