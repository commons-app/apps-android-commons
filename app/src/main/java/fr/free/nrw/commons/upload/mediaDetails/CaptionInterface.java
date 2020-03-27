package fr.free.nrw.commons.upload.mediaDetails;


import static org.wikipedia.dataclient.Service.MW_API_PREFIX;

import io.reactivex.Observable;
import org.wikipedia.dataclient.mwapi.MwPostResponse;
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
     */
    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=wbsetlabel&language=en")
    Observable<MwPostResponse> addLabelstoWikidata(@Field("id") String FileEntityId,
                                                   @Field("token") String editToken,
                                                   @Field("language") String language,
                                                   @Field("value") String captionValue);
}
