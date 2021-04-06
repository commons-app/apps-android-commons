/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.helper

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import com.nguyenhoanglam.imagepicker.model.Folder
import com.nguyenhoanglam.imagepicker.model.Image
import java.io.File

object ImageHelper {

    private fun getNameFromFilePath(path: String): String {
        return if (path.contains(File.separator)) {
            path.substring(path.lastIndexOf(File.separator) + 1)
        } else path
    }

    fun grantAppPermission(context: Context, intent: Intent, fileUri: Uri) {
        val resolvedIntentActivities = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolvedIntentInfo in resolvedIntentActivities) {
            val packageName = resolvedIntentInfo.activityInfo.packageName
            context.grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun revokeAppPermission(context: Context, fileUri: Uri) {
        context.revokeUriPermission(fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun singleListFromPath(id: Long, path: String): ArrayList<Image> {
        val images = arrayListOf<Image>()
        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        images.add(Image(id, getNameFromFilePath(path), uri, path))
        return images
    }

    fun singleListFromImage(image: Image): ArrayList<Image> {
        val images = arrayListOf<Image>()
        images.add(image)
        return images
    }

    fun folderListFromImages(images: List<Image>): List<Folder> {
        val folderMap: MutableMap<Long, Folder> = LinkedHashMap()
        for (image in images) {
            val bucketId = image.bucketId
            val bucketName = image.bucketName
            var folder = folderMap[bucketId]
            if (folder == null) {
                folder = Folder(bucketId, bucketName)
                folderMap[bucketId] = folder
            }
            folder.images.add(image)
        }
        return ArrayList(folderMap.values)
    }

    fun filterImages(images: ArrayList<Image>, bukketId: Long?): ArrayList<Image> {
        if (bukketId == null) return images

        val filteredImages = arrayListOf<Image>()
        for (image in images) {
            if (image.bucketId == bukketId) {
                filteredImages.add(image)
            }
        }
        return filteredImages
    }

    fun findImageIndex(image: Image, images: ArrayList<Image>): Int {
        for (i in images.indices) {
            if (images[i].path == image.path) {
                return i
            }
        }
        return -1
    }

    fun findImageIndexes(subImages: ArrayList<Image>, images: ArrayList<Image>): ArrayList<Int> {
        val indexes = arrayListOf<Int>()
        for (image in subImages) {
            for (i in images.indices) {
                if (images[i].path == image.path) {
                    indexes.add(i)
                    break
                }
            }
        }
        return indexes
    }


    fun isGifFormat(image: Image): Boolean {
        val extension = image.path.substring(image.path.lastIndexOf(".") + 1, image.path.length)
        return extension.equals("gif", ignoreCase = true)
    }
}