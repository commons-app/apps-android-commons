package fr.free.nrw.commons.wikidata

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.Gson
import fr.free.nrw.commons.R
import fr.free.nrw.commons.media.PAGE_ID_PREFIX
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import fr.free.nrw.commons.wikidata.WikidataProperties.DEPICTS
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
         * Adds only the items the file does not depict yet, so retrying an upload adds no
         * duplicate claims
         */
        fun addMissingDepicts(
            fileEntityId: String,
            depictedItems: List<String>,
        ): Observable<Boolean> =
            wikiBaseClient
                .getClaimItemIdsByProperty(PAGE_ID_PREFIX + fileEntityId, DEPICTS.propertyName)
                .switchMap { existingItems ->
                    val missingItems =
                        (if (isBetaFlavour) listOf("Q10") else depictedItems) - existingItems.toSet()
                    if (missingItems.isEmpty()) {
                        Observable.just(true)
                    } else {
                        addDepictsProperty(fileEntityId, missingItems)
                    }
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
