package fr.free.nrw.commons.customselector.ui.selector

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import fr.free.nrw.commons.customselector.listeners.ImageLoaderListener
import fr.free.nrw.commons.customselector.model.CallbackStatus
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.model.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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
        // fix: instead of scope.cancel(), we call our new abort method in the loader.
        //this stops the background processing while keeping the ViewModel scope alive.
        imageFileLoader.abortLoadImage()
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
     * Clear the coroutine task linked with context.
     */
    override fun onCleared() {
        //stop the loader immediately when the ViewModel is destroyed
        imageFileLoader.abortLoadImage()
        scope.cancel()
        super.onCleared()
    }
}
