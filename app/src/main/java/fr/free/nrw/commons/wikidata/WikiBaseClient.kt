package fr.free.nrw.commons.wikidata

import fr.free.nrw.commons.auth.csrf.CsrfTokenClient
import fr.free.nrw.commons.di.NetworkingModule
import fr.free.nrw.commons.media.PAGE_ID_PREFIX
import fr.free.nrw.commons.upload.UploadResult
import fr.free.nrw.commons.upload.WikiBaseInterface
import fr.free.nrw.commons.wikidata.mwapi.MwPostResponse
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import io.reactivex.Observable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Wikibase Client for calling WikiBase APIs
 */
@Singleton
class WikiBaseClient @Inject constructor(
    private val wikiBaseInterface: WikiBaseInterface,
    @param:Named(NetworkingModule.NAMED_COMMONS_CSRF) private val csrfTokenClient: CsrfTokenClient
) {
    fun postEditEntity(fileEntityId: String?, data: String?): Observable<Boolean> {
        return csrfToken().switchMap { editToken ->
            wikiBaseInterface.postEditEntity(fileEntityId!!, editToken, data!!)
                .map { response: MwPostResponse -> response.successVal == 1 }
        }
    }

    /**
     * Makes the server call for posting new depicts
     *
     * @param filename name of the file
     * @param data data of the depicts to be uploaded
     * @return Observable<Boolean>
    </Boolean> */
    fun postEditEntityByFilename(filename: String?, data: String?): Observable<Boolean> {
        return csrfToken().switchMap { editToken ->
            wikiBaseInterface.postEditEntityByFilename(filename!!, editToken, data!!)
                .map { response: MwPostResponse -> response.successVal == 1 }
        }
    }

    fun getFileEntityId(uploadResult: UploadResult): Observable<Long> {
        return wikiBaseInterface.getFileEntityId(uploadResult.createCanonicalFileName())
            .map { response: MwQueryResponse -> response.query()!!.pages()!![0].pageId().toLong() }
    }

    fun addLabelstoWikidata(fileEntityId: Long, languageCode: String?, captionValue: String?): Observable<MwPostResponse> {
        return csrfToken().switchMap { editToken ->
            wikiBaseInterface.addLabelstoWikidata(
                PAGE_ID_PREFIX + fileEntityId,
                editToken,
                languageCode,
                captionValue
            )
        }
    }

    private fun csrfToken(): Observable<String> = Observable.fromCallable {
        try {
            csrfTokenClient.getTokenBlocking()
        } catch (throwable: Throwable) {
            Timber.e(throwable)
            ""
        }
    }
}
