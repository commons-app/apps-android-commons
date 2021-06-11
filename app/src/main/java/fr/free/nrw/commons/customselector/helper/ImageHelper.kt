package fr.free.nrw.commons.customselector.helper

import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Image

object ImageHelper {

    fun folderListFromImages(images: List<Image>): List<Folder> {
        val folderMap: MutableMap<Long, Folder> = LinkedHashMap()
        for (image in images) {
            val bucketId = image.bucketId
            val bucketName = image.bucketName
            var folder = folderMap[bucketId]
            if (folder == null) {
                folder = Folder(bucketId, bucketName)
                folderMap[bucketId] = folder
            }
            folder.images.add(image)
        }
        return ArrayList(folderMap.values)
    }

    fun filterImages(images: ArrayList<Image>, bukketId: Long?): ArrayList<Image> {
        if (bukketId == null) return images

        val filteredImages = arrayListOf<Image>()
        for (image in images) {
            if (image.bucketId == bukketId) {
                filteredImages.add(image)
            }
        }
        return filteredImages
    }

}