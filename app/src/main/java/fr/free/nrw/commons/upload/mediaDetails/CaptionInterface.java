package fr.free.nrw.commons.upload.mediaDetails;

import java.util.Observable;

import fr.free.nrw.commons.mwapi.CustomApiResult;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface CaptionInterface {

    @FormUrlEncoded
    @POST("/w/api.php?action=wbsetlabel&format=json&bot=1")
    Call<CustomApiResult> addLabelstoWikidata(@Field("id") String FileEntityId,
                                              @Field("token") String token,
                                              @Field("language") String language,
                                              @Field("value") String value);
}
