package fr.free.nrw.commons.filepicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import fr.free.nrw.commons.filepicker.PickedFilesResolver.getFilesFromUris
import fr.free.nrw.commons.filepicker.PickedFilesResolver.getUrisFromIntent
import fr.free.nrw.commons.upload.FileUtils
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object FilePicker : Constants {
    internal const val KEY_PHOTO_URI = "photo_uri"
    internal const val KEY_LAST_CAMERA_PHOTO = "last_photo"
    internal const val KEY_TYPE = "type"

    /**
     * Opens default gallery or available galleries picker if there is no default
     *
     * @param type Custom type of your choice, which will be returned with the images
     */
    @JvmStatic
    fun openGallery(
        activity: Activity,
        resultLauncher: ActivityResultLauncher<Intent>,
        type: Int,
        openDocumentIntentPreferred: Boolean,
    ) {
        val intent = FilePickerIntents.createGalleryIntent(activity, type, openDocumentIntentPreferred)
        resultLauncher.launch(intent)
    }

    /**
     * Opens Custom Selector
     */
    @JvmStatic
    fun openCustomSelector(
        activity: Activity,
        resultLauncher: ActivityResultLauncher<Intent>,
        type: Int,
        singleSelection: Boolean = false,
    ) {
        val intent = FilePickerIntents.createCustomSelectorIntent(activity, type, singleSelection)
        resultLauncher.launch(intent)
    }

    /**
     * Opens the camera app to pick image clicked by user
     */
    @JvmStatic
    fun openCameraForImage(
        activity: Activity,
        resultLauncher: ActivityResultLauncher<Intent>,
        type: Int,
    ) {
        val intent = FilePickerIntents.createCameraForImageIntent(activity, type)
        resultLauncher.launch(intent)
    }

    @JvmStatic
    fun onPictureReturnedFromDocuments(
        result: ActivityResult,
        activity: Activity,
        callbacks: Callbacks,
    ) {
        if (result.resultCode == Activity.RESULT_OK && !PickedFilesResolver.isPhoto(result.data)) {
            PickedFilesResolver.takePersistableUriPermissions(activity, result)
            try {
                val files = PickedFilesResolver.getFilesFromGalleryPictures(result.data, activity)
                callbacks.onImagesPicked(files, ImageSource.DOCUMENTS, PickedFilesResolver.restoreType(activity))
            } catch (e: Exception) {
                Timber.e(e, "Could not handle images picked from documents")
                callbacks.onImagePickerError(e, ImageSource.DOCUMENTS, PickedFilesResolver.restoreType(activity))
            }
        } else {
            callbacks.onCanceled(ImageSource.DOCUMENTS, PickedFilesResolver.restoreType(activity))
        }
    }

    /**
     * onPictureReturnedFromCustomSelector.
     * Retrieve and forward the images to upload wizard through callback.
     */
    @JvmStatic
    fun onPictureReturnedFromCustomSelector(
        result: ActivityResult,
        activity: Activity,
        callbacks: Callbacks,
    ) {
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val files = PickedFilesResolver.getFilesFromCustomSelector(result.data, activity)
                callbacks.onImagesPicked(files, ImageSource.CUSTOM_SELECTOR, PickedFilesResolver.restoreType(activity))
            } catch (e: Exception) {
                Timber.e(e, "Could not handle images picked from the custom selector")
                callbacks.onImagePickerError(e, ImageSource.CUSTOM_SELECTOR, PickedFilesResolver.restoreType(activity))
            }
        } else {
            callbacks.onCanceled(ImageSource.CUSTOM_SELECTOR, PickedFilesResolver.restoreType(activity))
        }
    }

    @JvmStatic
    fun onPictureReturnedFromGallery(
        result: ActivityResult,
        activity: Activity,
        callbacks: Callbacks,
    ) {
        if (result.resultCode == Activity.RESULT_OK && !PickedFilesResolver.isPhoto(result.data)) {
            PickedFilesResolver.takePersistableUriPermissions(activity, result)
            try {
                val files = PickedFilesResolver.getFilesFromGalleryPictures(result.data, activity)
                callbacks.onImagesPicked(files, ImageSource.GALLERY, PickedFilesResolver.restoreType(activity))
            } catch (e: Exception) {
                Timber.e(e, "Could not handle images picked from the gallery")
                callbacks.onImagePickerError(e, ImageSource.GALLERY, PickedFilesResolver.restoreType(activity))
            }
        } else {
            callbacks.onCanceled(ImageSource.GALLERY, PickedFilesResolver.restoreType(activity))
        }
    }

    @JvmStatic
    fun onPictureReturnedFromCamera(
        activityResult: ActivityResult,
        activity: Activity,
        callbacks: Callbacks,
    ) {
        if (activityResult.resultCode != Activity.RESULT_OK) {
            callbacks.onCanceled(ImageSource.CAMERA_IMAGE, PickedFilesResolver.restoreType(activity))
            return
        }

        try {
            CameraResultHandler.processSuccessfulCameraResult(activity, callbacks)
        } catch (e: Exception) {
            Timber.e(e, "Could not handle image taken with the camera")
            callbacks.onImagePickerError(e, ImageSource.CAMERA_IMAGE, PickedFilesResolver.restoreType(activity))
        }
    }

    @JvmStatic
    fun convertHeicToJpg(
        file: File,
        context: Context,
    ): File {
        val bitmap =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, Uri.fromFile(file))
                ImageDecoder.decodeBitmap(source)
            } else {
                error("HEIC conversion requires Android P+")
            }

        val jpgFile = File(file.parent, file.nameWithoutExtension + ".jpg")
        FileOutputStream(jpgFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        return jpgFile
    }

    /**
     * Copies the files shared from another app so that they can be uploaded. Files in a format
     * that Commons does not accept, such as RAW photos shared along with their JPEG versions, are
     * skipped before they are copied.
     *
     * @param data The share intent
     * @param activity The activity that received the share
     * @return The uploadable files and the number of skipped files
     */
    @JvmStatic
    fun handleExternalImagesPicked(
        data: Intent?,
        activity: Activity,
    ): ExternalImagesResult =
        try {
            val (supportedUris, unsupportedUris) =
                getUrisFromIntent(data).partition { uri ->
                    FileUtils.isSupportedFileType(activity, uri)
                }
            ExternalImagesResult(getFilesFromUris(supportedUris, activity), unsupportedUris.size)
        } catch (e: IOException) {
            Timber.e(e, "Could not read externally shared images")
            ExternalImagesResult(emptyList(), 0)
        } catch (e: SecurityException) {
            Timber.e(e, "No permission to read externally shared images")
            ExternalImagesResult(emptyList(), 0)
        }

    @JvmStatic
    fun configuration(context: Context): FilePickerConfiguration = FilePickerConfiguration(context)

    enum class ImageSource {
        GALLERY,
        DOCUMENTS,
        CAMERA_IMAGE,
        CAMERA_VIDEO,
        CUSTOM_SELECTOR,
    }

    interface Callbacks {
        fun onImagePickerError(
            e: Exception,
            source: ImageSource,
            type: Int,
        )

        fun onImagesPicked(
            imageFiles: List<UploadableFile>,
            source: ImageSource,
            type: Int,
        )

        fun onCanceled(
            source: ImageSource,
            type: Int,
        )
    }

    fun interface HandleActivityResult {
        fun onHandleActivityResult(callbacks: Callbacks)
    }
}
