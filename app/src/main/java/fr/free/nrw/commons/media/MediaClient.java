package fr.free.nrw.commons.media;


import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.LinkedTreeMap;

import java.util.Date;


import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.utils.CommonsDateUtil;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Media Client to handle custom calls to Commons MediaWiki APIs
 */
@Singleton
public class MediaClient {

    private final MediaInterface mediaInterface;
    private final MediaDetailInterface mediaDetailInterface;

    //OkHttpJsonApiClient used JsonKvStore for this. I don't know why.
    private Map<String, Map<String, String>> continuationStore;
    private static final String NO_CAPTION = "No caption";
    private static final String NO_DEPICTION = "No depiction";

    @Inject
    public MediaClient(MediaInterface mediaInterface, MediaDetailInterface mediaDetailInterface) {
        this.mediaInterface = mediaInterface;
        this.mediaDetailInterface = mediaDetailInterface;
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
     * This method takes a keyword as input and returns a list of  Media objects filtered using image generator query
     * It uses the generator query API to get the images searched using a query, 10 at a time.
     *
     * @param keyword the search keyword
     * @return
     */
    public Single<List<Media>> getMediaListFromSearch(String keyword) {
        return responseToMediaList(
                continuationStore.containsKey("search_" + keyword) && (continuationStore.get("search_" + keyword)  != null) ?
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


    /**
     * @return  caption for image using wikibaseIdentifier
     */
    public Single<String> getCaptionByWikibaseIdentifier(String wikibaseIdentifier) {
        return mediaDetailInterface.getCaptionForImage(Locale.getDefault().getLanguage(), wikibaseIdentifier)
                .map(mediaDetailResponse -> {
                    if (mediaDetailResponse != null && mediaDetailResponse.getSuccess() != null && mediaDetailResponse.getSuccess() == 1 && mediaDetailResponse.getEntities() != null) {
                        Map<String, CommonsWikibaseItem> entities = mediaDetailResponse.getEntities();
                        try {
                            Map.Entry<String, CommonsWikibaseItem> entry = entities.entrySet().iterator().next();
                            CommonsWikibaseItem commonsWikibaseItem = entry.getValue();
                                Map<String, Caption> labels = commonsWikibaseItem.getLabels();
                                Map.Entry<String, Caption> captionEntry = labels.entrySet().iterator().next();
                                Caption caption = captionEntry.getValue();
                                return caption.getValue();

                        } catch (Exception e) {
                            return NO_CAPTION;
                        }
                    }
                        return NO_CAPTION;

                })
                .singleOrError();
    }

    /**
     * Fetches Structured data from API
     *
     * @param filename
     * @return a map containing caption and depictions (empty string in the map if no caption/depictions)
     */
    public Single<JsonObject> getCaptionAndDepictions(String filename)  {
        return mediaDetailInterface.fetchStructuredDataByFilename(Locale.getDefault().getLanguage(), filename)
                .map(mediaDetailResponse -> {
                        return fetchCaptionandDepictionsFromMediaDetailResponse(mediaDetailResponse);
                })
                .singleOrError();
    }

    /**
     * Parses the mediaDetailResponse from API to extract captions and depictions
     * @param mediaDetailResponse Response obtained from API for Media Details
     * @return a map containing caption and depictions (empty string in the map if no caption/depictions)
     */
    @SuppressLint("CheckResult")
    private JsonObject fetchCaptionandDepictionsFromMediaDetailResponse(MediaDetailResponse mediaDetailResponse) {
        JsonObject mediaDetails = new JsonObject();
        if (mediaDetailResponse != null && mediaDetailResponse.getSuccess() != null && mediaDetailResponse.getSuccess() == 1 && mediaDetailResponse.getEntities() != null) {
            Map<String, CommonsWikibaseItem> entities = mediaDetailResponse.getEntities();
            try {
                Map.Entry<String, CommonsWikibaseItem> entry = entities.entrySet().iterator().next();
                CommonsWikibaseItem commonsWikibaseItem = entry.getValue();
                try {
                    Map<String, Caption> labels = commonsWikibaseItem.getLabels();
                    Map.Entry<String, Caption> captionEntry = labels.entrySet().iterator().next();
                    Caption caption = captionEntry.getValue();
                    JsonElement jsonElement = new JsonPrimitive(caption.getValue());
                    mediaDetails.add("Caption", jsonElement);
                } catch (Exception e) {
                    JsonElement jsonElement = new JsonPrimitive(NO_CAPTION);
                    mediaDetails.add("Caption", jsonElement);
                }

                try {
                    LinkedTreeMap statements = (LinkedTreeMap) commonsWikibaseItem.getStatements();
                    ArrayList<LinkedTreeMap> depictsItemList = (ArrayList<LinkedTreeMap>) statements.get(BuildConfig.DEPICTS_PROPERTY);
                    String depictions = null;
                    JsonArray jsonArray = new JsonArray();
                    for (int i = 0; i < depictsItemList.size(); i++) {
                        LinkedTreeMap depictedItem = depictsItemList.get(i);
                        LinkedTreeMap mainsnak = (LinkedTreeMap) depictedItem.get("mainsnak");
                        Map<String, LinkedTreeMap> datavalue = (Map<String, LinkedTreeMap>) mainsnak.get("datavalue");
                        LinkedTreeMap value = datavalue.get("value");
                        String id = value.get("id").toString();
                        JsonObject jsonObject = getLabelForDepiction(id, Locale.getDefault().getLanguage())
                                .subscribeOn(Schedulers.newThread())
                                .blockingGet();
                                jsonArray.add(jsonObject);
                    }
                    mediaDetails.add("Depiction", jsonArray);
                } catch (Exception e) {
                    JsonElement jsonElement = new JsonPrimitive(NO_DEPICTION);
                    mediaDetails.add("Depiction", jsonElement);
                }
            } catch (Exception e) {
                JsonElement jsonElement = new JsonPrimitive(NO_CAPTION);
                mediaDetails.add("Caption", jsonElement);
                jsonElement = null;
                jsonElement = new JsonPrimitive(NO_DEPICTION);
                mediaDetails.add("Depiction", jsonElement);
            }
        } else {
            JsonElement jsonElement = new JsonPrimitive(NO_CAPTION);
            mediaDetails.add("Caption", jsonElement);
            jsonElement = null;
            jsonElement = new JsonPrimitive(NO_DEPICTION);
            mediaDetails.add("Depiction", jsonElement);
        }

        return mediaDetails;
    }

    /**
     * Gets labels for Depictions using Entity Id from MediaWikiAPI
     *
     * @param entityId  EntityId (Ex: Q81566) of the depict entity
     * @return Json Object having label and Wikidata URL for the Depiction Entity
     */
    public Single<JsonObject> getLabelForDepiction(String entityId, String language) {
        return mediaDetailInterface.getDepictions(entityId, language)
                .map(jsonResponse -> {
                    try {
                        if (jsonResponse.get("success").toString().equals("1")) {
                            JsonObject entities = (JsonObject) jsonResponse.getAsJsonObject().get("entities");
                            JsonObject responseObject = (JsonObject) entities.getAsJsonObject().get(entityId);
                            JsonObject labels = responseObject.getAsJsonObject("labels");
                            JsonObject languageObject = labels.getAsJsonObject(language);
                            String label = String.valueOf(languageObject.get("value"));


                            JsonElement labelJson = new JsonPrimitive(label);
                            JsonElement idJson = new JsonPrimitive(entityId);
                            JsonObject jsonObject = new JsonObject();
                            jsonObject.add("label", labelJson);
                            jsonObject.add("id", idJson);
                            return jsonObject;
                        }
                    } catch (Exception e) {
                        Timber.e("Label not found");
                        return new JsonObject();
                    }return new JsonObject();
                })
                .singleOrError();
    }

    }

