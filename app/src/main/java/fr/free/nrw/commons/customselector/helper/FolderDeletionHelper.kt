package fr.free.nrw.commons.customselector.helper

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import timber.log.Timber
import java.io.File

object FolderDeletionHelper {

    /**
     * Main function to confirm and delete a folder.
     */
    fun confirmAndDeleteFolder(context: Context, folder: File, onDeletionComplete: (Boolean) -> Unit) {
        val itemCount = countItemsInFolder(folder)
        val folderPath = folder.absolutePath

        // Show confirmation dialog
        AlertDialog.Builder(context)
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete the folder?\n\nPath: $folderPath\nItems: $itemCount")
            .setPositiveButton("Delete") { _, _ ->
                // Proceed with deletion if user confirms
                val success = deleteFolder(context, folder)
                onDeletionComplete(success)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                onDeletionComplete(false) // Return false if the user cancels
            }
            .show()
    }

    /**
     * Delete the folder based on the Android version.
     */
    private fun deleteFolder(context: Context, folder: File): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> deleteFolderScopedStorage(context, folder)
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> deleteFolderMediaStore(context, folder)
            else -> deleteFolderLegacy(folder)
        }
    }

    /**
     * Count the number of items in the folder, including subfolders.
     */
    private fun countItemsInFolder(folder: File): Int {
        return folder.listFiles()?.size ?: 0
    }
    /**
     * Deletes a folder using the Scoped Storage API for Android 11 (API level 30) and above.
     */
    private fun deleteFolderScopedStorage(context: Context, folder: File): Boolean {
        Timber.tag("FolderAction").d("Deleting folder using Scoped Storage API")

        // Implement deletion with Scoped Storage; fallback to recursive delete
        return folder.deleteRecursively().also {
            if (!it) {
                Timber.tag("FolderAction").e("Failed to delete folder with Scoped Storage API")
            }
        }
    }

    /**
     * Deletes a folder using the MediaStore API for Android 10 (API level 29).
     */
    /**
     * Deletes a folder using the MediaStore API for Android 10 (API level 29).
     */
    fun deleteFolderMediaStore(context: Context, folder: File): Boolean {
        Timber.tag("FolderAction").d("Deleting folder using MediaStore API on Android 10")

        val contentResolver = context.contentResolver
        val folderPath = folder.absolutePath
        var deletionSuccessful = true

        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Images.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("$folderPath/%")

        contentResolver.query(uri, arrayOf(MediaStore.Images.Media._ID), selection, selectionArgs, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                try {
                    val rowsDeleted = contentResolver.delete(imageUri, null, null)
                    if (rowsDeleted <= 0) {
                        Timber.tag("FolderAction").e("Failed to delete image with URI: $imageUri")
                        deletionSuccessful = false
                    }
                } catch (e: Exception) {
                    // Handle RecoverableSecurityException only for API 29 and above
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                        handleRecoverableSecurityException(context, e)
                        deletionSuccessful = false
                    } else {
                        Timber.tag("FolderAction").e("Error deleting file: ${e.message}")
                        deletionSuccessful = false
                    }
                }
            }
        }

        return deletionSuccessful
    }

    /**
     * Handles the RecoverableSecurityException for deletion requests requiring user confirmation.
     */
    private fun handleRecoverableSecurityException(context: Context, e: RecoverableSecurityException) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val intentSender = e.userAction.actionIntent.intentSender
                (context as? Activity)?.startIntentSenderForResult(
                    intentSender,
                    2,
                    null, 0, 0, 0
                )
            } catch (ex: IntentSender.SendIntentException) {
                Timber.tag("FolderAction").e("Error sending intent for deletion: ${ex.message}")
            }
        } else {
            Timber.tag("FolderAction").e("RecoverableSecurityException requires API 29 or higher")
        }
    }

    /**
     * Handles deletion for devices running Android 9 (API level 28) and below.
     */
    private fun deleteFolderLegacy(folder: File): Boolean {
        return folder.deleteRecursively().also {
            if (it) {
                Timber.tag("FolderAction").d("Folder deleted successfully")
            } else {
                Timber.tag("FolderAction").e("Failed to delete folder")
            }
        }
    }


    /**
     * Retrieves the path of the folder with the specified ID from the MediaStore.
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
        Timber.tag("FolderDeletion").d("Path is null for folder ID: $folderId")
        return null
    }


    fun printCurrentPermissions(context: Context) {
        val permissions = mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.ACCESS_MEDIA_LOCATION)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.ACCESS_MEDIA_LOCATION)
        } else {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        for (permission in permissions) {
            val status = if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                "GRANTED"
            } else {
                "DENIED"
            }
            Timber.tag("PermissionsStatus").d("$permission: $status")
        }
    }
}


