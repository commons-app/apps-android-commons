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
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
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
        Single<String> captionSingle = getCaption(filename);
        Single<JsonObject> captionAndDepictionJsonObjectSingle = getCaptionAndDepictions(filename);
        return Single.zip(mediaSingle, pageExistsSingle, discussionSingle, captionSingle, captionAndDepictionJsonObjectSingle, (media, deletionStatus, discussion,caption, captionAndDepictionJsonObject) -> {
            media.setDiscussion(discussion);
            String captionString = captionAndDepictionJsonObject.get("Caption").toString();
            media.setCaption(caption);
            media.setDepiction(formatDepictions(captionAndDepictionJsonObject));
            if (deletionStatus) {
                media.setRequestedDeletion();
            }
            return media;
        });
    }

    private Single<String> getCaption(String filename) {
        return mediaClient.getCaptionByFilename(filename);
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
                    depictedObject.put("label", depictedItem.get("label").toString().substring(3, depictedItem.get("label").toString().length()-3));
                    depictedObject.put("url", depictedItem.get("url").toString().substring(3, depictedItem.get("url").toString().length() - 3));
                    depictedObject.put("id", depictedItem.get("id").toString().substring(1, depictedItem.get("id").toString().length() - 1));
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
 private Single<JsonObject> getCaptionAndDepictions(String filename)  {
         return mediaClient.getCaptionAndDepictions(filename)
                .map(mediaResponse -> {
                    return mediaResponse;
                }).doOnError(throwable -> {
                    Timber.e(throwable+"eror while fetching captions");
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
        return mediaWikiApi.fetchMediaByFilename(filename.replace("File", "File talk"))
                .flatMap(mediaResult -> mediaWikiApi.parseWikicode(mediaResult.getWikiSource()))
                .map(discussion -> HtmlCompat.fromHtml(discussion, HtmlCompat.FROM_HTML_MODE_LEGACY).toString())
                .onErrorReturn(throwable -> {
                    Timber.e(throwable, "Error occurred while fetching discussion");
                    return "";
                });
    }
}
