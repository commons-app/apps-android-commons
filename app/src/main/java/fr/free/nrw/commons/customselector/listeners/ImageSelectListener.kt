package fr.free.nrw.commons.customselector.listeners

import android.net.Uri
import fr.free.nrw.commons.customselector.model.Image

/**
 * Custom selector Image select listener
 */
interface ImageSelectListener {

    /**
     * onSelectedImagesChanged
     * @param selectedImages : new selected images.
     */
    fun onSelectedImagesChanged(selectedImages: ArrayList<Image>)

    /**
     * onLongPress
     * @param imageUri : uri of image
     */
    fun onLongPress(imageUri: Uri)
}