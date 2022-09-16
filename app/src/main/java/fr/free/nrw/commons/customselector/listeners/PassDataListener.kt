package fr.free.nrw.commons.customselector.listeners

import fr.free.nrw.commons.customselector.model.Image

/**
 * Interface to pass data between fragment and activity
 */
interface PassDataListener {
    fun passSelectedImages(selectedImages: ArrayList<Image>, shouldRefresh: Boolean)
}