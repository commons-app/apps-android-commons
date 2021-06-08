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