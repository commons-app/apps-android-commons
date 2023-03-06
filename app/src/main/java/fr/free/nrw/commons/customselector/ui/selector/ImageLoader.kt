package fr.free.nrw.commons.customselector.ui.selector

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import fr.free.nrw.commons.customselector.database.NotForUploadStatusDao
import fr.free.nrw.commons.customselector.database.UploadedStatus
import fr.free.nrw.commons.customselector.database.UploadedStatusDao
import fr.free.nrw.commons.customselector.helper.ImageHelper
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.adapter.ImageAdapter.ImageViewHolder
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.upload.FileProcessor
import fr.free.nrw.commons.upload.FileUtilsWrapper
import fr.free.nrw.commons.utils.CustomSelectorUtils
import fr.free.nrw.commons.utils.CustomSelectorUtils.Companion.checkWhetherFileExistsOnCommonsUsingSHA1
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

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
     * NotForUploadDao for database operations
     */
    var notForUploadStatusDao: NotForUploadStatusDao,

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
     * Coroutine Scope.
     */
    private val scope : CoroutineScope = MainScope()

    /**
     * Query image and setUp the view.
     */
    fun queryAndSetView(
        holder: ImageViewHolder,
        image: Image,
        ioDispatcher: CoroutineDispatcher,
        defaultDispatcher: CoroutineDispatcher
    ) {

        /**
         * Recycler view uses same view holder, so we can identify the latest query image from holder.
         */
        mapHolderImage[holder] = image
        holder.itemNotUploaded()
        holder.itemForUpload()

        scope.launch {
            var result: Result = Result.NOTFOUND

            if (mapHolderImage[holder] != image) {
                return@launch
            }

            val imageSHA1: String = when (mapImageSHA1[image.uri] != null) {
                true -> mapImageSHA1[image.uri]!!
                else -> CustomSelectorUtils.getImageSHA1(
                    image.uri,
                    ioDispatcher,
                    fileUtilsWrapper,
                    context.contentResolver
                )
            }
            mapImageSHA1[image.uri] = imageSHA1

            if (imageSHA1.isEmpty()) {
                return@launch
            }
            val uploadedStatus = getFromUploaded(imageSHA1)

            val sha1 = uploadedStatus?.let {
                result = getResultFromUploadedStatus(uploadedStatus)
                uploadedStatus.modifiedImageSHA1
            } ?: run {
                if (mapHolderImage[holder] == image) {
                    getSHA1(image, defaultDispatcher)
                } else {
                    ""
                }
            }

            if (mapHolderImage[holder] != image) {
                return@launch
            }

            val existsInNotForUploadTable = notForUploadStatusDao.find(imageSHA1)

            if (result in arrayOf(Result.NOTFOUND, Result.INVALID) && sha1.isNotEmpty()) {
                when {
                    mapResult[imageSHA1] == null -> {
                        // Query original image.
                        result = checkWhetherFileExistsOnCommonsUsingSHA1(
                            imageSHA1,
                            ioDispatcher,
                            mediaClient
                        )
                        when (result) {
                            is Result.TRUE -> {
                                mapResult[imageSHA1] = Result.TRUE
                            }
                            is Result.ERROR -> {
                                mapResult[imageSHA1] = Result.ERROR
                            }
                            is Result.FALSE -> {
                                mapResult[imageSHA1] = Result.FALSE
                            }
                            is Result.INVALID -> {
                                mapResult[imageSHA1] = Result.INVALID
                            }
                            is Result.NOTFOUND -> {
                                mapResult[imageSHA1] = Result.NOTFOUND
                            }
                        }
                    }
                    else -> {
                        result = mapResult[imageSHA1]!!
                    }
                }
                if (result is Result.TRUE) {
                    // Original image found.
                    insertIntoUploaded(imageSHA1, sha1, result is Result.TRUE, false)
                } else {
                    when {
                        mapResult[sha1] == null -> {
                            // Original image not found, query modified image.
                            result = checkWhetherFileExistsOnCommonsUsingSHA1(
                                sha1,
                                ioDispatcher,
                                mediaClient
                            )
                            when (result) {
                                is Result.TRUE -> {
                                    mapResult[sha1] = Result.TRUE
                                }
                                is Result.ERROR -> {
                                    mapResult[sha1] = Result.ERROR
                                }
                                is Result.FALSE -> {
                                    mapResult[sha1] = Result.FALSE
                                }
                                is Result.INVALID -> {
                                    mapResult[sha1] = Result.INVALID
                                }
                                is Result.NOTFOUND -> {
                                    mapResult[sha1] = Result.NOTFOUND
                                }
                            }
                        }
                        else -> {
                            result = mapResult[sha1]!!
                        }
                    }
                    if (result != Result.ERROR) {
                        insertIntoUploaded(imageSHA1, sha1, false, result is Result.TRUE)
                    }
                }
            }

            val sharedPreferences: SharedPreferences =
                context
                    .getSharedPreferences(ImageHelper.CUSTOM_SELECTOR_PREFERENCE_KEY, 0)
            val showAlreadyActionedImages =
                sharedPreferences.getBoolean(
                    ImageHelper.SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY,
                    true
                )

            if (mapHolderImage[holder] == image) {
                if ((result is Result.TRUE) && showAlreadyActionedImages) {
                    holder.itemUploaded()
                } else holder.itemNotUploaded()

                if ((existsInNotForUploadTable > 0) && showAlreadyActionedImages) {
                    holder.itemNotForUpload()
                } else holder.itemForUpload()
            }
        }
    }

    /**
     * Finds out the next actionable image position
     */
    suspend fun nextActionableImage(
        allImages: List<Image>, ioDispatcher: CoroutineDispatcher,
        defaultDispatcher: CoroutineDispatcher,
        nextImagePosition: Int
    ): Int {
        var next: Int

        // Traversing from given position to the end
        for (i in nextImagePosition until allImages.size){
            val it = allImages[i]
            val imageSHA1: String = when (mapImageSHA1[it.uri] != null) {
                true -> mapImageSHA1[it.uri]!!
                else -> CustomSelectorUtils.getImageSHA1(
                    it.uri,
                    ioDispatcher,
                    fileUtilsWrapper,
                    context.contentResolver
                )
            }
            next = notForUploadStatusDao.find(imageSHA1)

            // After checking the image in the not for upload table, if the image is present then
            // skips the image and moves to next image for checking
            if(next > 0){
                continue

            // Otherwise checks in already uploaded table
            } else {
                next = uploadedStatusDao.findByImageSHA1(imageSHA1, true)

                // If the image is not present in the already uploaded table, checks for its
                // modified SHA1 in already uploaded table
                if (next <= 0) {
                    val modifiedImageSha1 = getSHA1(it, defaultDispatcher)
                    next = uploadedStatusDao.findByModifiedImageSHA1(
                        modifiedImageSha1,
                        true
                    )

                    // If the modified image SHA1 is not present in the already uploaded table,
                    // returns the position as next actionable image position
                    if (next <= 0) {
                        return i

                    // If present in the db then skips iteration for the image and moves to the next
                    // for checking
                    } else {
                        continue
                    }

                // If present in the db then skips iteration for the image and moves to the next
                // for checking
                } else {
                    continue
                }
            }
        }
        return -1
    }

    /**
     * Get SHA1, return SHA1 if available, otherwise generate and store the SHA1.
     *
     * @return sha1 of the image
     */
    suspend fun getSHA1(image: Image, defaultDispatcher: CoroutineDispatcher): String {
        mapModifiedImageSHA1[image]?.let{
            return it
        }
        val sha1 = CustomSelectorUtils
            .generateModifiedSHA1(image,
                defaultDispatcher,
                context,
                fileProcessor,
                fileUtilsWrapper
            )
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