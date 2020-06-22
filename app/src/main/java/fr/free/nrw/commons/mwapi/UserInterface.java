package fr.free.nrw.commons.mwapi;

import static org.wikipedia.dataclient.Service.MW_API_PREFIX;

import io.reactivex.Observable;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import retrofit2.http.GET;

public interface UserInterface {

    /**
     * Checks to see if a user is currently blocked from Commons
     */
    @GET(MW_API_PREFIX + "action=query&meta=userinfo&uiprop=blockinfo")
    Observable<MwQueryResponse> getUserBlockInfo();
}
