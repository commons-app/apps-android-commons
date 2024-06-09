package fr.free.nrw.commons.actions

import fr.free.nrw.commons.auth.csrf.InvalidLoginTokenException
import io.reactivex.Observable
import io.reactivex.Single
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient

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
            pageEditInterface.postEdit(pageTitle, summary, text, csrfTokenClient.getTokenBlocking())
                .map { editResponse -> editResponse.edit()!!.editSucceeded() }
        } catch (throwable: Throwable) {
            if (throwable is InvalidLoginTokenException) {
                throw throwable
            } else {
                Observable.just(false)
            }
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
            pageEditInterface.postAppendEdit(pageTitle, summary, appendText, csrfTokenClient.getTokenBlocking())
                .map { editResponse -> editResponse.edit()!!.editSucceeded() }
        } catch (throwable: Throwable) {
            if (throwable is InvalidLoginTokenException) {
                throw throwable
            } else {
                Observable.just(false)
            }
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
            pageEditInterface.postPrependEdit(pageTitle, summary, prependText, csrfTokenClient.getTokenBlocking())
                .map { editResponse -> editResponse.edit()?.editSucceeded() ?: false }
        } catch (throwable: Throwable) {
            if (throwable is InvalidLoginTokenException) {
                throw throwable
            } else {
                Observable.just(false)
            }
        }
    }


    /**
     * Appends a new section to the wiki page
     * @param pageTitle   Title of the page to edit
     * @param sectionTitle Title of the new section that needs to be created
     * @param sectionText  The page content that is to be added to the section
     * @param summary     Edit summary
     * @return whether the edit was successful
     */
    fun createNewSection(pageTitle: String, sectionTitle: String, sectionText: String, summary: String): Observable<Boolean> {
        return try {
            pageEditInterface.postNewSection(pageTitle, summary, sectionTitle, sectionText, csrfTokenClient.getTokenBlocking())
                .map { editResponse -> editResponse.edit()!!.editSucceeded() }
        } catch (throwable: Throwable) {
            if (throwable is InvalidLoginTokenException) {
                throw throwable
            } else {
                Observable.just(false)
            }
        }
    }


    /**
     * Set new labels to Wikibase server of commons
     * @param summary   Edit summary
     * @param title Title of the page to edit
     * @param language  Corresponding language of label
     * @param value label
     * @return 1 when the edit was successful
     */
    fun setCaptions(summary: String, title: String,
                    language: String, value: String) : Observable<Int>{
        return try {
            pageEditInterface.postCaptions(summary, title, language,
                value, csrfTokenClient.getTokenBlocking()
            ).map { it.success }
        } catch (throwable: Throwable) {
            if (throwable is InvalidLoginTokenException) {
                throw throwable
            } else {
                Observable.just(0)
            }
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
