package fr.free.nrw.commons.customselector.ui.selector

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import fr.free.nrw.commons.customselector.database.UploadedStatus
import fr.free.nrw.commons.customselector.database.UploadedStatusDao
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.adapter.ImageAdapter.ImageViewHolder
import fr.free.nrw.commons.filepicker.PickedFiles
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.upload.FileProcessor
import fr.free.nrw.commons.upload.FileUtilsWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap

/**
 * Image Loader class, loads images, depending on API results.
 */
class ImageLoader @Inject constructor(

    /**
     * MediaClient for SHA1 query.
     */
    var mediaClient: MediaClient,

    /**
     * FileProcessor to pre-process the file.
     */
    var fileProcessor: FileProcessor,

    /**
     * File Utils Wrapper for SHA1
     */
    var fileUtilsWrapper: FileUtilsWrapper,

    /**
     * UploadedStatusDao for cache query.
     */
    var uploadedStatusDao: UploadedStatusDao,

    /**
     * Context for coroutine.
     */
    val context: Context) {

    /**
     * Maps to facilitate image query.
     */
    private var mapImageSHA1: HashMap<Image,String> = HashMap()
    private var mapHolderImage : HashMap<ImageViewHolder,Image> = HashMap()
    private var mapResult: HashMap<String,Boolean> = HashMap()

    /**
     * Query image and setUp the view.
     */
    fun queryAndSetView(holder: ImageViewHolder, image: Image){

        /**
         * Recycler view uses same view holder, so we can identify the latest query image from holder.
         */
        mapHolderImage[holder] = image
        holder.itemNotUploaded()

        CoroutineScope(Dispatchers.Main).launch {

            var result : Boolean? = null
            withContext(Dispatchers.Default) {

                if(mapHolderImage[holder] != image) {
                    // View holder has a new query image, terminate this query.
                    return@withContext
                }

                val imageSHA1 = fileUtilsWrapper.getSHA1(context.contentResolver.openInputStream(image.uri))
                var uploadedStatus = uploadedStatusDao.getUploadedFromImageSHA1(imageSHA1)
                var sha1 = ""

                if(uploadedStatus != null) {
                    // returned from database.

                    sha1 = uploadedStatus.modifiedImageSHA1
                    if(mapHolderImage[holder] != image) {
                        // View holder has a new query image, terminate this query.
                        return@withContext
                    }
                    else if(uploadedStatus.imageResult || uploadedStatus.modifiedImageResult) {
                        // already uploaded image.
                        result = true
                    }
                    else if(uploadedStatus.lastUpdated!!.date >=
                                        Calendar.getInstance().time.date - invalidateDayCount) {
                        // database entry valid.
                        result = false
                    }
                }
                else {
                    sha1 = getSHA1(image)
                }
                if(mapHolderImage[holder] != image) {
                    // View holder has a new query image, terminate this query.
                    return@withContext
                }
                if(result == null) {
                    // not found in database or database result invalid.
                    result = querySHA1(sha1)
                    uploadedStatus = UploadedStatus(imageSHA1, sha1, false, result!!)
                    uploadedStatusDao.insertUploaded(uploadedStatus)
                }
            }
            if(mapHolderImage[holder] == image) {
                // View holder and latest query image match, setup the view.
                if (result!!) {
                    holder.itemUploaded()
                } else {
                    holder.itemNotUploaded()
                }
            }
        }
    }

    /**
     * Query SHA1, return result if previously queried, otherwise start a new query.
     *
     * @return Query result.
     */
    private fun querySHA1(SHA1: String): Boolean {
        if(mapResult[SHA1] != null) {
            return mapResult[SHA1]!!
        }
        val isUploaded = mediaClient.checkFileExistsUsingSha(SHA1).blockingGet()
        mapResult[SHA1] = isUploaded
        return isUploaded
    }

    /**
     * Get SHA1, return SHA1 if available, otherwise generate and store the SHA1.
     *
     * @return sha1 of the image
     */
    private fun getSHA1(image: Image): String{
        if(mapImageSHA1[image] != null) {
            return mapImageSHA1[image]!!
        }
        val sha1 = generateModifiedSHA1(image);
        mapImageSHA1[image] = sha1;
        return sha1;
    }

    /**
     * Generate Modified SHA1 using present Exif settings.
     *
     * @return modified sha1
     */
    private fun generateModifiedSHA1(image: Image) : String {
        val uploadableFile = PickedFiles.pickedExistingPicture(context, image.uri)
        val exifInterface: ExifInterface? = try {
            ExifInterface(uploadableFile.file!!)
        } catch (e: IOException) {
            Timber.e(e)
            null
        }
        fileProcessor.redactExifTags(exifInterface, fileProcessor.getExifTagsToRedact())
        val sha1 = fileUtilsWrapper.getSHA1(fileUtilsWrapper.getFileInputStream(uploadableFile.filePath))
        uploadableFile.file.delete()
        return sha1
    }

    companion object {
        /**
         * Invalidate day count.
         */
        const val invalidateDayCount: Int = 7
    }

}