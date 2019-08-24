package fr.free.nrw.commons.upload.mediaDetails;


import java.util.Map;

import fr.free.nrw.commons.mwapi.CustomApiResult;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface CaptionInterface {

    /**
     * Upload Captions for the image when upload is successful
     *
     * @param FileEntityId enityId for the uploaded file
     * @param editToken editToken for the file
     * @param captionValue value of the caption to be uploaded
     * @param caption additional data associated with caption
     */
    @FormUrlEncoded
    @POST("/w/api.php?action=wbsetlabel&format=json&language=en")
    Call<CustomApiResult> addLabelstoWikidata(@Field("id") String FileEntityId,
                                              @Field("token") String editToken,
                                              @Field("value") String captionValue,
                                              @Field("data") Map<String, String> caption);
}
