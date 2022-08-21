package fr.free.nrw.commons.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.filepicker.PickedFiles
import fr.free.nrw.commons.customselector.ui.selector.ImageLoader
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.upload.FileProcessor
import fr.free.nrw.commons.upload.FileUtilsWrapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException
import java.net.UnknownHostException

/**
 * Util Class for Custom Selector
 */
class CustomSelectorUtils {
    companion object {

        /**
         * Get image sha1 from uri, used to retrieve the original image sha1.
         */
        suspend fun getImageSHA1(
            uri: Uri,
            ioDispatcher: CoroutineDispatcher,
            fileUtilsWrapper: FileUtilsWrapper,
            contentResolver: ContentResolver
        ): String {
            return withContext(ioDispatcher) {

                try {
                    val result = fileUtilsWrapper.getSHA1(contentResolver.openInputStream(uri))
                    result
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                    ""
                }
            }
        }

        /**
         * Generates modified SHA1 of an image
         */
        suspend fun generateModifiedSHA1(
            image: Image,
            defaultDispatcher: CoroutineDispatcher,
            context: Context,
            fileProcessor: FileProcessor,
            fileUtilsWrapper: FileUtilsWrapper
        ): String {
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
                    fileUtilsWrapper.getSHA1(
                        fileUtilsWrapper.getFileInputStream(uploadableFile.filePath)
                    )
                uploadableFile.file.delete()
                sha1
            }
        }

        /**
         * Query SHA1, return result if previously queried, otherwise start a new query.
         *
         * @return Query result.
         */
        suspend fun querySHA1(SHA1: String,
                              ioDispatcher : CoroutineDispatcher,
                              mediaClient: MediaClient
        ): ImageLoader.Result {
            return withContext(ioDispatcher) {

                var result: ImageLoader.Result = ImageLoader.Result.FALSE
                try {
                    if (mediaClient.checkFileExistsUsingSha(SHA1).blockingGet()) {
                        result = ImageLoader.Result.TRUE
                    }
                } catch (e: Exception) {
                    if (e is UnknownHostException) {
                        // Handle no network connection.
                        Timber.e(e, "Network Connection Error")
                    }
                    result = ImageLoader.Result.ERROR
                    e.printStackTrace()
                }
                result
            }
        }
    }
}