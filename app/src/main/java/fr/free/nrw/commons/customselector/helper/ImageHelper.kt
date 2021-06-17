package fr.free.nrw.commons.customselector.helper

import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Image
import timber.log.Timber
import java.io.*
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.collections.ArrayList
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
     * Gets the list of indices from the master list.
     */
    fun getIndexList(list: ArrayList<Image>, masterList: ArrayList<Image>): ArrayList<Int> {

        /**
         * TODO
         * Can be optimised as masterList is sorted by time.
         */

        val indexes = arrayListOf<Int>()
        for(image in list) {
            val index = getIndex(masterList,image)
            if (index == -1) {
                continue
            }
            indexes.add(index)
        }
        return indexes
    }

    /**
     * Generates the file sha1 from file input stream.
     */
    fun generateSHA1(`is`: InputStream): String {
        val digest: MessageDigest = try {
            MessageDigest.getInstance("SHA1")
        } catch (e: NoSuchAlgorithmException) {
            Timber.e(e, "Exception while getting Digest")
            return ""
        }
        val buffer = ByteArray(8192)
        var read: Int
        return try {
            while (`is`.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
            val md5sum = digest.digest()
            val bigInt = BigInteger(1, md5sum)
            var output = bigInt.toString(16)
            output = String.format("%40s", output).replace(' ', '0')
            Timber.i("File SHA1: %s", output)
            output
        } catch (e: IOException) {
            Timber.e(e, "IO Exception")
            ""
        } finally {
            try {
                `is`.close()
            } catch (e: IOException) {
                Timber.e(e, "Exception on closing input stream")
            }
        }
    }

    /**
     * Gets the file input stream from the file path.
     */
    @Throws(FileNotFoundException::class)
    fun getFileInputStream(filePath: String?): InputStream {
        return FileInputStream(filePath)
    }

}