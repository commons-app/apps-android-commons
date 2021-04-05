/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.ui.camera

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.nguyenhoanglam.imagepicker.helper.ImageHelper
import com.nguyenhoanglam.imagepicker.helper.ImageHelper.grantAppPermission
import com.nguyenhoanglam.imagepicker.helper.ImageHelper.revokeAppPermission
import com.nguyenhoanglam.imagepicker.helper.LogHelper.Companion.instance
import com.nguyenhoanglam.imagepicker.model.Config
import java.io.File
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class DefaultCameraModule : CameraModule, Serializable {

    private var imageFilePath: String? = null
    private var imageUri: Uri? = null

    override fun getCameraIntent(context: Context, config: Config): Intent? {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val uri = createImageFileUri(context, config.rootDirectoryName, config.directoryName)
        instance?.d("Created image URI $uri")
        if (uri != null) {
            imageUri = uri
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            grantAppPermission(context, intent, uri)
            return intent
        }
        return null
    }

    private fun createImageFileUri(context: Context, rootDirectory: String, directory: String): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val fileName = "IMG_$timeStamp.jpg"
        var uri: Uri? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val filePath = "$rootDirectory/$directory"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.RELATIVE_PATH, filePath)
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            }
            if (rootDirectory == Config.ROOT_DIR_DCIM || rootDirectory == Config.ROOT_DIR_PICTURES) {
                uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            } else if (rootDirectory == Config.ROOT_DIR_DOWNLOAD) {
                uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            }
            imageFilePath = uri?.path
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(rootDirectory)
            val imageFile = File(dir, "/$directory/$fileName")
            val parentFile = imageFile.parentFile
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs()
            }
            val appContext = context.applicationContext
            val providerName = appContext.packageName + ".fileprovider"
            try {
                uri = FileProvider.getUriForFile(appContext, providerName, imageFile)
            } catch (e: Exception) {
                return null
            }
            imageFilePath = imageFile.absolutePath
        }
        return uri
    }

    override fun getImage(context: Context, isRequireId: Boolean, imageReadyListener: OnImageReadyListener?) {
        checkNotNull(imageReadyListener) { "OnImageReadyListener must not be null" }
        if (imageFilePath == null) {
            imageReadyListener.onImageNotReady()
            return
        }
        if (imageFilePath != null) {
            MediaScannerConnection.scanFile(context.applicationContext, arrayOf(imageFilePath!!), null) { path, uri ->
                if (path != null) {
                    val id = if (isRequireId && imageUri != null) (getImageIdFromURI(context, imageUri!!) ?: 0) else 0
                    imageReadyListener.onImageReady(ImageHelper.singleListFromPath(id, path))
                } else {
                    imageReadyListener.onImageNotReady()
                }
                revokeAppPermission(context, uri)
                imageFilePath = null
                imageUri = null
            }
        }
    }

    private fun getImageIdFromURI(context: Context, uri: Uri): Long? {
        var cursor: Cursor? = null
        return try {
            val projection = arrayOf(MediaStore.Images.Media._ID)
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.moveToFirst()
            cursor?.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID))
        } catch (e: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

}