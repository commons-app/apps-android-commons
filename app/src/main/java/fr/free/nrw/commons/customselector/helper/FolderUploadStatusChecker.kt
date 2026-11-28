package fr.free.nrw.commons.customselector.helper

import android.content.Context
import android.net.Uri
import fr.free.nrw.commons.customselector.database.NotForUploadStatusDao
import fr.free.nrw.commons.customselector.database.UploadedStatusDao
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.upload.FileUtilsWrapper
import fr.free.nrw.commons.utils.CustomSelectorUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/**
 * Finds the folders of the custom selector whose pictures have all been handled already, that is
 * uploaded to Commons or marked as not for upload.
 *
 * Only the local upload status cache is used, so no request is sent and a picture only counts
 * once the app has confirmed it. A picture that is added to a folder later has no entry yet, so
 * the folder is shown as not handled again.
 */
class FolderUploadStatusChecker
    @Inject
    constructor(
        private val context: Context,
        private val uploadedStatusDao: UploadedStatusDao,
        private val notForUploadStatusDao: NotForUploadStatusDao,
        private val fileUtilsWrapper: FileUtilsWrapper,
    ) {
        /**
         * SHA1 of the pictures that were already read, so that a folder is only read once.
         */
        private val sha1Cache: MutableMap<Pair<Uri, String>, String> = HashMap()

        /**
         * Returns the bucket ids of the folders whose pictures have all been handled.
         */
        suspend fun findHandledFolders(
            folders: List<Folder>,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ): Set<Long> =
            folders
                .filter { isFolderHandled(it, ioDispatcher) }
                .map { it.bucketId }
                .toSet()

        /**
         * Whether every picture of the folder has been uploaded or marked as not for upload.
         * Stops at the first picture that still needs to be handled, so most folders only need
         * their first picture to be read.
         */
        suspend fun isFolderHandled(
            folder: Folder,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ): Boolean {
            // Copied, as the folder screen removes deleted pictures from the list while this runs
            val images = folder.images.toList()
            return images.isNotEmpty() && images.all { isImageHandled(it, ioDispatcher) }
        }

        private suspend fun isImageHandled(
            image: Image,
            ioDispatcher: CoroutineDispatcher,
        ): Boolean {
            val sha1 = getSHA1(image, ioDispatcher)
            return sha1.isNotEmpty() && (notForUploadStatusDao.find(sha1) > 0 || isUploaded(sha1))
        }

        /**
         * Whether Commons has the picture. Also covers pictures that were uploaded after the app
         * changed their EXIF data.
         */
        private suspend fun isUploaded(sha1: String): Boolean {
            val uploadedStatus = uploadedStatusDao.getFromImageSHA1(sha1)
            return uploadedStatus != null && (uploadedStatus.imageResult || uploadedStatus.modifiedImageResult)
        }

        private suspend fun getSHA1(
            image: Image,
            ioDispatcher: CoroutineDispatcher,
        ): String {
            val key = image.uri to image.date
            sha1Cache[key]?.let { return it }
            val sha1 =
                CustomSelectorUtils.getImageSHA1(
                    image.uri,
                    ioDispatcher,
                    fileUtilsWrapper,
                    context.contentResolver,
                )
            if (sha1.isNotEmpty()) {
                sha1Cache[key] = sha1
            }
            return sha1
        }
    }
