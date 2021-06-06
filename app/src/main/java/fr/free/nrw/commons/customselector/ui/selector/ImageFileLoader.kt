package fr.free.nrw.commons.customselector.ui.selector

import android.content.Context
import android.net.Uri
import fr.free.nrw.commons.customselector.listeners.ImageLoaderListener
import fr.free.nrw.commons.customselector.model.Image

class ImageFileLoader(private val context: Context) {

    fun loadDeviceImages(listener: ImageLoaderListener){
        var tempImage:Image=Image(0,"temp", Uri.parse("http://www.google.com"),"path",0,"bucket","1223")
        var array: ArrayList<Image> = ArrayList()
        for(i in 1..100)
            array.add(tempImage)
        listener.onImageLoaded(array)
    }

    fun abortLoadImage(){

    }
}