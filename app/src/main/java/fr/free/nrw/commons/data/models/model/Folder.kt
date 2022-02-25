package fr.free.nrw.commons.data.models.model

/**
 * Custom selector data class Folder.
 */
data class Folder(
    /**
    bucketId : Unique directory id, eg 540528482
     */
    var bucketId: Long,

    /**
    name : bucket/folder name, eg Camera
     */
    var name: String,

    /**
    images : folder images, list of all images under this folder.
     */
    var images: ArrayList<Image> = arrayListOf<Image>()


) {
    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {

        if (javaClass != other?.javaClass) {
            return false
        }

        other as Folder

        if (bucketId != other.bucketId) {
            return false
        }
        if (name != other.name) {
            return false
        }
        if (images != other.images) {
            return false
        }

        return true
    }
}