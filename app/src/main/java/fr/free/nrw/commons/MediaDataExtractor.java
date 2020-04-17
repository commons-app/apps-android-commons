package fr.free.nrw.commons;

import static fr.free.nrw.commons.depictions.Media.DepictedImagesFragment.PAGE_ID_PREFIX;

import androidx.core.text.HtmlCompat;
import fr.free.nrw.commons.media.Depictions;
import fr.free.nrw.commons.media.MediaClient;
import io.reactivex.Single;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jetbrains.annotations.NotNull;
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
    public MediaDataExtractor(final MediaClient mediaClient) {
        this.mediaClient = mediaClient;
    }

    /**
     * Simplified method to extract all details required to show media details.
     * It fetches media object, deletion status, talk page and captions for the filename
     * @param filename for which the details are to be fetched
     * @return full Media object with all details including deletion status and talk page
     */
    public Single<Media> fetchMediaDetails(final String filename, final String pageId) {
      return Single.zip(getMediaFromFileName(filename),
            mediaClient.checkPageExistsUsingTitle("Commons:Deletion_requests/" + filename),
            getDiscussion(filename),
            pageId != null ? getCaption(PAGE_ID_PREFIX + pageId) : Single.just(""),
            getDepictions(filename),
            this::combineToMedia);
    }

  @NotNull
  private Media combineToMedia(final Media media, final Boolean deletionStatus, final String discussion,
      final String caption, final Depictions depictions) {
    media.setDiscussion(discussion);
    media.setCaption(caption);
    media.setDepictions(depictions);
    if (deletionStatus) {
        media.setRequestedDeletion(true);
    }
    return media;
  }

  /**
     * Obtains captions using filename
     * @param wikibaseIdentifier
     *
     * @return caption for the image in user's locale
     * Ex: "a nice painting" (english locale) and "No Caption" in case the caption is not available for the image
     */
    private Single<String> getCaption(final String wikibaseIdentifier) {
        return mediaClient.getCaptionByWikibaseIdentifier(wikibaseIdentifier);
    }

    /**
     * Fetch depictions from the MediaWiki API
     * @param filename the filename we will return the caption for
     * @return Depictions
     */
 private Single<Depictions> getDepictions(final String filename)  {
         return mediaClient.getDepictions(filename)
             .doOnError(throwable -> Timber.e(throwable, "error while fetching depictions"));
    }

    /**
     * Method can be used to fetch media for a given filename
     * @param filename Eg. File:Test.jpg
     * @return return data rich Media object
     */
    public Single<Media> getMediaFromFileName(final String filename) {
        return mediaClient.getMedia(filename);
    }

    /**
     * Fetch talk page from the MediaWiki API
     * @param filename
     * @return
     */
    private Single<String> getDiscussion(final String filename) {
        return mediaClient.getPageHtml(filename.replace("File", "File talk"))
                .map(discussion -> HtmlCompat.fromHtml(discussion, HtmlCompat.FROM_HTML_MODE_LEGACY).toString())
                .onErrorReturn(throwable -> {
                    Timber.e(throwable, "Error occurred while fetching discussion");
                    return "";
                });
    }
}
