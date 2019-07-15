package fr.free.nrw.commons.actions;

import androidx.annotation.NonNull;

import org.wikipedia.edit.Edit;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import static org.wikipedia.dataclient.Service.MW_API_PREFIX;

public interface PageEditInterface {

    @FormUrlEncoded
    @Headers("Cache-Control: no-cache")
    @POST(MW_API_PREFIX + "action=edit")
    @NonNull
    Observable<Edit> postEdit(@NonNull @Field("title") String title,
                              @NonNull @Field("summary") String summary,
                              @NonNull @Field("text") String text,
                              @NonNull @Field("token") String token);

    @FormUrlEncoded
    @Headers("Cache-Control: no-cache")
    @POST(MW_API_PREFIX + "action=edit")
    @NonNull Observable<Edit> postAppendEdit(@NonNull @Field("title") String title,
                                             @NonNull @Field("summary") String summary,
                                             @NonNull @Field("appendtext") String text,
                                             @NonNull @Field("token") String token);

    @FormUrlEncoded
    @Headers("Cache-Control: no-cache")
    @POST(MW_API_PREFIX + "action=edit")
    @NonNull Observable<Edit> postPrependEdit(@NonNull @Field("title") String title,
                                              @NonNull @Field("summary") String summary,
                                              @NonNull @Field("prependtext") String text,
                                              @NonNull @Field("token") String token);
}
