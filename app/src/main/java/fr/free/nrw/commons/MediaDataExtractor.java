package fr.free.nrw.commons;

import static fr.free.nrw.commons.depictions.Media.DepictedImagesFragment.PAGE_ID_PREFIX;

import androidx.core.text.HtmlCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.free.nrw.commons.media.MediaClient;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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

  private static final int LABEL_BEGIN_INDEX = 3;
  private static final int LABEL_END_OFFSET = 3;
  private static final int ID_BEGIN_INDEX = 1;
  private static final int ID_END_OFFSET = 1;
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
            getCaption(PAGE_ID_PREFIX + pageId),
            getDepictions(filename),
            this::combineToMedia);
    }

  @NotNull
  private Media combineToMedia(final Media media, final Boolean deletionStatus, final String discussion,
      final String caption, final JsonObject depiction) {
    media.setDiscussion(discussion);
    media.setCaption(caption);
    media.setDepictionList(formatDepictions(depiction));
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
     * From the Json Object extract depictions into an array list
     * @param mediaResponse
     * @return List containing map for depictions, the map has two keys,
     *  first key is for the label and second is for the url of the item
     */
    private ArrayList<Map<String, String>> formatDepictions(final JsonObject mediaResponse) {
        try {
            final JsonArray depictionArray = (JsonArray) mediaResponse.get("Depiction");
            final ArrayList<Map<String, String>> depictedItemList = new ArrayList<>();
            for (int i = 0; i <depictionArray.size() ; i++) {
                final JsonObject depictedItem = (JsonObject) depictionArray.get(i);
                final Map <String, String> depictedObject = new HashMap<>();
                final String label = depictedItem.get("label").toString();
                final String id =  depictedItem.get("id").toString();
                final String transformedLabel = label.substring(LABEL_BEGIN_INDEX, label.length()- LABEL_END_OFFSET);
                final String transformedId = id.substring(ID_BEGIN_INDEX,id.length() - ID_END_OFFSET);
                depictedObject.put("label", transformedLabel); //remove the additional characters obtained in label and ID object to extract the relevant string (since the string also contains extra quites that are not required)
                depictedObject.put("id", transformedId);
                depictedItemList.add(depictedObject);
            }
            return depictedItemList;
        } catch (final ClassCastException | NullPointerException ignore) {
            return new ArrayList<>();
        }
    }

    /**
     * Fetch caption and depictions from the MediaWiki API
     * @param filename the filename we will return the caption for
     * @return a map containing caption and depictions (empty string in the map if no caption/depictions)
     */
 private Single<JsonObject> getDepictions(final String filename)  {
         return mediaClient.getCaptionAndDepictions(filename)
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
