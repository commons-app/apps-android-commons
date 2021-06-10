package fr.free.nrw.commons.customselector.listeners

import fr.free.nrw.commons.customselector.model.Image

interface ImageLoaderListener {
    fun onImageLoaded(images: ArrayList<Image>)
    fun onFailed(throwable: Throwable)
}