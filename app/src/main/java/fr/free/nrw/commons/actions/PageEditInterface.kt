package fr.free.nrw.commons.actions

import io.reactivex.Observable
import io.reactivex.Single
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.edit.Edit
import retrofit2.http.*

/**
 * This interface facilitates wiki commons page editing services to the Networking module
 * which provides all network related services used by the app.
 *
 * This interface posts a form encoded request to the wikimedia API
 * with editing action as argument to edit a particular page
 */
interface PageEditInterface {
    /**
     * This method posts such that the Content which the page
     * has will be completely replaced by the value being passed to the
     * "text" field of the encoded form data
     * @param title    Title of the page to edit. Cannot be used together with pageid.
     * @param summary  Edit summary. Also section title when section=new and sectiontitle is not set
     * @param text     Holds the page content
     * @param token    A "csrf" token
     */
    @FormUrlEncoded
    @Headers("Cache-Control: no-cache")
    @POST(Service.MW_API_PREFIX + "action=edit")
    fun postEdit(
        @Field("title") title: String,
        @Field("summary") summary: String,
        @Field("text") text: String,
        // NOTE: This csrf shold always be sent as the last field of form data
        @Field("token") token: String
    ): Observable<Edit>

    /**
     * This method posts such that the Content which the page
     * has will be appended with the value being passed to the
     * "appendText" field of the encoded form data
     * @param title    Title of the page to edit. Cannot be used together with pageid.
     * @param summary  Edit summary. Also section title when section=new and sectiontitle is not set
     * @param appendText Text to add to the end of the page
     * @param token    A "csrf" token
     */
    @FormUrlEncoded
    @Headers("Cache-Control: no-cache")
    @POST(Service.MW_API_PREFIX + "action=edit")
    fun postAppendEdit(
        @Field("title") title: String,
        @Field("summary") summary: String,
        @Field("appendtext") appendText: String,
        @Field("token") token: String
    ): Observable<Edit>

    /**
     * This method posts such that the Content which the page
     * has will be prepended with the value being passed to the
     * "prependText" field of the encoded form data
     * @param title    Title of the page to edit. Cannot be used together with pageid.
     * @param summary  Edit summary. Also section title when section=new and sectiontitle is not set
     * @param prependText Text to add to the beginning of the page
     * @param token    A "csrf" token
     */
    @FormUrlEncoded
    @Headers("Cache-Control: no-cache")
    @POST(Service.MW_API_PREFIX + "action=edit")
    fun postPrependEdit(
        @Field("title") title: String,
        @Field("summary") summary: String,
        @Field("prependtext") prependText: String,
        @Field("token") token: String
    ): Observable<Edit>

    /**
     * Get wiki text for provided file names
     * @param titles : Name of the file
     * @return Single<MwQueryResult>
     */
    @GET(
        Service.MW_API_PREFIX +
                "action=query&prop=revisions&rvprop=content|timestamp&rvlimit=1&converttitles="
    )
    fun getWikiText(
        @Query("titles") title: String
    ): Single<MwQueryResponse?>
}