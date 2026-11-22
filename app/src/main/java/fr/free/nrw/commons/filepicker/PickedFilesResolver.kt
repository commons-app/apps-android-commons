package fr.free.nrw.commons.filepicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.preference.PreferenceManager
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.filepicker.FilePicker.KEY_TYPE
import java.io.IOException

/**
 * Turns the results returned by the pickers into files that the upload flow can use.
 */

object PickedFilesResolver {
    /**
     * Converts a HEIC or HEIF file to JPEG and marks it as having an unsupported format. Any
     * other file is returned unchanged.
     *
     * @param file The picked file
     * @param activity The activity used for the conversion
     * @return The converted file for HEIC and HEIF, otherwise the same file
     */
    @JvmStatic
    private fun handleHeicFile(
        file: UploadableFile,
        activity: Activity,
    ): UploadableFile {
        val extension = file.file.extension.lowercase()
        return if (extension == "heic" || extension == "heif") {
            val jpgFile = FilePicker.convertHeicToJpg(file.file, activity)
            val uploadableFile = UploadableFile(jpgFile)
            uploadableFile.hasUnsupportedFormat = true
            uploadableFile
        } else {
            file
        }
    }

    /**
     * Returns the files for the images that the gallery or the document picker returned.
     *
     * @param data The intent returned by the picker
     * @param activity The activity used to read and copy the files
     * @return The uploadable files
     */
    @Throws(IOException::class, SecurityException::class)
    @JvmStatic
    internal fun getFilesFromGalleryPictures(
        data: Intent?,
        activity: Activity,
    ): List<UploadableFile> = getFilesFromUris(getUrisFromIntent(data), activity)

    /**
     * Returns the uris of the files in the intent, from its clip data if present
     */
    @JvmStatic
    internal fun getUrisFromIntent(data: Intent?): List<Uri> {
        val clipData = data?.clipData ?: return listOf(data?.data!!)
        return (0 until clipData.itemCount).map { clipData.getItemAt(it).uri }
    }

    /**
     * Copies the files behind the given uris so that they can be uploaded
     */
    @Throws(IOException::class, SecurityException::class)
    @JvmStatic
    internal fun getFilesFromUris(
        uris: List<Uri>,
        activity: Activity,
    ): List<UploadableFile> {
        val files =
            uris.map { uri ->
                handleHeicFile(PickedFiles.pickedExistingPicture(activity, uri), activity)
            }

        if (FilePicker.configuration(activity).shouldCopyPickedImagesToPublicGalleryAppFolder()) {
            PickedFilesCopier.copyFilesInSeparateThread(activity, files)
        }

        return files
    }

    /**
     * Get files from custom selector
     * Retrieve and process the selected images from the custom selector.
     */
    @Throws(IOException::class, SecurityException::class)
    @JvmStatic
    internal fun getFilesFromCustomSelector(
        data: Intent?,
        activity: Activity,
    ): List<UploadableFile> {
        val files = mutableListOf<UploadableFile>()
        val images = data?.getParcelableArrayListExtra<Image>("Images")
        images?.forEach { image ->
            val uri = image.uri
            var file = PickedFiles.pickedExistingPicture(activity, uri)
            file = handleHeicFile(file, activity)
            files.add(file)
        }

        if (FilePicker.configuration(activity).shouldCopyPickedImagesToPublicGalleryAppFolder()) {
            PickedFilesCopier.copyFilesInSeparateThread(activity, files)
        }

        return files
    }

    /**
     * Returns true when the intent is null or carries neither a uri nor clip data, which means
     * that no image was returned.
     *
     * @param data The intent returned by the picker
     * @return Whether the intent holds no picked image
     */
    @JvmStatic
    internal fun isPhoto(data: Intent?): Boolean = data == null || (data.data == null && data.clipData == null)

    /**
     * takePersistableUriPermission is necessary to persist the URI permission as
     * the permission granted by the system for read or write access on ACTION_OPEN_DOCUMENT
     * lasts only until the user's device restarts.
     * Ref: https://developer.android.com/training/data-storage/shared/documents-files#persist-permissions
     *
     * This helps fix the SecurityException reported in this issue:
     * https://github.com/commons-app/apps-android-commons/issues/6357
     */
    internal fun takePersistableUriPermissions(
        context: Context,
        result: ActivityResult,
    ) {
        result.data?.let { intentData ->
            val takeFlags: Int = (Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Persist the URI permission for all URIs in the clip data
            // if multiple images are selected,
            // or for the single URI if only one image is selected
            intentData.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    context.contentResolver.takePersistableUriPermission(
                        clipData.getItemAt(i).uri,
                        takeFlags,
                    )
                }
            } ?: intentData.data?.let { uri ->
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
        }
    }

    /**
     * Returns the type that was stored for the last request, or 0 if none was stored.
     *
     * @param context The context used to access the shared preferences
     * @return The stored type
     */
    @JvmStatic
    internal fun restoreType(context: Context): Int =
        PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_TYPE, 0)
}
