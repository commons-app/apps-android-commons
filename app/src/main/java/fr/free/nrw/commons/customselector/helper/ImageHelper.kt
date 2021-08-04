package fr.free.nrw.commons.customselector.helper

import android.content.Context
import com.mapbox.android.core.FileUtils
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.filepicker.Constants
import timber.log.Timber
import java.io.*
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap


/**
 * Image Helper object, includes all the static functions required by custom selector.
 */

object ImageHelper {

    /**
     * Returns the list of folders from given image list.
     */
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

    /**
     * Filters the images based on the given bucketId (folder)
     */
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

    /**
     * getIndex: Returns the index of image in given list.
     */
    fun getIndex(list: ArrayList<Image>, image: Image): Int {
        return list.indexOf(image)
    }

    /**
     * getIndex: Returns the index of image in given list.
     */
    fun getIndexFromId(list: ArrayList<Image>, imageId: Long): Int {
        for(i in list){
            if(i.id == imageId)
                return list.indexOf(i)
        }
        return 0;
    }

    fun cleanImages(list: ArrayList<Image>) {
        val toBeRemoved:ArrayList<Int> = ArrayList()
        for(image in list){
            val imageExist = File(image.path).exists()
            if(!imageExist){
                toBeRemoved.add(list.indexOf(image))
            }
        }
        list.removeAll(toBeRemoved.map { list[it] })
    }

    fun cleanFolders(list :List<Folder>) {
        for(folder in list){
            cleanImages(folder.images)
        }
    }

    /**
     * Gets the list of indices from the master list.
     */
    fun getIndexList(list: ArrayList<Image>, masterList: ArrayList<Image>): ArrayList<Int> {

        /**
         * TODO
         * Can be optimised as masterList is sorted by time.
         */

        val indexes = arrayListOf<Int>()
        for(image in list) {
            val index = getIndex(masterList, image)
            if (index == -1) {
                continue
            }
            indexes.add(index)
        }
        return indexes
    }
}