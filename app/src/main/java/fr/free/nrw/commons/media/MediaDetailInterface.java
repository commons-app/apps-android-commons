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
    @GET("w/api.php?action=wbgetentities&props=labels&format=json&languagefallback=1&languages=en&sites=commonswiki")
    Observable<MediaDetailResponse> fetchCaptionByFilename(@Query("titles") String filename);
}
