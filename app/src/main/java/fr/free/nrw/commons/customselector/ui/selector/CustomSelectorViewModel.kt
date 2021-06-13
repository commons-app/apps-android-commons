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
import kotlinx.coroutines.cancel

class CustomSelectorViewModel(var context: Context,var imageFileLoader: ImageFileLoader) : ViewModel() {

    private val scope = CoroutineScope(Dispatchers.Main)

    var selectedImages: MutableLiveData<ArrayList<Image>> = MutableLiveData()

    /**
     * Result Live Data
     */
    val result = MutableLiveData(Result(CallbackStatus.IDLE, arrayListOf()))

    /**
     * Fetch Images and supply to result.
     */
    fun fetchImages() {
        result.postValue(Result(CallbackStatus.FETCHING, arrayListOf()))
        scope.cancel()
        imageFileLoader.loadDeviceImages(object: ImageLoaderListener {
            override fun onImageLoaded(images: ArrayList<Image>) {
                result.postValue(Result(CallbackStatus.SUCCESS, images))
            }

            override fun onFailed(throwable: Throwable) {
                result.postValue(Result(CallbackStatus.SUCCESS, arrayListOf()))
            }
        },scope)
    }

    /**
     * Clear the coroutine task linked with context.
     */
    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}