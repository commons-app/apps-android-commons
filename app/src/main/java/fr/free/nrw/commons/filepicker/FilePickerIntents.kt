package fr.free.nrw.commons.filepicker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.preference.PreferenceManager
import fr.free.nrw.commons.customselector.ui.selector.CustomSelectorActivity
import fr.free.nrw.commons.filepicker.FilePicker.KEY_LAST_CAMERA_PHOTO
import fr.free.nrw.commons.filepicker.FilePicker.KEY_PHOTO_URI
import fr.free.nrw.commons.filepicker.FilePicker.KEY_TYPE
import fr.free.nrw.commons.filepicker.FilePicker.configuration
import timber.log.Timber
import java.io.IOException

/**
 * Builds the intents used to pick images from the gallery or the custom selector and to take
 * pictures with the camera. It also keeps the picked type and the camera photo location in the
 * shared preferences.
 */
object FilePickerIntents {
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

    /**
     * Creates the intent that opens the gallery or the document picker for images. The type is
     * stored first so that it can be returned with the result.
     *
     * @param context The context used to store the type and read the picker configuration
     * @param type Custom type of the caller, which is returned with the images
     * @param openDocumentIntentPreferred Whether ACTION_OPEN_DOCUMENT is preferred over
     * ACTION_GET_CONTENT
     * @return The picker intent, limited to the image types that Commons supports
     */
    @JvmStatic
    internal fun createGalleryIntent(
        context: Context,
        type: Int,
        openDocumentIntentPreferred: Boolean,
    ): Intent {
        // storing picked image type to shared preferences
        storeType(context, type)
        // Supported types are SVG, PNG and JPEG, GIF, TIFF, WebP, XCF and HEIC (for future conversion)
        val mimeTypes =
            arrayOf(
                "image/jpg",
                "image/png",
                "image/jpeg",
                "image/gif",
                "image/tiff",
                "image/webp",
                "image/xcf",
                "image/svg+xml",
                "image/heic",
                "image/heif",
            )
        return plainGalleryPickerIntent(openDocumentIntentPreferred)
            .putExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                configuration(context).allowsMultiplePickingInGallery(),
            ).putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
    }

    /**
     * CreateCustomSectorIntent, creates intent for custom selector activity.
     * @param context
     * @param type
     * @param singleSelection If true, restricts to single image selection
     * @return Custom selector intent
     */
    @JvmStatic
    internal fun createCustomSelectorIntent(
        context: Context,
        type: Int,
        singleSelection: Boolean = false,
    ): Intent {
        storeType(context, type)
        val intent = Intent(context, CustomSelectorActivity::class.java)
        if (singleSelection) {
            intent.putExtra(EXTRA_SINGLE_SELECTION, true)
        }
        return intent
    }

    /**
     * Creates the intent that opens the camera app to take a picture. If the picture file cannot
     * be prepared, the error is logged and the intent is returned without an output uri.
     *
     * @param context The context used to store the type and prepare the picture file
     * @param type Custom type of the caller, which is returned with the image
     * @return The intent that captures an image
     */
    @JvmStatic
    internal fun createCameraForImageIntent(
        context: Context,
        type: Int,
    ): Intent {
        storeType(context, type)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            val capturedImageUri = createCameraPictureFile(context)
            // We have to explicitly grant the write permission since Intent.setFlag works only on API Level >=20
            grantWritePermission(context, intent, capturedImageUri)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri)
        } catch (e: Exception) {
            Timber.e(e, "Could not prepare camera intent")
        }

        return intent
    }

    /**
     * Grants read and write permission on the uri to every app that can handle the intent.
     *
     * @param context The context used to look up the apps and grant the permission
     * @param intent The intent that the apps must be able to handle
     * @param uri The uri to grant the permission on
     */
    @JvmStatic
    private fun grantWritePermission(
        context: Context,
        intent: Intent,
        uri: Uri,
    ) {
        val resInfoList =
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            context.grantUriPermission(
                packageName,
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    /**
     * Stores the type given by the caller in the shared preferences, so that it can be returned
     * with the result.
     *
     * @param context The context used to access the shared preferences
     * @param type Custom type of the caller
     */
    @JvmStatic
    private fun storeType(
        context: Context,
        type: Int,
    ) {
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .putInt(KEY_TYPE, type)
            .apply()
    }

    /**
     * Creates the base intent for picking an image file.
     *
     * @param openDocumentIntentPreferred If true, ACTION_OPEN_DOCUMENT is used, otherwise
     * ACTION_GET_CONTENT
     * @return The picker intent, set to accept any image type
     */
    @JvmStatic
    private fun plainGalleryPickerIntent(openDocumentIntentPreferred: Boolean): Intent {
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
        val intent =
            if (openDocumentIntentPreferred) {
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
            } else {
                Intent(Intent.ACTION_GET_CONTENT)
            }
        intent.type = "image/*"
        return intent
    }
}
