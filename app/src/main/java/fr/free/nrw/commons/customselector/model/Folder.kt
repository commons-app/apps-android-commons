package fr.free.nrw.commons.customselector.model

data class Folder(
    /**
    bucketId : Unique directory id.
     */
    var bucketId: Long,

    /**
    name : bucket/folder name.
     */
    var name: String,

    /**
    images : folder images.
     */
    var images: ArrayList<Image> = arrayListOf<Image>()
)