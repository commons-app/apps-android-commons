package fr.free.nrw.commons.customselector.listeners

import fr.free.nrw.commons.customselector.model.Image

interface ImageSelectListener {
    fun onSelectedImagesChanged(selectedImages: ArrayList<Image>)
}