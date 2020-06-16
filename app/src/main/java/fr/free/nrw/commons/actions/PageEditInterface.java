package fr.free.nrw.commons.actions;

import static org.wikipedia.dataclient.Service.MW_API_PREFIX;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import org.wikipedia.edit.Edit;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * This interface facilitates wiki commons page editing services to the Networking module which
 * provides all network related services used by the app.
 * <p>
 * This interface posts a form encoded request to the wikimedia API with editing action as argument
 * to edit a particular page
 */
public interface PageEditInterface {

  /**
   * This method posts such that the Content which the page has will be completely replaced by the
   * value being passed to the "text" field of the encoded form data
   *
   * @param title   Title of the page to edit. Cannot be used together with pageid.
   * @param summary Edit summary. Also section title when section=new and sectiontitle is not set
   * @param text    Holds the page content
   * @param token   A "csrf" token
   */
  @FormUrlEncoded
  @Headers("Cache-Control: no-cache")
  @POST(MW_API_PREFIX + "action=edit")
  @NonNull
  Observable<Edit> postEdit(@NonNull @Field("title") String title,
      @NonNull @Field("summary") String summary,
      @NonNull @Field("text") String text,
      // NOTE: This csrf shold always be sent as the last field of form data
      @NonNull @Field("token") String token);

  /**
   * This method posts such that the Content which the page has will be completely replaced by the
   * value being passed to the "text" field of the encoded form data
   *
   * @param title   Title of the page to edit. Cannot be used together with pageid.
   * @param summary Edit summary. Also section title when section=new and sectiontitle is not set
   * @param text    The received page content is added to beginning of the page
   * @param token   A "csrf" token
   */
  @FormUrlEncoded
  @Headers("Cache-Control: no-cache")
  @POST(MW_API_PREFIX + "action=edit")
  @NonNull
  Observable<Edit> postAppendEdit(@NonNull @Field("title") String title,
      @NonNull @Field("summary") String summary,
      @NonNull @Field("appendtext") String text,
      @NonNull @Field("token") String token);

  /**
   * This method posts such that the Content which the page has will be completely replaced by the
   * value being passed to the "text" field of the encoded form data
   *
   * @param title   Title of the page to edit. Cannot be used together with pageid.
   * @param summary Edit summary. Also section title when section=new and sectiontitle is not set
   * @param text    The received page content is added to beginning of the page
   * @param token   A "csrf" token
   */
  @FormUrlEncoded
  @Headers("Cache-Control: no-cache")
  @POST(MW_API_PREFIX + "action=edit")
  @NonNull
  Observable<Edit> postPrependEdit(@NonNull @Field("title") String title,
      @NonNull @Field("summary") String summary,
      @NonNull @Field("prependtext") String text,
      @NonNull @Field("token") String token);
}
