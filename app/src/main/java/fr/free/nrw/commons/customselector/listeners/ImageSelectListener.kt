package fr.free.nrw.commons.customselector.listeners

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
}