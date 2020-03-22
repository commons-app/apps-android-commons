package fr.free.nrw.commons.upload.mediaDetails;


import org.wikipedia.dataclient.mwapi.MwPostResponse;

import java.util.Map;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

import static org.wikipedia.dataclient.Service.MW_API_PREFIX;

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
    @POST(MW_API_PREFIX + "action=wbsetlabel&language=en")
    Observable<MwPostResponse> addLabelstoWikidata(@Field("id") String FileEntityId,
                                                   @Field("token") String editToken,
                                                   @Field("value") String captionValue,
                                                   @Field("data") Map<String, String> caption);
}
