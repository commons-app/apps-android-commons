package fr.free.nrw.commons.customselector.ui.selector

import android.content.Context
import android.net.Uri
import fr.free.nrw.commons.customselector.listeners.ImageLoaderListener
import fr.free.nrw.commons.customselector.model.Image

class ImageFileLoader(val context: Context) {

    /**
     * Load Device Images.
     */
    fun loadDeviceImages(listener: ImageLoaderListener) {
        var tempImage = Image(0, "temp", Uri.parse("http://www.google.com"), "path", 0, "bucket", "1223")
        var array: ArrayList<Image> = ArrayList()
        for(i in 1..100) {
            array.add(tempImage)
        }
        listener.onImageLoaded(array)

        // todo load images from device using cursor.
    }

    /**
     * Abort loading images.
     */
    fun abortLoadImage(){
        //todo Abort loading images.
    }

    /**
     *
     * TODO
     * Runnable Thread for image loading.
     * Sha1 for image (original image).
     *
     */
}