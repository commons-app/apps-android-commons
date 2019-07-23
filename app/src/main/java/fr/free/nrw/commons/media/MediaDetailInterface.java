package fr.free.nrw.commons.media;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MediaDetailInterface {

    /**
     * Fetches caption using file name
     *
     * @param filename name of the file to be used for fetching captions
     * */

    //Please note that languages=en does not have an impact on the languages returned. All captions are returned for all languages.
    @GET("w/api.php?action=wbgetentities&props=labels&format=json&languagefallback=1&sites=commonswiki")
    Observable<MediaDetailResponse> fetchCaptionByFilename(@Query("languages") String language, @Query("titles") String filename);
}
