package fr.free.nrw.commons.filepicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.preference.PreferenceManager
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.selector.CustomSelectorActivity
import fr.free.nrw.commons.filepicker.PickedFiles.singleFileList
import fr.free.nrw.commons.upload.FileUtils
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.io.FileOutputStream


object FilePicker : Constants {

    private const val KEY_PHOTO_URI = "photo_uri"
    private const val KEY_VIDEO_URI = "video_uri"
    private const val KEY_LAST_CAMERA_PHOTO = "last_photo"
    private const val KEY_TYPE = "type"

    // Add extra for single selection
    private const val EXTRA_SINGLE_SELECTION = "EXTRA_SINGLE_SELECTION"

    /**
     * Returns the uri of the clicked image so that it can be put in MediaStore
     */
    @Throws(IOException::class)
    @JvmStatic
    private fun createCameraPictureFile(context: Context): Uri {
        val imagePath = PickedFiles.getCameraPicturesLocation(context)
        val uri = PickedFiles.getUriToFile(context, imagePath)
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putString(KEY_PHOTO_URI, uri.toString())
        editor.putString(KEY_LAST_CAMERA_PHOTO, imagePath.toString())
        editor.apply()
        return uri
    }


    @JvmStatic
    private fun createGalleryIntent(
        context: Context,
        type: Int,
        openDocumentIntentPreferred: Boolean
    ): Intent {
        // storing picked image type to shared preferences
        storeType(context, type)
        // Supported types are SVG, PNG and JPEG, GIF, TIFF, WebP, XCF and HEIC (for future conversion)
        val mimeTypes = arrayOf(
            "image/jpg",
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/tiff",
            "image/webp",
            "image/xcf",
            "image/svg+xml",
            "image/heic",
            "image/heif"
        )
        return plainGalleryPickerIntent(openDocumentIntentPreferred)
            .putExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                configuration(context).allowsMultiplePickingInGallery()
            )
            .putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
    }

    /**
     * CreateCustomSectorIntent, creates intent for custom selector activity.
     * @param context
     * @param type
     * @param singleSelection If true, restricts to single image selection
     * @return Custom selector intent
     */
    @JvmStatic
    private fun createCustomSelectorIntent(context: Context, type: Int, singleSelection: Boolean = false): Intent {
        storeType(context, type)
        val intent = Intent(context, CustomSelectorActivity::class.java)
        if (singleSelection) {
            intent.putExtra(EXTRA_SINGLE_SELECTION, true)
        }
        return intent
    }

    @JvmStatic
    private fun createCameraForImageIntent(context: Context, type: Int): Intent {
        storeType(context, type)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            val capturedImageUri = createCameraPictureFile(context)
            // We have to explicitly grant the write permission since Intent.setFlag works only on API Level >=20
            grantWritePermission(context, intent, capturedImageUri)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return intent
    }

    @JvmStatic
    private fun revokeWritePermission(context: Context, uri: Uri) {
        context.revokeUriPermission(
            uri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }

    @JvmStatic
    private fun grantWritePermission(context: Context, intent: Intent, uri: Uri) {
        val resInfoList =
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            context.grantUriPermission(
                packageName,
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    @JvmStatic
    private fun storeType(context: Context, type: Int) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(KEY_TYPE, type).apply()
    }

    @JvmStatic
    private fun restoreType(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_TYPE, 0)
    }

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
        openDocumentIntentPreferred: Boolean
    ) {
        val intent = createGalleryIntent(activity, type, openDocumentIntentPreferred)
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
        singleSelection: Boolean = false
    ) {
        val intent = createCustomSelectorIntent(activity, type, singleSelection)
        resultLauncher.launch(intent)
    }

    /**
     * Opens the camera app to pick image clicked by user
     */
    @JvmStatic
    fun openCameraForImage(
        activity: Activity,
        resultLauncher: ActivityResultLauncher<Intent>,
        type: Int
    ) {
        val intent = createCameraForImageIntent(activity, type)
        resultLauncher.launch(intent)
    }

    @Throws(URISyntaxException::class)
    @JvmStatic
    private fun takenCameraPicture(context: Context): UploadableFile? {
        val lastCameraPhoto = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_LAST_CAMERA_PHOTO, null)
        return if (lastCameraPhoto != null) {
            UploadableFile(File(lastCameraPhoto))
        } else {
            null
        }
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
    fun handleExternalImagesPicked(data: Intent?, activity: Activity): ExternalImagesResult {
        return try {
            val (supportedUris, unsupportedUris) = getUrisFromIntent(data).partition { uri ->
                FileUtils.isSupportedFileType(activity, uri)
            }
            ExternalImagesResult(getFilesFromUris(supportedUris, activity), unsupportedUris.size)
        } catch (e: IOException) {
            e.printStackTrace()
            ExternalImagesResult(emptyList(), 0)
        } catch (e: SecurityException) {
            e.printStackTrace()
            ExternalImagesResult(emptyList(), 0)
        }
    }

    @JvmStatic
    private fun isPhoto(data: Intent?): Boolean {
        return data == null || (data.data == null && data.clipData == null)
    }

    @JvmStatic
    private fun plainGalleryPickerIntent(
        openDocumentIntentPreferred: Boolean
    ): Intent {
        /*
         * Asking for ACCESS_MEDIA_LOCATION at runtime solved the location-loss issue
         * in the custom selector in Contributions fragment.
         * Detailed discussion: https://github.com/commons-app/apps-android-commons/issues/5015
         *
         * This permission check, however, was insufficient to fix location-loss in
         * the regular selector in Contributions fragment and Nearby fragment,
         * especially on some devices running Android 13 that use the new Photo Picker by default.
         *
         * New Photo Picker: https://developer.android.com/training/data-storage/shared/photopicker
         *
         * The new Photo Picker introduced by Android redacts location tags from EXIF metadata.
         * Reported on the Google Issue Tracker: https://issuetracker.google.com/issues/243294058
         * Status: Won't fix (Intended behaviour)
         *
         * Switched intent from ACTION_GET_CONTENT to ACTION_OPEN_DOCUMENT (by default; can
         * be changed through the Setting page) as:
         *
         * ACTION_GET_CONTENT opens the 'best application' for choosing that kind of data
         * The best application is the new Photo Picker that redacts the location tags
         *
         * ACTION_OPEN_DOCUMENT, however,  displays the various DocumentsProvider instances
         * installed on the device, letting the user interactively navigate through them.
         *
         * So, this allows us to use the traditional file picker that does not redact location tags
         * from EXIF.
         *
         */
        val intent = if (openDocumentIntentPreferred) {
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
            }
        } else {
            Intent(Intent.ACTION_GET_CONTENT)
        }
        intent.type = "image/*"
        return intent
    }

    @JvmStatic
    fun onPictureReturnedFromDocuments(
        result: ActivityResult,
        activity: Activity,
        callbacks: Callbacks
    ) {
        if (result.resultCode == Activity.RESULT_OK && !isPhoto(result.data)) {
            takePersistableUriPermissions(activity, result)
            try {
                val files = getFilesFromGalleryPictures(result.data, activity)
                callbacks.onImagesPicked(files, ImageSource.DOCUMENTS, restoreType(activity))
            } catch (e: Exception) {
                e.printStackTrace()
                callbacks.onImagePickerError(e, ImageSource.DOCUMENTS, restoreType(activity))
            }
        } else {
            callbacks.onCanceled(ImageSource.DOCUMENTS, restoreType(activity))
        }
    }

    /**
     * takePersistableUriPermission is necessary to persist the URI permission as
     * the permission granted by the system for read or write access on ACTION_OPEN_DOCUMENT
     * lasts only until the user's device restarts.
     * Ref: https://developer.android.com/training/data-storage/shared/documents-files#persist-permissions
     *
     * This helps fix the SecurityException reported in this issue:
     * https://github.com/commons-app/apps-android-commons/issues/6357
     */
    private fun takePersistableUriPermissions(context: Context, result: ActivityResult) {
        result.data?.let { intentData ->
            val takeFlags: Int = (Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Persist the URI permission for all URIs in the clip data
            // if multiple images are selected,
            // or for the single URI if only one image is selected
            intentData.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    context.contentResolver.takePersistableUriPermission(
                        clipData.getItemAt(i).uri, takeFlags)
                }
            } ?: intentData.data?.let { uri ->
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
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
        callbacks: Callbacks
    ) {
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val files = getFilesFromCustomSelector(result.data, activity)
                callbacks.onImagesPicked(files, ImageSource.CUSTOM_SELECTOR, restoreType(activity))
            } catch (e: Exception) {
                e.printStackTrace()
                callbacks.onImagePickerError(e, ImageSource.CUSTOM_SELECTOR, restoreType(activity))
            }
        } else {
            callbacks.onCanceled(ImageSource.CUSTOM_SELECTOR, restoreType(activity))
        }
    }

    /**
     * Get files from custom selector
     * Retrieve and process the selected images from the custom selector.
     */
    @Throws(IOException::class, SecurityException::class)
    @JvmStatic
    private fun getFilesFromCustomSelector(
        data: Intent?,
        activity: Activity
    ): List<UploadableFile> {
        val files = mutableListOf<UploadableFile>()
        val images = data?.getParcelableArrayListExtra<Image>("Images")
        images?.forEach { image ->
            val uri = image.uri
            var file = PickedFiles.pickedExistingPicture(activity, uri)
            file = handleHeicFile(file, activity)
            files.add(file)
        }

        if (configuration(activity).shouldCopyPickedImagesToPublicGalleryAppFolder()) {
            PickedFiles.copyFilesInSeparateThread(activity, files)
        }

        return files
    }

    @JvmStatic
    fun onPictureReturnedFromGallery(
        result: ActivityResult,
        activity: Activity,
        callbacks: Callbacks
    ) {
        if (result.resultCode == Activity.RESULT_OK && !isPhoto(result.data)) {
            takePersistableUriPermissions(activity, result)
            try {
                val files = getFilesFromGalleryPictures(result.data, activity)
                callbacks.onImagesPicked(files, ImageSource.GALLERY, restoreType(activity))
            } catch (e: Exception) {
                e.printStackTrace()
                callbacks.onImagePickerError(e, ImageSource.GALLERY, restoreType(activity))
            }
        } else {
            callbacks.onCanceled(ImageSource.GALLERY, restoreType(activity))
        }
    }
    @JvmStatic
    fun convertHeicToJpg(file: File, context: Context): File {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, Uri.fromFile(file))
            ImageDecoder.decodeBitmap(source)
        } else {
            throw IllegalStateException("HEIC conversion requires Android P+")
        }

        val jpgFile = File(file.parent, file.nameWithoutExtension + ".jpg")
        FileOutputStream(jpgFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        return jpgFile
    }
    @JvmStatic
    private fun handleHeicFile(file: UploadableFile, activity: Activity): UploadableFile {
        val extension = file.file.extension.lowercase()
        return if (extension == "heic" || extension == "heif") {
            val jpgFile = convertHeicToJpg(file.file, activity)
            val uploadableFile = UploadableFile(jpgFile)
            uploadableFile.hasUnsupportedFormat = true
            uploadableFile
        } else {
            file
        }
    }

    @Throws(IOException::class, SecurityException::class)
    @JvmStatic
    private fun getFilesFromGalleryPictures(
        data: Intent?,
        activity: Activity
    ): List<UploadableFile> {
        return getFilesFromUris(getUrisFromIntent(data), activity)
    }

    /**
     * Returns the uris of the files in the intent, from its clip data if present
     */
    @JvmStatic
    private fun getUrisFromIntent(data: Intent?): List<Uri> {
        val clipData = data?.clipData ?: return listOf(data?.data!!)
        return (0 until clipData.itemCount).map { clipData.getItemAt(it).uri }
    }

    /**
     * Copies the files behind the given uris so that they can be uploaded
     */
    @Throws(IOException::class, SecurityException::class)
    @JvmStatic
    private fun getFilesFromUris(
        uris: List<Uri>,
        activity: Activity
    ): List<UploadableFile> {
        val files = uris.map { uri ->
            handleHeicFile(PickedFiles.pickedExistingPicture(activity, uri), activity)
        }

        if (configuration(activity).shouldCopyPickedImagesToPublicGalleryAppFolder()) {
            PickedFiles.copyFilesInSeparateThread(activity, files)
        }

        return files
    }

    @JvmStatic
    fun onPictureReturnedFromCamera(
        activityResult: ActivityResult,
        activity: Activity,
        callbacks: Callbacks
    ) {
        if (activityResult.resultCode != Activity.RESULT_OK) {
            callbacks.onCanceled(ImageSource.CAMERA_IMAGE, restoreType(activity))
            return
        }

        try {
            processSuccessfulCameraResult(activity, callbacks)
        } catch (e: Exception) {
            e.printStackTrace()
            callbacks.onImagePickerError(e, ImageSource.CAMERA_IMAGE, restoreType(activity))
        }
    }

    private fun processSuccessfulCameraResult(activity: Activity, callbacks: Callbacks) {
        revokeLastCameraPermission(activity)

        val photoFile = takenCameraPicture(activity)
        if (photoFile == null) {
            val e = IllegalStateException("Unable to get the picture returned from camera")
            callbacks.onImagePickerError(e, ImageSource.CAMERA_IMAGE, restoreType(activity))
        } else {
            handleValidCameraPhoto(activity, callbacks, photoFile)
        }

        clearCameraPreferences(activity)
    }

    private fun revokeLastCameraPermission(activity: Activity) {
        val lastImageUri = PreferenceManager.getDefaultSharedPreferences(activity)
            .getString(KEY_PHOTO_URI, null)
        if (!lastImageUri.isNullOrEmpty()) {
            revokeWritePermission(activity, Uri.parse(lastImageUri))
        }
    }

    private fun handleValidCameraPhoto(
        activity: Activity,
        callbacks: Callbacks,
        photoFile: UploadableFile
    ) {
        if (configuration(activity).shouldCopyTakenPhotosToPublicGalleryAppFolder()) {
            PickedFiles.copyFilesInSeparateThread(activity, singleFileList(photoFile))
        }
        val files = listOf(photoFile)
        callbacks.onImagesPicked(files, ImageSource.CAMERA_IMAGE, restoreType(activity))
    }

    private fun clearCameraPreferences(activity: Activity) {
        PreferenceManager.getDefaultSharedPreferences(activity).edit()
            .remove(KEY_LAST_CAMERA_PHOTO)
            .remove(KEY_PHOTO_URI)
            .apply()
    }

    @JvmStatic
    fun configuration(context: Context): FilePickerConfiguration {
        return FilePickerConfiguration(context)
    }

    enum class ImageSource {
        GALLERY, DOCUMENTS, CAMERA_IMAGE, CAMERA_VIDEO, CUSTOM_SELECTOR
    }

    interface Callbacks {
        fun onImagePickerError(e: Exception, source: ImageSource, type: Int)

        fun onImagesPicked(imageFiles: List<UploadableFile>, source: ImageSource, type: Int)

        fun onCanceled(source: ImageSource, type: Int)
    }

    fun interface HandleActivityResult {
        fun onHandleActivityResult(callbacks: Callbacks)
    }
}
