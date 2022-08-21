package fr.free.nrw.commons.customselector.listeners

import fr.free.nrw.commons.customselector.model.Image

interface PassDataListener {
    fun passSelectedImages(selectedImages: ArrayList<Image>, shouldRefresh: Boolean)
}