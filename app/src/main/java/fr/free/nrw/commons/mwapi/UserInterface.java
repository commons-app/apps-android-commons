package fr.free.nrw.commons.mwapi;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import java.util.Map;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

import static org.wikipedia.dataclient.Service.MW_API_PREFIX;

public interface UserInterface {

    /**
     * Gets the log events of user
     * @param user name of user without prefix
     * @param continuation continuation params returned in previous query
     * @return query response
     */

    @GET(MW_API_PREFIX+"action=query&list=logevents&letype=upload&leprop=title|timestamp|ids&lelimit=500")
    Observable<MwQueryResponse> getUserLogEvents(@Query("leuser") String user, @QueryMap Map<String, String> continuation);

    /**
     * Checks to see if a user is currently blocked from Commons
     */
    @GET(MW_API_PREFIX + "action=query&meta=userinfo&uiprop=blockinfo")
    Observable<MwQueryResponse> getUserBlockInfo();
}
