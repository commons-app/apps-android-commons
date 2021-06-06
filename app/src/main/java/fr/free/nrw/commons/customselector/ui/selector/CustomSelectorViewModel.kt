package fr.free.nrw.commons.customselector.ui.selector

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import fr.free.nrw.commons.customselector.listeners.ImageLoaderListener
import fr.free.nrw.commons.customselector.model.CallbackStatus
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.model.Result

class CustomSelectorViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val imageFileLoader : ImageFileLoader = ImageFileLoader(context)

    var selectedImages: MutableLiveData<ArrayList<Image>> = MutableLiveData()
    val result = MutableLiveData(Result(CallbackStatus.IDLE, arrayListOf()))

    fun fetchImages(){
        result.postValue(Result(CallbackStatus.FETCHING, arrayListOf()))
        imageFileLoader.abortLoadImage()
        imageFileLoader.loadDeviceImages(object: ImageLoaderListener {
            override fun onImageLoaded(images: ArrayList<Image>) {
                result.postValue(Result(CallbackStatus.SUCCESS,images))
            }

            override fun onFailed(throwable: Throwable) {
                result.postValue(Result(CallbackStatus.SUCCESS, arrayListOf()))
            }
        })
    }

}