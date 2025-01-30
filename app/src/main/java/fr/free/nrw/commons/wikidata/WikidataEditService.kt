package fr.free.nrw.commons.wikidata

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.media.PAGE_ID_PREFIX
import fr.free.nrw.commons.upload.UploadResult
import fr.free.nrw.commons.upload.WikidataItem
import fr.free.nrw.commons.upload.WikidataPlace
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import fr.free.nrw.commons.wikidata.WikidataProperties.DEPICTS
import fr.free.nrw.commons.wikidata.WikidataProperties.IMAGE
import fr.free.nrw.commons.wikidata.WikidataProperties.MEDIA_LEGENDS
import fr.free.nrw.commons.wikidata.model.DataValue.MonoLingualText
import fr.free.nrw.commons.wikidata.model.DataValue.ValueString
import fr.free.nrw.commons.wikidata.model.EditClaim
import fr.free.nrw.commons.wikidata.model.RemoveClaim
import fr.free.nrw.commons.wikidata.model.SnakPartial
import fr.free.nrw.commons.wikidata.model.StatementPartial
import fr.free.nrw.commons.wikidata.model.WikiBaseMonolingualTextValue
import fr.free.nrw.commons.wikidata.mwapi.MwPostResponse
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.Arrays
import java.util.Collections
import java.util.Locale
import java.util.Objects
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * This class is meant to handle the Wikidata edits made through the app It will talk with MediaWiki
 * Apis to make the necessary calls, log the edits and fire listeners on successful edits
 */
