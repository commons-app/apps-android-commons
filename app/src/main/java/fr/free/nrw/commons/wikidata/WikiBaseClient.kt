package fr.free.nrw.commons.wikidata

import fr.free.nrw.commons.auth.csrf.CsrfTokenClient
import fr.free.nrw.commons.di.NetworkingModule
import fr.free.nrw.commons.media.PAGE_ID_PREFIX
import fr.free.nrw.commons.upload.WikiBaseInterface
import fr.free.nrw.commons.wikidata.model.DataValue
import fr.free.nrw.commons.wikidata.model.StatementPartial
import fr.free.nrw.commons.wikidata.mwapi.MwException
import fr.free.nrw.commons.wikidata.mwapi.MwPostResponse
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

// Error code from Wikibase when the entity does not exist
private const val NO_SUCH_ENTITY = "no-such-entity"

/**
 * Wikibase Client for calling WikiBase APIs
 */
@Singleton
class WikiBaseClient
    @Inject
    constructor(
        private val wikiBaseInterface: WikiBaseInterface,
        @param:Named(NetworkingModule.NAMED_COMMONS_CSRF) private val csrfTokenClient: CsrfTokenClient,
    ) {
        fun postEditEntity(
            fileEntityId: String?,
            data: String?,
        ): Observable<Boolean> =
            csrfToken().switchMap { editToken ->
                wikiBaseInterface
                    .postEditEntity(fileEntityId!!, editToken, data!!)
                    .map { response: MwPostResponse -> response.successVal == 1 }
            }

        /**
         * Makes the server call for posting new depicts
         *
         * @param filename name of the file
         * @param data data of the depicts to be uploaded
         * @return Observable<Boolean>
         </Boolean> */
        fun postEditEntityByFilename(
            filename: String?,
            data: String?,
        ): Observable<Boolean> =
            csrfToken().switchMap { editToken ->
                wikiBaseInterface
                    .postEditEntityByFilename(filename!!, editToken, data!!)
                    .map { response: MwPostResponse -> response.successVal == 1 }
            }

        fun getClaimIdsByProperty(
            fileEntityId: String,
            property: String,
        ): Observable<List<String>> =
            getClaimsByProperty(fileEntityId, property).map { claims -> claims.mapNotNull { it.id } }

        /**
         * Ids of the items the property points to, for example the depicted items for P180
         */
        fun getClaimItemIdsByProperty(
            fileEntityId: String,
            property: String,
        ): Observable<List<String>> =
            getClaimsByProperty(fileEntityId, property).map { claims ->
                claims.mapNotNull { (it.mainSnak.dataValue as? DataValue.EntityId)?.value?.id }
            }

        /**
         * Claims of the file for the property, empty when the file has no structured data yet
         */
        private fun getClaimsByProperty(
            fileEntityId: String,
            property: String,
        ): Observable<List<StatementPartial>> =
            wikiBaseInterface
                .getClaimsByProperty(fileEntityId, property)
                .map { claimsResponse -> claimsResponse.claims[property] ?: emptyList() }
                .onErrorResumeNext { throwable: Throwable ->
                    // A file without structured data has no entity yet, so it has no claims
                    if (throwable is MwException && throwable.errorCode == NO_SUCH_ENTITY) {
                        Observable.just(emptyList())
                    } else {
                        Observable.error(throwable)
                    }
                }

        fun postDeleteClaims(
            entityId: String,
            data: String?,
        ): Observable<Boolean> =
            csrfToken().switchMap { editToken ->
                wikiBaseInterface
                    .postDeleteClaims(editToken, entityId, data!!)
                    .map { response: MwPostResponse -> response.successVal == 1 }
            }

        fun getFileEntityId(filename: String): Observable<Long> =
            wikiBaseInterface
                .getFileEntityId("File:$filename")
                .map { response: MwQueryResponse ->
                    response
                        .query()!!
                        .pages()!![0]
                        .pageId()
                        .toLong()
                }

        fun addLabelsToWikidata(
            fileEntityId: Long,
            languageCode: String?,
            captionValue: String?,
        ): Observable<MwPostResponse> =
            csrfToken().switchMap { editToken ->
                wikiBaseInterface.addLabelstoWikidata(
                    PAGE_ID_PREFIX + fileEntityId,
                    editToken,
                    languageCode,
                    captionValue,
                )
            }

        private fun csrfToken(): Observable<String> =
            Observable.fromCallable {
                csrfTokenClient.getTokenBlocking()
            }
    }
