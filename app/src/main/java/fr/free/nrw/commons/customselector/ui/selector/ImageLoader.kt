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
import java.util.concurrent.TimeUnit
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
    private var mapResult: HashMap<String, Result> = HashMap()

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

            var result : Result = Result.NOTFOUND
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
                        result in arrayOf(Result.NOTFOUND, Result.INVALID) &&
                        sha1.isNotEmpty()) {
                            // Query original image.
                            result = querySHA1(imageSHA1)
                            if( result is Result.TRUE ) {
                                // Original image found.
                                insertIntoUploaded(imageSHA1, sha1, result is Result.TRUE, false)
                            }
                            else {
                                // Original image not found, query modified image.
                                result = querySHA1(sha1)
                                if (result != Result.ERROR) {
                                    insertIntoUploaded(imageSHA1, sha1, false, result is Result.TRUE)
                                }
                            }
                    }
                }
            }
            if(mapHolderImage[holder] == image) {
                if (result is Result.TRUE) holder.itemUploaded() else holder.itemNotUploaded()
            }
        }
    }

    /**
     * Query SHA1, return result if previously queried, otherwise start a new query.
     *
     * @return Query result.
     */
    private fun querySHA1(SHA1: String): Result {
        mapResult[SHA1]?.let{
            return it
        }
        var result : Result = Result.FALSE
        try {
            if (mediaClient.checkFileExistsUsingSha(SHA1).blockingGet()) {
                mapResult[SHA1] = Result.TRUE
                result = Result.TRUE
            }
        } catch (e: Exception) {
            if (e is UnknownHostException) {
                // Handle no network connection.
                Timber.e(e, "Network Connection Error")
            }
            result = Result.ERROR
            e.printStackTrace()
        } finally {
               return result
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

    /**
     * Insert into uploaded status table.
     */
    private fun insertIntoUploaded(imageSha1:String, modifiedImageSha1:String, imageResult:Boolean, modifiedImageResult: Boolean){
        uploadedStatusDao.insertUploaded(UploadedStatus(imageSha1, modifiedImageSha1, imageResult, modifiedImageResult))
    }

    /**
     * Get image sha1 from uri, used to retrieve the original image sha1.
     */
    private fun getImageSHA1(uri: Uri): String {
        return fileUtilsWrapper.getSHA1(context.contentResolver.openInputStream(uri))
    }

    /**
     * Get result data from database.
     */
    private fun getResultFromUploadedStatus(uploadedStatus: UploadedStatus): Result {
        if (uploadedStatus.imageResult || uploadedStatus.modifiedImageResult) {
            return Result.TRUE
        } else {
            uploadedStatus.lastUpdated?.let {
                val duration = Calendar.getInstance().time.time - it.time
                if (TimeUnit.MILLISECONDS.toDays(duration) < INVALIDATE_DAY_COUNT) {
                    return Result.FALSE
                }
            }
        }
        return Result.INVALID
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

    /**
     * Sealed Result class.
     */
    sealed class Result {
        object TRUE : Result()
        object FALSE : Result()
        object INVALID : Result()
        object NOTFOUND : Result()
        object ERROR : Result()
    }

    companion object {
        const val INVALIDATE_DAY_COUNT: Long = 7
    }

}