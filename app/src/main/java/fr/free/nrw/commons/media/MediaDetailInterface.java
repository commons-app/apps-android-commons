package fr.free.nrw.commons.media;

import com.google.gson.JsonObject;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface for interacting with Commons Structured Data related APIs
 */
public interface MediaDetailInterface {

    /**
     * Fetches caption using file name
     *
     * @param filename name of the file to be used for fetching captions
     * Please note that languages=en does not have an impact on the languages returned. All captions are returned for all languages.
     */
    @GET("w/api.php?action=wbgetentities&props=labels&format=json&languagefallback=1&sites=commonswiki")
    Observable<MediaDetailResponse> fetchStructuredDataByFilename(@Query("languages") String language, @Query("titles") String filename);

    /**
     * Gets labels for Depictions using Entity Id from MediaWikiAPI
     *
     * @param entityId  EntityId (Ex: Q81566) of the depict entity
     * @param language user's locale
     */
    @GET("/w/api.php?format=json&action=wbgetentities&props=labels&languagefallback=1")
    Observable<JsonObject> getDepictions(@Query("ids") String entityId, @Query("languages") String language);

    /**
     * Fetches caption using wikibaseIdentifier
     *
     * @param wikibaseIdentifier pageId for the media
     */
    @GET("/w/api.php?action=wbgetentities&props=labels&format=json&languagefallback=1&sites=commonswiki")
    Observable<MediaDetailResponse> getCaptionForImage(@Query("languages") String language, @Query("ids") String wikibaseIdentifier);
}
