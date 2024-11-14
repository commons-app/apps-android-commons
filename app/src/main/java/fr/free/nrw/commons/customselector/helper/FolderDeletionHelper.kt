package fr.free.nrw.commons.customselector.helper

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.appcompat.app.AlertDialog
import fr.free.nrw.commons.R
import timber.log.Timber
import java.io.File

object FolderDeletionHelper {

    /**
     * Prompts the user to confirm deletion of a specified folder and, if confirmed, deletes it.
     *
     * @param context The context used to show the confirmation dialog and manage deletion.
     * @param folder The folder to be deleted.
     * @param onDeletionComplete Callback invoked with `true` if the folder was
     * @param trashFolderLauncher An ActivityResultLauncher for handling the result of the trash request.
     * successfully deleted, `false` otherwise.
     */
    fun confirmAndDeleteFolder(
        context: Context,
        folder: File,
        trashFolderLauncher: ActivityResultLauncher<IntentSenderRequest>,
        onDeletionComplete: (Boolean) -> Unit) {
        val itemCount = countItemsInFolder(context, folder)
        val folderPath = folder.absolutePath

        //don't show this dialog on API 30+, it's handled automatically using MediaStore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val success = deleteFolderMain(context, folder, trashFolderLauncher)
            onDeletionComplete(success)

        } else {
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.custom_selector_confirm_deletion_title))
                .setMessage(context.getString(R.string.custom_selector_confirm_deletion_message, folderPath, itemCount))
                .setPositiveButton(context.getString(R.string.custom_selector_delete)) { _, _ ->

                    //proceed with deletion if user confirms
                    val success = deleteFolderMain(context, folder, trashFolderLauncher)
                    onDeletionComplete(success)
                }
                .setNegativeButton(context.getString(R.string.custom_selector_cancel)) { dialog, _ ->
                    dialog.dismiss()
                    onDeletionComplete(false)
                }
                .show()
        }
    }

    /**
     * Deletes the specified folder, handling different Android storage models based on the API
     *
     * @param context The context used to manage storage operations.
     * @param folder The folder to delete.
     * @param trashFolderLauncher An ActivityResultLauncher for handling the result of the trash request.
     * @return `true` if the folder deletion was successful, `false` otherwise.
     */
    private fun deleteFolderMain(
        context: Context,
        folder: File,
        trashFolderLauncher: ActivityResultLauncher<IntentSenderRequest>): Boolean
    {
        return when {
            //for API 30 and above, use MediaStore
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> trashFolderContents(context, folder, trashFolderLauncher)

            //for API 29 ('requestLegacyExternalStorage' is set to true in Manifest)
            // and below use file system
            else -> deleteFolderLegacy(folder)
        }
    }

    /**
     * Moves all contents of a specified folder to the trash on devices running
     * Android 11 (API level 30) and above.
     *
     * @param context The context used to access the content resolver.
     * @param folder The folder whose contents are to be moved to the trash.
     * @param trashFolderLauncher An ActivityResultLauncher for handling the result of the trash request.
     * @return `true` if the trash request was initiated successfully, `false` otherwise.
     */
    private fun trashFolderContents(
        context: Context,
        folder: File,
        trashFolderLauncher: ActivityResultLauncher<IntentSenderRequest>): Boolean
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false

        val contentResolver = context.contentResolver
        val folderPath = folder.absolutePath
        val urisToTrash = mutableListOf<Uri>()

        // Use URIs specific to media items
        val mediaUris = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        )

        for (mediaUri in mediaUris) {
            val selection = "${MediaStore.MediaColumns.DATA} LIKE ?"
            val selectionArgs = arrayOf("$folderPath/%")

            contentResolver.query(mediaUri, arrayOf(MediaStore.MediaColumns._ID), selection,
                selectionArgs, null)
                ?.use{ cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val fileUri = ContentUris.withAppendedId(mediaUri, id)
                    urisToTrash.add(fileUri)
                }
            }
        }

        //proceed with trashing if we have valid URIs
        if (urisToTrash.isNotEmpty()) {
            try {
                val trashRequest = MediaStore.createTrashRequest(contentResolver, urisToTrash, true)
                val intentSenderRequest = IntentSenderRequest.Builder(trashRequest.intentSender).build()
                trashFolderLauncher.launch(intentSenderRequest)
                return true
            } catch (e: SecurityException) {
                Timber.tag("DeleteFolder").e(context.getString(R.string.custom_selector_error_trashing_folder_contents, e.message))
            }
        }
        return false
    }


    /**
     * Counts the number of items in a specified folder, including items in subfolders.
     *
     * @param context The context used to access the content resolver.
     * @param folder The folder in which to count items.
     * @return The total number of items in the folder.
     */
    private fun countItemsInFolder(context: Context, folder: File): Int {
        val contentResolver = context.contentResolver
        val folderPath = folder.absolutePath
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Images.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("$folderPath/%")

        return contentResolver.query(
            uri,
            arrayOf(MediaStore.Images.Media._ID),
            selection,
            selectionArgs,
            null)?.use { cursor ->
            cursor.count
        } ?: 0
    }



    /**
     * Refreshes the MediaStore for a specified folder, updating the system to recognize any changes
     *
     * @param context The context used to access the MediaScannerConnection.
     * @param folder The folder to refresh in the MediaStore.
     */
    fun refreshMediaStore(context: Context, folder: File) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(folder.absolutePath),
            null
        ) { _, _ -> }
    }



    /**
     * Deletes a specified folder and all of its contents on devices running
     * Android 10 (API level 29) and below.
     *
     * @param folder The `File` object representing the folder to be deleted.
     * @return `true` if the folder and all contents were deleted successfully; `false` otherwise.
     */
    private fun deleteFolderLegacy(folder: File): Boolean {
        return folder.deleteRecursively()
    }


    /**
     * Retrieves the absolute path of a folder given its unique identifier (bucket ID).
     *
     * @param context The context used to access the content resolver.
     * @param folderId The unique identifier (bucket ID) of the folder.
     * @return The absolute path of the folder as a `String`, or `null` if the folder is not found.
     */
    fun getFolderPath(context: Context, folderId: Long): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(folderId.toString())

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val fullPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                return File(fullPath).parent
            }
        }
        return null
    }

    /**
     * Displays an error message to the user and logs it for debugging purposes.
     *
     * @param context The context used to display the Toast.
     * @param message The error message to display and log.
     * @param folderName The name of the folder to delete.
     */
    fun showError(context: Context, message: String, folderName: String) {
        Toast.makeText(context,
            context.getString(R.string.custom_selector_folder_deleted_failure, folderName),
            Toast.LENGTH_SHORT).show()
        Timber.tag("DeleteFolder").e(message)
    }

    /**
     * Displays a success message to the user.
     *
     * @param context The context used to display the Toast.
     * @param message The success message to display.
     * @param folderName The name of the folder to delete.
     */
    fun showSuccess(context: Context, message: String, folderName: String) {
        Toast.makeText(context,
            context.getString(R.string.custom_selector_folder_deleted_success, folderName),
            Toast.LENGTH_SHORT).show()
        Timber.tag("DeleteFolder").d(message)
    }

}