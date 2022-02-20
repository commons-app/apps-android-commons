package fr.free.nrw.commons.feedback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface FeedbackService {

    @FormUrlEncoded
    @POST("w/api.php?action=discussiontoolsedit&format=json&formatversion=2&uselang=en&list=linterrors&lntcategories=fostered&lntlimit=1&lnttitle=Commons%3AMobile_app%2FFeedback")
    Call<Void> postFeedback(
        @Nullable @Field("summary") String summary,
        @Nullable @Field("paction") String paction,
        @Nullable @Field("page") String page,
        @Nullable @Field("sectiontitle") String sectiontitle,
        @Nullable @Field("wikitext") String wikitext,
        @Nullable @Field("token") String token
    );
}
