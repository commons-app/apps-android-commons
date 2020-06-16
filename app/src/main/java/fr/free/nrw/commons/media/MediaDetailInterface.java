package fr.free.nrw.commons.media;

import io.reactivex.Observable;
import org.wikipedia.wikidata.Entities;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface for interacting with Commons Structured Data related APIs
 */
public interface MediaDetailInterface {

  /**
   * Fetches entity using file name
   *
   * @param filename name of the file to be used for fetching captions
   */
  @GET("w/api.php?action=wbgetentities&props=labels&format=json&languagefallback=1&sites=commonswiki")
  Observable<Entities> fetchEntitiesByFileName(@Query("languages") String language,
      @Query("titles") String filename);

  /**
   * Gets labels for Depictions using Entity Id from MediaWikiAPI
   *
   * @param entityId EntityId (Ex: Q81566) of the depict entity
   */
  @GET("/w/api.php?format=json&action=wbgetentities&props=labels&languagefallback=1")
  Observable<Entities> getEntity(@Query("ids") String entityId);

  /**
   * Fetches caption using wikibaseIdentifier
   *
   * @param wikibaseIdentifier pageId for the media
   */
  @GET("/w/api.php?action=wbgetentities&props=labels&format=json&languagefallback=1&sites=commonswiki")
  Observable<Entities> getEntityForImage(@Query("languages") String language,
      @Query("ids") String wikibaseIdentifier);
}