@Singleton
class WikidataEditService @Inject constructor(
    private val context: Context,
    private val wikidataEditListener: WikidataEditListener?,
    @param:Named("default_preferences") private val directKvStore: JsonKvStore,
    private val wikiBaseClient: WikiBaseClient,
    private val wikidataClient: WikidataClient, private val gson: Gson
) {
    @SuppressLint("CheckResult")
    private fun addDepictsProperty(
        fileEntityId: String,
        depictedItems: List<String>
    ): Observable<Boolean> {

        val data = EditClaim.from(
            if (isBetaFlavour) listOf("Q10") else depictedItems, DEPICTS.propertyName
        )

        return wikiBaseClient.postEditEntity(PAGE_ID_PREFIX + fileEntityId, gson.toJson(data))
            .doOnNext { success: Boolean ->
                if (success) {
                    Timber.d("DEPICTS property was set successfully for %s", fileEntityId)
                } else {
                    Timber.d("Unable to set DEPICTS property for %s", fileEntityId)
                }
            }
            .doOnError { throwable: Throwable ->
                Timber.e(throwable, "Error occurred while setting DEPICTS property")
                showLongToast(context, throwable.toString())
            }
            .subscribeOn(Schedulers.io())
    }

    @SuppressLint("CheckResult")
    fun updateDepictsProperty(
        fileEntityId: String?,
        depictedItems: List<String>
    ): Observable<Boolean> {

        val entityId: String = PAGE_ID_PREFIX + fileEntityId
        val claimIds = getDepictionsClaimIds(entityId)

        /* Please consider removeClaim scenario for BetaDebug */
        val data = RemoveClaim.from(if (isBetaFlavour) listOf("Q10") else claimIds)

        return wikiBaseClient.postDeleteClaims(entityId, gson.toJson(data))
            .doOnError { throwable: Throwable? ->
                Timber.e(
                    throwable,
                    "Error occurred while removing existing claims for DEPICTS property"
                )
                showLongToast(
                    context,
                    context.getString(R.string.wikidata_edit_failure)
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

    @SuppressLint("CheckResult")
    private fun getDepictionsClaimIds(entityId: String): List<String> {
        val claimIds = wikiBaseClient.getClaimIdsByProperty(entityId, DEPICTS.propertyName)
            .subscribeOn(Schedulers.io())
            .blockingFirst()

        return claimIds
    }

    @SuppressLint("StringFormatInvalid")
    private fun showSuccessToast(wikiItemName: String) {
        val successStringTemplate = context.getString(R.string.successful_wikidata_edit)
        val successMessage = String.format(Locale.getDefault(), successStringTemplate, wikiItemName)
        showLongToast(context, successMessage)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("CheckResult")
    private fun addCaption(
        fileEntityId: Long, languageCode: String,
        captionValue: String
    ): Observable<Boolean> {
        return wikiBaseClient.addLabelsToWikidata(fileEntityId, languageCode, captionValue)
            .doOnNext { mwPostResponse: MwPostResponse? ->
                onAddCaptionResponse(
                    fileEntityId,
                    mwPostResponse
                )
            }
            .doOnError { throwable: Throwable? ->
                Timber.e(throwable, "Error occurred while setting Captions")
                showLongToast(
                    context,
                    context.getString(R.string.wikidata_edit_failure)
                )
            }
            .map(Objects::nonNull)
    }

    private fun onAddCaptionResponse(fileEntityId: Long, response: MwPostResponse?) {
        if (response != null) {
            Timber.d("Caption successfully set, revision id = %s", response)
        } else {
            Timber.d("Error occurred while setting Captions, fileEntityId = %s", fileEntityId)
        }
    }

    fun createClaim(
        wikidataPlace: WikidataPlace?, fileName: String,
        captions: Map<String, String>
    ): Long? {
        if (!(directKvStore.getBoolean("Picture_Has_Correct_Location", true))) {
            Timber.d(
                "Image location and nearby place location mismatched, so Wikidata item won't be edited"
            )
            return null
        }
        val result = addImageAndMediaLegends(wikidataPlace!!, fileName, captions)
        return result
    }

    fun addImageAndMediaLegends(
        wikidataItem: WikidataItem, fileName: String,
        captions: Map<String, String>
    ): Long {
        val p18 = SnakPartial(
            "value",
            IMAGE.propertyName,
            ValueString(fileName.replace("File:", ""))
        )

        val snaks: MutableList<SnakPartial> = ArrayList()
        for ((key, value) in captions) {
            snaks.add(
                SnakPartial(
                    "value",
                    MEDIA_LEGENDS.propertyName, MonoLingualText(
                        WikiBaseMonolingualTextValue(value, key)
                    )
                )
            )
        }

        val id = wikidataItem.id + "$" + UUID.randomUUID().toString()
        val claim = StatementPartial(
            p18, "statement", "normal", id, Collections.singletonMap<String, List<SnakPartial>>(
                MEDIA_LEGENDS.propertyName, snaks
            ), Arrays.asList(MEDIA_LEGENDS.propertyName)
        )

        val result = wikidataClient.setClaim(claim, COMMONS_APP_TAG).blockingSingle()
        return result
    }

    fun handleImageClaimResult(wikidataItem: WikidataItem, revisionId: Long?) {
        if (revisionId != null) {
            wikidataEditListener?.onSuccessfulWikidataEdit()
            showSuccessToast(wikidataItem.name)
        } else {
            Timber.d("Unable to make wiki data edit for entity %s", wikidataItem)
            showLongToast(context, context.getString(R.string.wikidata_edit_failure))
        }
    }

    fun addDepictionsAndCaptions(
        uploadResult: UploadResult,
        contribution: Contribution
    ): Observable<Boolean> {
        return wikiBaseClient.getFileEntityId(uploadResult)
            .doOnError { throwable: Throwable? ->
                Timber.e(
                    throwable,
                    "Error occurred while getting EntityID to set DEPICTS property"
                )
                showLongToast(
                    context,
                    context.getString(R.string.wikidata_edit_failure)
                )
            }
            .switchMap { fileEntityId: Long? ->
                if (fileEntityId != null) {
                    Timber.d("EntityId for image was received successfully: %s", fileEntityId)
                    return@switchMap Observable.concat<Boolean>(
                        depictionEdits(contribution, fileEntityId),
                        captionEdits(contribution, fileEntityId)
                    )
                } else {
                    Timber.d("Error acquiring EntityId for image: %s", uploadResult)
                    return@switchMap Observable.empty<Boolean>()
                }
            }
    }

    @SuppressLint("NewApi")
    private fun captionEdits(contribution: Contribution, fileEntityId: Long): Observable<Boolean> {
        val result = Observable.fromIterable(contribution.media.captions.entries)
            .concatMap { addCaption(fileEntityId, it.key, it.value) }
        return result
    }

    private fun depictionEdits(
        contribution: Contribution,
        fileEntityId: Long
    ): Observable<Boolean> {
        val result = addDepictsProperty(fileEntityId.toString(), buildList {
            for ((_, _, _, _, _, _, id) in contribution.depictedItems) {
                add(id)
            }
        })
        return result
    }

    companion object {
        const val COMMONS_APP_TAG: String = "wikimedia-commons-app"
    }
}