package fr.free.nrw.commons.customselector.listeners

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
    fun onSelectedImagesChanged(
        selectedImages: ArrayList<Image>,
        selectedNotForUploadImages: Int,
    )

    /**
     * Called when the user performs a long press on an image.
     *
     * @param position The index of the pressed image in the list.
     * @param images The list of all available images.
     * @param selectedImages The currently selected images.
     */
    fun onLongPress(
        position: Int,
        images: ArrayList<Image>,
        selectedImages: ArrayList<Image>,
    )
}
