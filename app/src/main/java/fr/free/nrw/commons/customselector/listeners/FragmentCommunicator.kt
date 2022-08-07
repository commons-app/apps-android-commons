package fr.free.nrw.commons.customselector.listeners

import fr.free.nrw.commons.customselector.model.Image

interface FragmentCommunicator {
    fun passData(selectedImages: ArrayList<Image>)
}