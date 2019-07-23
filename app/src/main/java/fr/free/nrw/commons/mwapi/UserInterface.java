package fr.free.nrw.commons.mwapi;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import io.reactivex.Observable;
import retrofit2.http.GET;

import static org.wikipedia.dataclient.Service.MW_API_PREFIX;

public interface UserInterface {

    /**
     * Checks to see if a user is currently blocked from Commons
     */
    @GET(MW_API_PREFIX + "action=query&meta=userinfo&uiprop=blockinfo")
    Observable<MwQueryResponse> getUserBlockInfo();
}
