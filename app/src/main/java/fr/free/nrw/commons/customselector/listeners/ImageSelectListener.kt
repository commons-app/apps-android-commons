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
     * @param selectedNotForUploadImages : number of selected not for upload images
     */
    fun onSelectedImagesChanged(selectedImages: ArrayList<Image>, selectedNotForUploadImages: Int)

    /**
     * onLongPress
     * @param imageUri : uri of image
     */
    fun onLongPress(position: Int, images: ArrayList<Image>)
}