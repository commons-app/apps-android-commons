package fr.free.nrw.commons.customselector.ui.selector

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import fr.free.nrw.commons.customselector.helper.CustomSelectorConstants
import fr.free.nrw.commons.customselector.listeners.ImageLoaderListener
import fr.free.nrw.commons.customselector.model.CallbackStatus
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.model.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

/**
 * Custom Selector view model.
 */
class CustomSelectorViewModel(
    var context: Context,
    var imageFileLoader: ImageFileLoader,
) : ViewModel() {
    /**
     * Scope for coroutine task (image fetch).
     */
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Stores selected images.
     */
    var selectedImages: MutableLiveData<ArrayList<Image>> = MutableLiveData()

    /**
     * Result Live Data.
     */
    val result = MutableLiveData(Result(CallbackStatus.IDLE, arrayListOf()))

    /**
     * Fetch Images and supply to result.
     */
    fun fetchImages() {
        result.postValue(Result(CallbackStatus.FETCHING, arrayListOf()))
        scope.cancel()
        imageFileLoader.loadDeviceImages(
            object : ImageLoaderListener {
                override fun onImageLoaded(images: ArrayList<Image>) {
                    result.postValue(Result(CallbackStatus.SUCCESS, images))
                }

                override fun onFailed(throwable: Throwable) {
                    result.postValue(Result(CallbackStatus.SUCCESS, arrayListOf()))
                }
            },
        )
    }

    /**
     * When data will be send from full screen mode, it will be passed to fragment
     */
    fun onFullScreenDataReceived(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val newSelectedImages: ArrayList<Image> =
                result.data!!
                    .getParcelableArrayListExtra(CustomSelectorConstants.NEW_SELECTED_IMAGES)!!
            selectedImages.value = newSelectedImages
        }
    }

    /**
     * Clear the coroutine task linked with context.
     */
    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}
