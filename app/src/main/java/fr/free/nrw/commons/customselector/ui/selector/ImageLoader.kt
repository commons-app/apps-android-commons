package fr.free.nrw.commons.customselector.ui.selector

import android.content.Context
import android.net.Uri
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
import java.net.UnknownHostException
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
    val context: Context
) {

    /**
     * Maps to facilitate image query.
     */
    private var mapImageSHA1: HashMap<Image, String> = HashMap()
    private var mapHolderImage : HashMap<ImageViewHolder, Image> = HashMap()
    private var mapResult: HashMap<String, Int> = HashMap()

    /**
     * Query image and setUp the view.
     */
    fun queryAndSetView(holder: ImageViewHolder, image: Image) {

        /**
         * Recycler view uses same view holder, so we can identify the latest query image from holder.
         */
        mapHolderImage[holder] = image
        holder.itemNotUploaded()

        CoroutineScope(Dispatchers.Main).launch {

            var result : Int = NOT_FOUND
            withContext(Dispatchers.Default) {

                if (mapHolderImage[holder] == image) {
                    val imageSHA1 = getImageSHA1(image.uri)
                    val uploadedStatus = uploadedStatusDao.getUploadedFromImageSHA1(imageSHA1)

                    val sha1 = uploadedStatus?.let {
                        result = getResultFromUploadedStatus(uploadedStatus)
                        uploadedStatus.modifiedImageSHA1
                        } ?: run {
                            if(mapHolderImage[holder] == image) {
                                getSHA1(image)
                            } else {
                                ""
                            }
                        }

                    if (mapHolderImage[holder] == image &&
                        result in arrayOf(NOT_FOUND, RESULT_INVALID) &&
                        sha1.isNotEmpty()) {
                            result = querySHA1(sha1)
                            insertIntoUploaded(imageSHA1, sha1, false, result == RESULT_TRUE)
                    }
                }
            }
            if(mapHolderImage[holder] == image) {
                if (result == RESULT_TRUE) holder.itemUploaded() else holder.itemNotUploaded()
            }
        }
    }

    /**
     * Query SHA1, return result if previously queried, otherwise start a new query.
     *
     * @return Query result.
     */
    private fun querySHA1(SHA1: String): Int {
        mapResult[SHA1]?.let{
            return it
        }
        var apiResult = RESULT_FALSE
        try {
            if (mediaClient.checkFileExistsUsingSha(SHA1).blockingGet()) {
                apiResult = RESULT_TRUE
                mapResult[SHA1] = RESULT_TRUE
            }
        } catch (e: Exception) {
            if (e is UnknownHostException) {
                // Handle no network connection.
                Timber.e(e, "Network Connection Error")
            }
            e.printStackTrace()
        } finally {
            return apiResult
        }
    }

    /**
     * Get SHA1, return SHA1 if available, otherwise generate and store the SHA1.
     *
     * @return sha1 of the image
     */
    private fun getSHA1(image: Image): String {
        mapImageSHA1[image]?.let{
            return it
        }
        val sha1 = generateModifiedSHA1(image);
        mapImageSHA1[image] = sha1;
        return sha1;
    }

    private fun insertIntoUploaded(imageSha1:String, modifiedImageSha1:String, imageResult:Boolean, modifiedImageResult: Boolean){
        uploadedStatusDao.insertUploaded(UploadedStatus(imageSha1, modifiedImageSha1, imageResult, modifiedImageResult))
    }

    private fun getImageSHA1(uri: Uri): String {
        return fileUtilsWrapper.getSHA1(context.contentResolver.openInputStream(uri))
    }

    private fun getResultFromUploadedStatus(uploadedStatus: UploadedStatus): Int {
        if (uploadedStatus.imageResult || uploadedStatus.modifiedImageResult) {
            return RESULT_TRUE
        } else {
            uploadedStatus.lastUpdated?.let {
                if (it.date >= Calendar.getInstance().time.date - INVALIDATE_DAY_COUNT) {
                    return RESULT_FALSE
                }
            }
        }
        return RESULT_INVALID
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
        const val INVALIDATE_DAY_COUNT: Int = 7
        const val RESULT_TRUE: Int = 1
        const val RESULT_FALSE: Int = 0
        const val RESULT_INVALID: Int = -1
        const val NOT_FOUND: Int = -2
    }

}