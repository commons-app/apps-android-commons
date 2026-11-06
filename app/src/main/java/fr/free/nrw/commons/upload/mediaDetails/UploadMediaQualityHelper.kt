package fr.free.nrw.commons.upload.mediaDetails

import android.app.Activity
import fr.free.nrw.commons.R
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.UploadActivity
import fr.free.nrw.commons.upload.UploadItem
import fr.free.nrw.commons.utils.ImageUtils.EMPTY_CAPTION
import fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_EXISTS
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_KEEP
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.net.UnknownHostException

/**
 * Helper class responsible for calculating, caching, and validating image qualities
 * and dispatching quality-related feedback or error dialogs to the view.
 */
class UploadMediaQualityHelper(
    private val repository: UploadRepository,
    private val mainThreadScheduler: Scheduler,
    private val compositeDisposable: CompositeDisposable,
    private val viewProvider: () -> UploadMediaDetailsContract.View,
) {
    private var basicKvStoreFactory: ((String) -> BasicKvStore)? = null

    fun setupBasicKvStoreFactory(factory: (String) -> BasicKvStore) {
        basicKvStoreFactory = factory
    }

    /**
     * Calculates the image quality
     *
     * @param uploadItemIndex      Index of the UploadItem whose quality is to be checked
     * @param inAppPictureLocation In app picture location (if any)
     * @param activity             Context reference
     * @return true if no internal error occurs, else returns false
     */
    fun getImageQuality(
        uploadItemIndex: Int,
        inAppPictureLocation: LatLng?,
        activity: Activity,
    ): Boolean {
        val view = viewProvider()
        val uploadItems = repository.getUploads()
        view.showProgress(true)
        if (uploadItems.isEmpty()) {
            view.showProgress(false)
            // No internationalization required for this error message because it's an internal error.
            view.showMessage(
                "Internal error: Zero upload items received by the Upload Media Detail Fragment. " +
                    "Sorry, please upload again.",
                R.color.color_error,
            )
            return false
        }
        val uploadItem = uploadItems[uploadItemIndex]
        compositeDisposable.add(
            repository
                .getImageQuality(uploadItem, inAppPictureLocation)
                .observeOn(mainThreadScheduler)
                .subscribe({ imageResult: Int ->
                    storeImageQuality(imageResult, uploadItemIndex, activity, uploadItem)
                }, { throwable: Throwable ->
                    if (throwable is UnknownHostException) {
                        view.showProgress(false)
                        view.showConnectionErrorPopup()
                    } else {
                        view.showMessage(throwable.localizedMessage, R.color.color_error)
                    }
                    Timber.e(throwable, "Error occurred while handling image")
                }),
        )
        return true
    }

    /**
     * Stores the image quality in JSON format in SharedPrefs
     *
     * @param imageResult     Image quality
     * @param uploadItemIndex Index of the UploadItem whose quality is calculated
     * @param activity        Context reference
     * @param uploadItem      UploadItem whose quality is to be checked
     */
    private fun storeImageQuality(
        imageResult: Int,
        uploadItemIndex: Int,
        activity: Activity,
        uploadItem: UploadItem,
    ) {
        try {
            val store =
                basicKvStoreFactory
                    ?.let { it(UploadActivity.STORE_NAME_FOR_CURRENT_UPLOAD_IMAGE_SIZE) }
                    ?: BasicKvStore(activity, UploadActivity.STORE_NAME_FOR_CURRENT_UPLOAD_IMAGE_SIZE)
            val value = store.getString(UPLOAD_QUALITIES_KEY, null)
            val jsonObject =
                value.asJsonObject().apply {
                    put("UploadItem$uploadItemIndex", imageResult)
                }
            store.putString(UPLOAD_QUALITIES_KEY, jsonObject.toString())
        } catch (e: JSONException) {
            Timber.e(e)
        }

        if (uploadItemIndex == 0) {
            if (!UploadMediaPresenter.isBatteryDialogShowing && !UploadMediaPresenter.isCategoriesDialogShowing) {
                // if battery-optimisation dialog is not being shown, call checkImageQuality
                checkImageQuality(uploadItem, uploadItemIndex)
            } else {
                viewProvider().showProgress(false)
            }
        }
    }

    /**
     * Used to check image quality from stored qualities and display dialogs
     *
     * @param uploadItem UploadItem whose quality is to be checked
     * @param index      Index of the UploadItem whose quality is to be checked
     */
    fun checkImageQuality(
        uploadItem: UploadItem,
        index: Int,
    ) {
        if ((uploadItem.imageQuality != IMAGE_OK) && (uploadItem.imageQuality != IMAGE_KEEP)) {
            val value =
                basicKvStoreFactory
                    ?.let { it(UploadActivity.STORE_NAME_FOR_CURRENT_UPLOAD_IMAGE_SIZE) }
                    ?.getString(UPLOAD_QUALITIES_KEY, null)

            try {
                val imageQuality = value.asJsonObject()["UploadItem$index"] as Int
                val view = viewProvider()
                view.showProgress(false)
                if (imageQuality == IMAGE_OK) {
                    uploadItem.hasInvalidLocation = false
                    uploadItem.imageQuality = imageQuality
                } else {
                    handleBadImage(imageQuality, uploadItem, index)
                }
            } catch (e: JSONException) {
                Timber.e(e)
            }
        }
    }

    /**
     * Updates the image qualities stored in JSON, whenever an image is deleted
     *
     * @param size Size of uploadableFiles
     * @param index Index of the UploadItem which was deleted
     */
    fun updateImageQualitiesJSON(
        size: Int,
        index: Int,
    ) {
        val value =
            basicKvStoreFactory
                ?.let { it(UploadActivity.STORE_NAME_FOR_CURRENT_UPLOAD_IMAGE_SIZE) }
                ?.getString(UPLOAD_QUALITIES_KEY, null)

        try {
            val jsonObject =
                value.asJsonObject().apply {
                    for (i in index until (size - 1)) {
                        put("UploadItem$i", this["UploadItem" + (i + 1)])
                    }
                    remove("UploadItem" + (size - 1))
                }

            basicKvStoreFactory
                ?.let { it(UploadActivity.STORE_NAME_FOR_CURRENT_UPLOAD_IMAGE_SIZE) }
                ?.putString(UPLOAD_QUALITIES_KEY, jsonObject.toString())
        } catch (e: JSONException) {
            Timber.e(e)
        }
    }

    /**
     * Handles bad pictures, like too dark, already on wikimedia, downloaded from internet
     *
     * @param errorCode Error code of the bad image quality
     * @param uploadItem UploadItem whose quality is bad
     * @param index Index of item whose quality is bad
     */
    private fun handleBadImage(
        errorCode: Int,
        uploadItem: UploadItem,
        index: Int,
    ) {
        Timber.d("Handle bad picture with error code %d", errorCode)
        if (errorCode >= LOCATION_MISMATCH_ERROR_CODE) { // If location of image and nearby does not match
            uploadItem.hasInvalidLocation = true
        }

        // If image has some other problems, show popup accordingly
        if (errorCode != EMPTY_CAPTION && errorCode != FILE_NAME_EXISTS) {
            viewProvider().showBadImagePopup(errorCode, index, uploadItem)
        }
    }

    companion object {
        internal const val UPLOAD_QUALITIES_KEY = "UploadedImagesQualities"
        private const val LOCATION_MISMATCH_ERROR_CODE = 8
    }
}

private fun String?.asJsonObject() =
    if (this != null) {
        JSONObject(this)
    } else {
        JSONObject()
    }
