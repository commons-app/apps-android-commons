package fr.free.nrw.commons.category;

import io.reactivex.Observable;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import static org.wikipedia.dataclient.Service.MW_API_PREFIX;


public interface CategoryEditInterface {

    //@GET(MW_API_PREFIX +"action=query&format=json&prop=revisions&formatversion=2&rvprop=content&rvslots=*")
    @GET(MW_API_PREFIX +"action=query&prop=description")
    Observable<MwQueryResponse> getContentOfFile(@Query("titles") String titles);
}
