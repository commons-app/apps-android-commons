package fr.free.nrw.commons.media;

import io.reactivex.Single;
import org.wikipedia.wikidata.Entities;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface for interacting with Commons Structured Data related APIs
 */
public interface MediaDetailInterface {

    /**
     * Gets labels for Depictions using Entity Id from MediaWikiAPI
     *  @param entityId  EntityId (Ex: Q81566) of the depict entity
     *
     */
    @GET("/w/api.php?format=json&action=wbgetentities&props=labels&languagefallback=1")
    Single<Entities> getEntity(@Query("ids") String entityId);

}
