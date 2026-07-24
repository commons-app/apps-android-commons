package fr.free.nrw.commons.upload

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.URLUtil
import android.widget.ImageView
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import timber.log.Timber
import java.io.File
import kotlin.math.max

internal fun ImageView.loadUploadItemImage(
    imageSource: String?,
    placeholderResId: Int,
) {
    val data: Any? =
        imageSource
            ?.takeUnless { it.isBlank() }
            ?.let {
                val uri = Uri.parse(it)
                when {
                    URLUtil.isHttpUrl(it) || URLUtil.isHttpsUrl(it) -> it
                    !uri.scheme.isNullOrBlank() -> uri
                    else -> File(it)
                }
            }

    try {
        load(data) {
            placeholder(placeholderResId)
            error(placeholderResId)
            listener(
                onError = { _, result ->
                    if (!tryLoadDownsampledImage(imageSource, placeholderResId, result.throwable)) {
                        Timber.e(result.throwable, "Unable to load upload item image: %s", imageSource)
                    }
                }
            )
        }
    } catch (error: Exception) {
        Timber.e(error, "Unable to start upload item image load: %s", imageSource)
        setImageResource(placeholderResId)
    }
}

private fun ImageView.tryLoadDownsampledImage(
    imageSource: String?,
    placeholderResId: Int,
    error: Throwable?,
): Boolean {
    if (imageSource.isNullOrBlank() || !isOutOfMemory(error)) {
        return false
    }

    return try {
        val downsampledBitmap =
            decodeDownsampledBitmap(
                imageSource,
                requestedWidth = measuredWidth.takeIf { it > 0 } ?: width,
                requestedHeight = measuredHeight.takeIf { it > 0 } ?: height
            ) ?: return false

        load(downsampledBitmap) {
            placeholder(placeholderResId)
            error(placeholderResId)
            listener(
                onError = { _, result ->
                    Timber.e(result.throwable, "Unable to load downsampled upload item image: %s", imageSource)
                    setImageResource(placeholderResId)
                }
            )
        }
        true
    } catch (oom: OutOfMemoryError) {
        Timber.e(oom, "Out of memory while downsampling upload item image: %s", imageSource)
        setImageResource(placeholderResId)
        true
    } catch (exception: Exception) {
        Timber.e(exception, "Unable to downsample upload item image: %s", imageSource)
        setImageResource(placeholderResId)
        true
    }
}

private fun ImageView.decodeDownsampledBitmap(
    imageSource: String,
    requestedWidth: Int,
    requestedHeight: Int,
): Bitmap? {
    val targetWidth = requestedWidth.takeIf { it > 0 } ?: DEFAULT_DOWNSAMPLED_IMAGE_SIZE_PX
    val targetHeight = requestedHeight.takeIf { it > 0 } ?: DEFAULT_DOWNSAMPLED_IMAGE_SIZE_PX
    val uri = Uri.parse(imageSource)

    return when {
        !uri.scheme.isNullOrBlank() && !URLUtil.isHttpUrl(imageSource) && !URLUtil.isHttpsUrl(imageSource) ->
            decodeDownsampledBitmapFromUri(uri, targetWidth, targetHeight)

        else -> decodeDownsampledBitmapFromFile(File(imageSource), targetWidth, targetHeight)
    }
}

private fun ImageView.decodeDownsampledBitmapFromUri(
    uri: Uri,
    requestedWidth: Int,
    requestedHeight: Int,
): Bitmap? {
    val bounds =
        BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    } ?: return null

    val options =
        BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds, requestedWidth, requestedHeight)
        }
    return context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    }
}

private fun decodeDownsampledBitmapFromFile(
    file: File,
    requestedWidth: Int,
    requestedHeight: Int,
): Bitmap? {
    if (!file.exists()) {
        return null
    }

    val bounds =
        BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
    BitmapFactory.decodeFile(file.path, bounds)

    val options =
        BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds, requestedWidth, requestedHeight)
        }
    return BitmapFactory.decodeFile(file.path, options)
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    requestedWidth: Int,
    requestedHeight: Int,
): Int {
    val safeRequestedWidth = max(requestedWidth, 1)
    val safeRequestedHeight = max(requestedHeight, 1)
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > safeRequestedHeight || width > safeRequestedWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        while (halfHeight / inSampleSize >= safeRequestedHeight &&
            halfWidth / inSampleSize >= safeRequestedWidth
        ) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

private fun isOutOfMemory(error: Throwable?): Boolean {
    var current = error
    while (current != null) {
        if (current is OutOfMemoryError) {
            return true
        }
        current = current.cause
    }
    return false
}

private const val DEFAULT_DOWNSAMPLED_IMAGE_SIZE_PX = 1024
