package fr.free.nrw.commons.customselector.helper

import android.content.ContentResolver
import fr.free.nrw.commons.customselector.database.NotForUploadStatus
import fr.free.nrw.commons.customselector.database.NotForUploadStatusDao
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.upload.FileUtilsWrapper
import fr.free.nrw.commons.utils.CustomSelectorUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Marks images as not for upload in the database, and unmarks them again.
 */
class NotForUploadMarker(
    /**
     * NotForUploadStatus Dao class for database operations
     */
    private val notForUploadStatusDao: NotForUploadStatusDao,
    /**
     * FileUtilsWrapper class to get imageSHA1 from uri
     */
    private val fileUtilsWrapper: FileUtilsWrapper,
    /**
     * Content resolver used to read the images.
     */
    private val contentResolver: ContentResolver,
    /**
     * IO coroutine dispatcher.
     */
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /**
     * Marks the images as not for upload, or unmarks them if all of them are already marked.
     *
     * @param images The images to mark or unmark.
     * @return `true` if the images are now marked as not for upload, `false` if they were unmarked.
     */
    suspend fun toggle(images: List<Image>): Boolean {
        val imageSHA1s =
            images.map { image ->
                CustomSelectorUtils.getImageSHA1(
                    image.uri,
                    ioDispatcher,
                    fileUtilsWrapper,
                    contentResolver,
                )
            }
        val allImagesAlreadyNotForUpload =
            imageSHA1s.all { imageSHA1 -> notForUploadStatusDao.find(imageSHA1) >= 1 }

        if (allImagesAlreadyNotForUpload) {
            // Remove images from not for upload
            imageSHA1s.forEach { imageSHA1 -> notForUploadStatusDao.deleteNotForUploadWithImageSHA1(imageSHA1) }
        } else {
            // Insert images into not for upload
            imageSHA1s.forEach { imageSHA1 -> notForUploadStatusDao.insert(NotForUploadStatus(imageSHA1)) }
        }
        return !allImagesAlreadyNotForUpload
    }
}
