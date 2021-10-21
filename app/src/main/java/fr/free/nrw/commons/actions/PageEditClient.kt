package fr.free.nrw.commons.actions

import io.reactivex.Observable
import io.reactivex.Single
import org.wikipedia.csrf.CsrfTokenClient

/**
 * This class acts as a Client to facilitate wiki page editing
 * services to various dependency providing modules such as the Network module, the Review Controller, etc.
 *
 * The methods provided by this class will post to the Media wiki api
 * documented at: https://commons.wikimedia.org/w/api.php?action=help&modules=edit
 */
class PageEditClient(
    private val csrfTokenClient: CsrfTokenClient,
    private val pageEditInterface: PageEditInterface
) {

    /**
     * Replace the content of a wiki page
     * @param pageTitle   Title of the page to edit
     * @param text        Holds the page content
     * @param summary     Edit summary
     * @return whether the edit was successful
     */
    fun edit(pageTitle: String, text: String, summary: String): Observable<Boolean> {
        return try {
            pageEditInterface.postEdit(pageTitle, summary, text, csrfTokenClient.tokenBlocking)
                .map { editResponse -> editResponse.edit()!!.editSucceeded() }
        } catch (throwable: Throwable) {
            Observable.just(false)
        }
    }

    /**
     * Append text to the end of a wiki page
     * @param pageTitle   Title of the page to edit
     * @param appendText  The received page content is added to the end of the page
     * @param summary     Edit summary
     * @return whether the edit was successful
     */
    fun appendEdit(pageTitle: String, appendText: String, summary: String): Observable<Boolean> {
        return try {
            pageEditInterface.postAppendEdit(pageTitle, summary, appendText, csrfTokenClient.tokenBlocking)
                .map { editResponse -> editResponse.edit()!!.editSucceeded() }
        } catch (throwable: Throwable) {
            Observable.just(false)
        }
    }

    /**
     * Prepend text to the beginning of a wiki page
     * @param pageTitle   Title of the page to edit
     * @param prependText The received page content is added to the beginning of the page
     * @param summary     Edit summary
     * @return whether the edit was successful
     */
    fun prependEdit(pageTitle: String, prependText: String, summary: String): Observable<Boolean> {
        return try {
            pageEditInterface.postPrependEdit(pageTitle, summary, prependText, csrfTokenClient.tokenBlocking)
                .map { editResponse -> editResponse.edit()!!.editSucceeded() }
        } catch (throwable: Throwable) {
            Observable.just(false)
        }
    }

    fun setCaptions(summary: String, title: String,
                    language: String, value: String) : Observable<Int>{
        return try {
            pageEditInterface.postCaptions(summary, title, language, value, csrfTokenClient.tokenBlocking)
                .map { it.success }
        } catch (throwable: Throwable) {
            Observable.just(0)
        }
    }

    /**
     * Get whole WikiText of required file
     * @param title : Name of the file
     * @return Observable<MwQueryResult>
     */
    fun getCurrentWikiText(title: String): Single<String?> {
        return pageEditInterface.getWikiText(title).map {
            it.query()?.pages()?.get(0)?.revisions()?.get(0)?.content()
        }
    }
}