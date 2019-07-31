package fr.free.nrw.commons.upload.mediaDetails;


import java.util.Map;

import fr.free.nrw.commons.mwapi.CustomApiResult;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface CaptionInterface {

    @FormUrlEncoded
    @POST("/w/api.php?action=wbsetlabel&format=json&language=en&value=Testcaptions")
    Call<CustomApiResult> addLabelstoWikidata(@Field("id") String FileEntityId,
                                              @Field("token") String editToken,
                                              @Field("data") Map<String, String> caption);
}
