package fr.free.nrw.commons;

import androidx.core.text.HtmlCompat;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
     * It fetches media object, deletion status, talk page and captions for the filename
     * @param filename for which the details are to be fetched
     * @return full Media object with all details including deletion status and talk page
     */
    public Single<Media> fetchMediaDetails(String filename, String pageId) {
        Single<Media> mediaSingle = getMediaFromFileName(filename);
        Single<Boolean> pageExistsSingle = mediaClient.checkPageExistsUsingTitle("Commons:Deletion_requests/" + filename);
        Single<String> discussionSingle = getDiscussion(filename);
        Single<String> captionSingle = getCaption("M"+pageId);
        Single<JsonObject> depictionSingle = getDepictions(filename);
        return Single.zip(mediaSingle, pageExistsSingle, discussionSingle, captionSingle, depictionSingle, (media, deletionStatus, discussion, caption, depiction) -> {
            media.setDiscussion(discussion);
            media.setCaption(caption);
            media.setDepiction(formatDepictions(depiction));
            if (deletionStatus) {
                media.setRequestedDeletion();
            }
            return media;
        });
    }

    /**
     * Obtains captions using filename
     * @param wikibaseIdentifier
     *
     * @return caption for the image in user's locale
     * Ex: "a nice painting" (english locale) and "No Caption" in case the caption is not available for the image
     */
    private Single<String> getCaption(String wikibaseIdentifier) {
        return mediaClient.getCaptionByWikibaseIdentifier(wikibaseIdentifier);
    }

    /**
     * From the Json Object extract depictions into an array list
     * @param mediaResponse
     * @return List containing map for depictions, the map has two keys,
     *  first key is for the label and second is for the url of the item
     */
    private ArrayList<Map<String, String>> formatDepictions(JsonObject mediaResponse) {
        try {
            JsonArray depictionArray = (JsonArray) mediaResponse.get("Depiction");
            ArrayList<Map<String, String>> depictedItemList = new ArrayList<>();
            try {
                for (int i = 0; i <depictionArray.size() ; i++) {
                    JsonObject depictedItem = (JsonObject) depictionArray.get(i);
                    Map <String, String> depictedObject = new HashMap<>();
                    String label = depictedItem.get("label").toString();
                    String id =  depictedItem.get("id").toString();
                    String transformedLabel = label.substring(3, label.length()-3);
                    String transformedId = id.substring(1,id.length() - 1);
                    depictedObject.put("label", transformedLabel); //remove the additional characters obtained in label and ID object to extract the relevant string (since the string also contains extra quites that are not required)
                    depictedObject.put("id", transformedId);
                    depictedItemList.add(depictedObject);
                }
                return depictedItemList;
            } catch (NullPointerException e) {
                return new ArrayList<>();
            }
        } catch (ClassCastException c) {
            return new ArrayList<>();
        }
    }

    /**
     * Fetch caption and depictions from the MediaWiki API
     * @param filename the filename we will return the caption for
     * @return a map containing caption and depictions (empty string in the map if no caption/depictions)
     */
 private Single<JsonObject> getDepictions(String filename)  {
         return mediaClient.getCaptionAndDepictions(filename)
                .map(mediaResponse -> {
                    return mediaResponse;
                }).doOnError(throwable -> {
                    Timber.e(throwable+ "error while fetching depictions");
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
