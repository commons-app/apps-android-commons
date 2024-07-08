package fr.free.nrw.commons.actions

import fr.free.nrw.commons.wikidata.WikidataConstants.MW_API_PREFIX
import fr.free.nrw.commons.wikidata.model.Entities
import fr.free.nrw.commons.wikidata.model.edit.Edit
import io.reactivex.Observable
import io.reactivex.Single
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
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
    @POST(MW_API_PREFIX + "action=edit")
    fun postEdit(
        @Field("title") title: String,
        @Field("summary") summary: String,
        @Field("text") text: String,
        // NOTE: This csrf shold always be sent as the last field of form data
        @Field("token") token: String
    ): Observable<Edit>

    /**
     * This method creates or edits a page for nearby items.
     *
     * @param title           Title of the page to edit. Cannot be used together with pageid.
     * @param summary         Edit summary. Also used as the section title when section=new and sectiontitle is not set.
     * @param text            Text of the page.
     * @param contentformat   Format of the content (e.g., "text/x-wiki").
     * @param contentmodel    Model of the content (e.g., "wikitext").
     * @param minor           Whether the edit is a minor edit.
     * @param recreate        Whether to recreate the page if it does not exist.
     * @param token           A "csrf" token. This should always be sent as the last field of form data.
     */
    @FormUrlEncoded
    @Headers("Cache-Control: no-cache")
    @POST(MW_API_PREFIX + "action=edit")
    fun postCreate(
        @Field("title") title: String,
        @Field("summary") summary: String,
        @Field("text") text: String,
        @Field("contentformat") contentformat: String,
        @Field("contentmodel") contentmodel: String,
        @Field("minor") minor: Boolean,
        @Field("recreate") recreate: Boolean,
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
    @POST(MW_API_PREFIX + "action=edit")
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
    @POST(MW_API_PREFIX + "action=edit")
    fun postPrependEdit(
        @Field("title") title: String,
        @Field("summary") summary: String,
        @Field("prependtext") prependText: String,
        @Field("token") token: String
    ): Observable<Edit>

    @FormUrlEncoded
    @Headers("Cache-Control: no-cache")
    @POST(MW_API_PREFIX + "action=edit&section=new")
    fun postNewSection(
        @Field("title") title: String,
        @Field("summary") summary: String,
        @Field("sectiontitle") sectionTitle: String,
        @Field("text") sectionText: String,
        @Field("token") token: String
    ): Observable<Edit>

    @FormUrlEncoded
    @Headers("Cache-Control: no-cache")
    @POST(MW_API_PREFIX + "action=wbsetlabel&format=json&site=commonswiki&formatversion=2")
    fun postCaptions(
        @Field("summary") summary: String,
        @Field("title") title: String,
        @Field("language") language: String,
        @Field("value") value: String,
        @Field("token") token: String
    ): Observable<Entities>

    /**
     * Get wiki text for provided file names
     * @param titles : Name of the file
     * @return Single<MwQueryResult>
     */
    @GET(MW_API_PREFIX + "action=query&prop=revisions&rvprop=content|timestamp&rvlimit=1&converttitles=")
    fun getWikiText(
        @Query("titles") title: String
    ): Single<MwQueryResponse?>
}