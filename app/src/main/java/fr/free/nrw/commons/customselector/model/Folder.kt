package fr.free.nrw.commons.customselector.model

data class Folder(
    var bucketId: Long,
    var name: String,
    var images: ArrayList<Image> = arrayListOf<Image>()
)