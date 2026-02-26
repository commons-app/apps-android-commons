package fr.free.nrw.commons.customselector.domain.use_case

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import fr.free.nrw.commons.customselector.ui.selector.ImageLoader
import fr.free.nrw.commons.filepicker.PickedFiles
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.upload.FileProcessor
import fr.free.nrw.commons.upload.FileUtilsWrapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileNotFoundException
import timber.log.Timber
import java.io.IOException
import java.net.UnknownHostException
import javax.inject.Inject

class ImageUseCase @Inject constructor(
    private val fileUtilsWrapper: FileUtilsWrapper,
    private val fileProcessor: FileProcessor,
    private val mediaClient: MediaClient,
    private val context: Context
) {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /**
     * Retrieves the SHA1 hash of an image from its URI.
     *
     * @param uri The URI of the image.
     * @return The SHA1 hash of the image, or an empty string if the image is not found.
     */
    suspend fun getImageSHA1(uri: Uri): String = withContext(ioDispatcher) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            fileUtilsWrapper.getSHA1(inputStream)
        } catch (e: FileNotFoundException) {
            Timber.e(e)
            ""
        }
    }

    /**
     * Generates a modified SHA1 hash of an image after redacting sensitive EXIF tags.
     *
     * @param imageUri The URI of the image to process.
     * @return The modified SHA1 hash of the image.
     */
    suspend fun generateModifiedSHA1(imageUri: Uri): String = withContext(ioDispatcher) {
        val uploadableFile = PickedFiles.pickedExistingPicture(context, imageUri)
        val exifInterface: ExifInterface? = try {
                ExifInterface(uploadableFile.file!!)
            } catch (e: IOException) {
                Timber.e(e)
                null
            }
        fileProcessor.redactExifTags(exifInterface, fileProcessor.getExifTagsToRedact())

        val sha1 = fileUtilsWrapper.getSHA1(
                fileUtilsWrapper.getFileInputStream(uploadableFile.getFilePath()))
        uploadableFile.file.delete()
        sha1
    }

    /**
     * Checks whether a file with the given SHA1 hash exists on Wikimedia Commons.
     *
     * @param sha1 The SHA1 hash of the file to check.
     * @return An ImageLoader.Result indicating the existence of the file on Commons.
     */
    suspend fun checkWhetherFileExistsOnCommonsUsingSHA1(
        sha1: String
    ): ImageLoader.Result = withContext(ioDispatcher) {
        return@withContext try {
            if (mediaClient.checkFileExistsUsingSha(sha1).blockingGet()) {
                ImageLoader.Result.TRUE
            } else {
                ImageLoader.Result.FALSE
            }
        } catch (e: UnknownHostException) {
            Timber.e(e, "Network Connection Error")
            ImageLoader.Result.ERROR
        } catch (e: Exception) {
            e.printStackTrace()
            ImageLoader.Result.ERROR
        }
    }
}