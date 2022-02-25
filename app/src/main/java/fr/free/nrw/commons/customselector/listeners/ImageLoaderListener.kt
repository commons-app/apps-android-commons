package fr.free.nrw.commons.customselector.listeners

import fr.free.nrw.commons.data.models.customselector.Image

/**
 * Custom Selector Image Loader Listener
 * responds to the device image query.
 */
interface ImageLoaderListener {

    /**
     * On image loaded
     * @param images : queried device images.
     */
    fun onImageLoaded(images: ArrayList<Image>)

    /**
     * On failed
     * @param throwable : throwable exception on failure.
     */
    fun onFailed(throwable: Throwable)
}