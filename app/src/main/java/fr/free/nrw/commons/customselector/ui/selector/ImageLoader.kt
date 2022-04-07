package fr.free.nrw.commons.customselector.ui.selector

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import fr.free.nrw.commons.customselector.models.UploadedStatus
import fr.free.nrw.commons.customselector.database.UploadedStatusDao
import fr.free.nrw.commons.customselector.models.Image
import fr.free.nrw.commons.customselector.ui.adapter.ImageAdapter.ImageViewHolder
import fr.free.nrw.commons.filepicker.PickedFiles
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.upload.FileProcessor
import fr.free.nrw.commons.upload.FileUtilsWrapper
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.FileNotFoundException
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
    private var mapModifiedImageSHA1: HashMap<Image, String> = HashMap()
    private var mapHolderImage : HashMap<ImageViewHolder, Image> = HashMap()
    private var mapResult: HashMap<String, Result> = HashMap()
    private var mapImageSHA1: HashMap<Uri, String> = HashMap()

    /**
     * Coroutine Dispatchers and Scope.
     */
    private var defaultDispatcher : CoroutineDispatcher = Dispatchers.Default
    private var ioDispatcher : CoroutineDispatcher = Dispatchers.IO
    private val scope : CoroutineScope = MainScope()

    /**
     * Query image and setUp the view.
     */
    fun queryAndSetView(holder: ImageViewHolder, image: Image) {

        /**
         * Recycler view uses same view holder, so we can identify the latest query image from holder.
         */
        mapHolderImage[holder] = image
        holder.itemNotUploaded()

        scope.launch {

            var result: Result = Result.NOTFOUND

            if (mapHolderImage[holder] != image) {
                return@launch
            }

            val imageSHA1 = getImageSHA1(image.uri)
            if(imageSHA1.isEmpty())
                return@launch
            val uploadedStatus = getFromUploaded(imageSHA1)

            val sha1 = uploadedStatus?.let {
                result = getResultFromUploadedStatus(uploadedStatus)
                uploadedStatus.modifiedImageSHA1
            } ?: run {
                if (mapHolderImage[holder] == image) {
                    getSHA1(image)
                } else {
                    ""
                }
            }

            if (mapHolderImage[holder] != image) {
                return@launch
            }

            if (result in arrayOf(Result.NOTFOUND, Result.INVALID) && sha1.isNotEmpty()) {
                // Query original image.
                result = querySHA1(imageSHA1)
                if (result is Result.TRUE) {
                    // Original image found.
                    insertIntoUploaded(imageSHA1, sha1, result is Result.TRUE, false)
                } else {
                    // Original image not found, query modified image.
                    result = querySHA1(sha1)
                    if (result != Result.ERROR) {
                        insertIntoUploaded(imageSHA1, sha1, false, result is Result.TRUE)
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

    suspend fun querySHA1(SHA1: String): Result {
        return withContext(ioDispatcher) {
            mapResult[SHA1]?.let {
                return@withContext it
            }
            var result: Result = Result.FALSE
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
            }
            result
        }
    }

    /**
     * Get SHA1, return SHA1 if available, otherwise generate and store the SHA1.
     *
     * @return sha1 of the image
     */
    suspend fun getSHA1(image: Image): String {
        mapModifiedImageSHA1[image]?.let{
            return it
        }
        val sha1 = generateModifiedSHA1(image);
        mapModifiedImageSHA1[image] = sha1;
        return sha1;
    }

    /**
     * Get the uploaded status entry from the database.
     */
    suspend fun getFromUploaded(imageSha1:String): UploadedStatus? {
        return uploadedStatusDao.getUploadedFromImageSHA1(imageSha1)
    }

    /**
     * Insert into uploaded status table.
     */
    suspend fun insertIntoUploaded(imageSha1:String, modifiedImageSha1:String, imageResult:Boolean, modifiedImageResult: Boolean){
        uploadedStatusDao.insertUploaded(
            UploadedStatus(
                imageSha1,
                modifiedImageSha1,
                imageResult,
                modifiedImageResult
            )
        )
    }

    /**
     * Get image sha1 from uri, used to retrieve the original image sha1.
     */
    suspend fun getImageSHA1(uri: Uri): String {
        return withContext(ioDispatcher) {
            mapImageSHA1[uri]?.let{
                return@withContext it
            }
            try {
                val result = fileUtilsWrapper.getSHA1(context.contentResolver.openInputStream(uri))
                mapImageSHA1[uri] = result
                result
            } catch (e: FileNotFoundException){
                e.printStackTrace()
                ""
            }
        }
    }

    /**
     * Get result data from database.
     */
    fun getResultFromUploadedStatus(uploadedStatus: UploadedStatus): Result {
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
    private suspend fun generateModifiedSHA1(image: Image) : String {
        return withContext(defaultDispatcher) {
            val uploadableFile = PickedFiles.pickedExistingPicture(context, image.uri)
            val exifInterface: ExifInterface? = try {
                ExifInterface(uploadableFile.file!!)
            } catch (e: IOException) {
                Timber.e(e)
                null
            }
            fileProcessor.redactExifTags(exifInterface, fileProcessor.getExifTagsToRedact())
            val sha1 =
                fileUtilsWrapper.getSHA1(fileUtilsWrapper.getFileInputStream(uploadableFile.filePath))
            uploadableFile.file.delete()
            sha1
        }
    }

    /**
     * CleanUp function.
     */
    fun cleanUP() {
        scope.cancel()
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

    /**
     * Companion Object
     */
    companion object {
        /**
         * Invalidate Day count.
         * False Database Entries are invalid after INVALIDATE_DAY_COUNT and need to be re-queried.
         */
        const val INVALIDATE_DAY_COUNT: Long = 7
    }

}