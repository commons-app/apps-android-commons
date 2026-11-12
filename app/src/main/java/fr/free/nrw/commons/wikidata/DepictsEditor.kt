package fr.free.nrw.commons.wikidata

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.Gson
import fr.free.nrw.commons.R
import fr.free.nrw.commons.media.PAGE_ID_PREFIX
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import fr.free.nrw.commons.wikidata.WikidataProperties.DEPICTS
import fr.free.nrw.commons.wikidata.WikidataProperties.LOCATION_OF_CREATION
import fr.free.nrw.commons.wikidata.model.EditClaim
import fr.free.nrw.commons.wikidata.model.RemoveClaim
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class DepictsEditor
    @Inject
    constructor(
        private val context: Context,
        private val wikiBaseClient: WikiBaseClient,
        private val gson: Gson,
    ) {
        @SuppressLint("CheckResult")
        fun addDepictsProperty(
            fileEntityId: String,
            depictedItems: List<String>,
        ): Observable<Boolean> {
            val data =
                EditClaim.from(
                    if (isBetaFlavour) listOf("Q10") else depictedItems,
                    DEPICTS.propertyName,
                )

            return wikiBaseClient
                .postEditEntity(PAGE_ID_PREFIX + fileEntityId, gson.toJson(data))
                .doOnNext { success: Boolean ->
                    if (success) {
                        Timber.d("DEPICTS property was set successfully for %s", fileEntityId)
                    } else {
                        Timber.d("Unable to set DEPICTS property for %s", fileEntityId)
                    }
                }.doOnError { throwable: Throwable ->
                    Timber.e(throwable, "Error occurred while setting DEPICTS property")
                    showLongToast(context, throwable.toString())
                }.subscribeOn(Schedulers.io())
        }

        /**
         * Adds a "location of creation" (P1071) statement to the file, for a place where the
         * picture was taken but which the picture does not really depict.
         *
         * @param fileEntityId the MediaInfo entity id of the file, without the "M" prefix
         * @param locationId the Wikidata item id (for example Q12439) of the place
         * @return emits true if the statement was saved
         */
        fun addLocationOfCreationProperty(
            fileEntityId: String,
            locationId: String,
        ): Observable<Boolean> {
            // Beta Commons does not know real Wikidata items, so it uses the same placeholder
            // item as the depicts statement
            val data =
                EditClaim.from(
                    listOf(if (isBetaFlavour) "Q10" else locationId),
                    LOCATION_OF_CREATION.propertyName,
                )

            return wikiBaseClient
                .postEditEntity(PAGE_ID_PREFIX + fileEntityId, gson.toJson(data))
                .doOnNext { success: Boolean ->
                    if (success) {
                        Timber.d("LOCATION_OF_CREATION property was set successfully for %s", fileEntityId)
                    } else {
                        Timber.d("Unable to set LOCATION_OF_CREATION property for %s", fileEntityId)
                    }
                }.doOnError { throwable: Throwable ->
                    Timber.e(throwable, "Error occurred while setting LOCATION_OF_CREATION property")
                }.subscribeOn(Schedulers.io())
        }

        @SuppressLint("CheckResult")
        fun updateDepictsProperty(
            fileEntityId: String?,
            depictedItems: List<String>,
        ): Observable<Boolean> {
            val entityId: String = PAGE_ID_PREFIX + fileEntityId

            // The existing claims are read when subscribing, so that an error reaches the caller
            return wikiBaseClient
                .getClaimIdsByProperty(entityId, DEPICTS.propertyName)
                .switchMap { claimIds ->
                    // Please consider removeClaim scenario for BetaDebug
                    val claimIdsToRemove = if (isBetaFlavour) listOf("Q10") else claimIds
                    if (claimIdsToRemove.isEmpty()) {
                        // Nothing to remove, for example a file without structured data
                        Observable.just(true)
                    } else {
                        val data = RemoveClaim.from(claimIdsToRemove)
                        wikiBaseClient.postDeleteClaims(entityId, gson.toJson(data))
                    }
                }.doOnError { throwable: Throwable? ->
                    Timber.e(
                        throwable,
                        "Error occurred while reading or removing existing claims for DEPICTS property",
                    )
                    showLongToast(
                        context,
                        context.getString(R.string.wikidata_edit_failure),
                    )
                }.switchMap { success: Boolean ->
                    if (success) {
                        Timber.d("DEPICTS property was deleted successfully")
                        return@switchMap addDepictsProperty(fileEntityId!!, depictedItems)
                    } else {
                        Timber.d("Unable to delete DEPICTS property")
                        return@switchMap Observable.empty<Boolean>()
                    }
                }
        }
    }
