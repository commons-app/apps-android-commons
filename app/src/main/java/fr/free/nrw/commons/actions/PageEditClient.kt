package fr.free.nrw.commons.actions

import com.google.gson.JsonParseException
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient
import fr.free.nrw.commons.auth.csrf.InvalidLoginTokenException
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.ExecutionException

/**
 * This class acts as a Client to facilitate wiki page editing
 * services to various dependency providing modules such as the Network module, the Review Controller, etc.
 *
 * The methods provided by this class will post to the Media wiki api
 * documented at: https://commons.wikimedia.org/w/api.php?action=help&modules=edit
 */
class PageEditClient(
    private val csrfTokenClient: CsrfTokenClient,
    private val pageEditInterface: PageEditInterface,
) {
    /**
     * Replace the content of a wiki page
     * @param pageTitle   Title of the page to edit
     * @param text        Holds the page content
     * @param summary     Edit summary
     * @return whether the edit was successful
     */
    fun edit(
        pageTitle: String,
        text: String,
        summary: String,
    ): Observable<Boolean> =
        executeEdit("Failed to edit the page", false) { token ->
            pageEditInterface
                .postEdit(pageTitle, summary, text, token)
                .map { editResponse ->
                    editResponse.getEditResult()!!.editSucceeded()
                }
        }

    /**
     * Creates a new page with the given title, text, and summary.
     *
     * @param pageTitle The title of the page to be created.
     * @param text      The content of the page in wikitext format.
     * @param summary   The edit summary for the page creation.
     * @return An observable that emits true if the page creation succeeded, false otherwise.
     * @throws InvalidLoginTokenException If an invalid login token is encountered during the process.
     */
    fun postCreate(
        pageTitle: String,
        text: String,
        summary: String,
    ): Observable<Boolean> =
        executeEdit("Failed to create the page", false) { token ->
            pageEditInterface
                .postCreate(
                    pageTitle,
                    summary,
                    text,
                    "text/x-wiki",
                    "wikitext",
                    true,
                    true,
                    token,
                ).map { editResponse ->
                    editResponse.getEditResult()!!.editSucceeded()
                }
        }

    /**
     * Append text to the end of a wiki page
     * @param pageTitle   Title of the page to edit
     * @param appendText  The received page content is added to the end of the page
     * @param summary     Edit summary
     * @return whether the edit was successful
     */
    fun appendEdit(
        pageTitle: String,
        appendText: String,
        summary: String,
    ): Observable<Boolean> =
        executeEdit("Failed to append the page", false) { token ->
            pageEditInterface
                .postAppendEdit(pageTitle, summary, appendText, token)
                .map { editResponse -> editResponse.getEditResult()!!.editSucceeded() }
        }

    /**
     * Prepend text to the beginning of a wiki page
     * @param pageTitle   Title of the page to edit
     * @param prependText The received page content is added to the beginning of the page
     * @param summary     Edit summary
     * @return whether the edit was successful
     */
    fun prependEdit(
        pageTitle: String,
        prependText: String,
        summary: String,
    ): Observable<Boolean> =
        executeEdit("Failed to prepend the page", false) { token ->
            pageEditInterface
                .postPrependEdit(pageTitle, summary, prependText, token)
                .map { editResponse -> editResponse.getEditResult()?.editSucceeded() ?: false }
        }

    /**
     * Appends a new section to the wiki page
     * @param pageTitle   Title of the page to edit
     * @param sectionTitle Title of the new section that needs to be created
     * @param sectionText  The page content that is to be added to the section
     * @param summary     Edit summary
     * @return whether the edit was successful
     */
    fun createNewSection(
        pageTitle: String,
        sectionTitle: String,
        sectionText: String,
        summary: String,
    ): Observable<Boolean> =
        executeEdit("Failed to create the new section", false) { token ->
            pageEditInterface
                .postNewSection(pageTitle, summary, sectionTitle, sectionText, token)
                .map { editResponse -> editResponse.getEditResult()!!.editSucceeded() }
        }

    /**
     * Set new labels to Wikibase server of commons
     * @param summary   Edit summary
     * @param title Title of the page to edit
     * @param language  Corresponding language of label
     * @param value label
     * @return 1 when the edit was successful
     */
    fun setCaptions(
        summary: String,
        title: String,
        language: String,
        value: String,
    ): Observable<Int> = setCaptions(summary, title, language, value, null)

    /**
     * @param entityId MediaInfo id (M + page id) when the file has already been uploaded.
     * Falls back to site and title when the local media object still has a temporary page id.
     */
    fun setCaptions(
        summary: String,
        title: String,
        language: String,
        value: String,
        entityId: String?,
    ): Observable<Int> =
        executeEdit("Failed to set the captions", 0) { token ->
            val response =
                if (entityId != null) {
                    pageEditInterface.postCaptionsByEntityId(summary, entityId, language, value, token)
                } else {
                    pageEditInterface.postCaptions(summary, title, language, value, token)
                }
            response.map { it.success }
        }

    /**
     * Get whole WikiText of required file
     * @param title : Name of the file
     * @return Observable<MwQueryResult>
     */
    fun getCurrentWikiText(title: String): Single<String?> =
        pageEditInterface.getWikiText(title).map {
            it
                .query()
                ?.pages()
                ?.get(0)
                ?.revisions()
                ?.get(0)
                ?.content()
        }

    private fun <T> executeEdit(
        errorMessage: String,
        fallback: T,
        block: (String) -> Observable<T>,
    ): Observable<T> =
        try {
            block(csrfTokenClient.getTokenBlocking())
        } catch (e: InvalidLoginTokenException) {
            throw e
        } catch (e: IOException) {
            Timber.e(e, errorMessage)
            Observable.just(fallback)
        } catch (e: JsonParseException) {
            Timber.e(e, errorMessage)
            Observable.just(fallback)
        } catch (e: ExecutionException) {
            Timber.e(e, errorMessage)
            Observable.just(fallback)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Timber.e(e, errorMessage)
            Observable.just(fallback)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, errorMessage)
            Observable.just(fallback)
        } catch (e: IllegalStateException) {
            Timber.e(e, errorMessage)
            Observable.just(fallback)
        }
}
