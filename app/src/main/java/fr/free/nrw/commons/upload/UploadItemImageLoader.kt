package fr.free.nrw.commons.upload

import android.net.Uri
import android.webkit.URLUtil
import android.widget.ImageView
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import timber.log.Timber
import java.io.File

internal fun ImageView.loadUploadItemImage(
    imageSource: String?,
    placeholderResId: Int,
) {
    val data: Any? =
        imageSource
            ?.takeUnless { it.isBlank() }
            ?.let {
                when {
                    URLUtil.isHttpUrl(it) || URLUtil.isHttpsUrl(it) -> it
                    URLUtil.isFileUrl(it) -> Uri.parse(it)
                    else -> File(it)
                }
            }

    try {
        load(data) {
            placeholder(placeholderResId)
            error(placeholderResId)
            listener(
                onError = { _, result ->
                    Timber.e(result.throwable, "Unable to load upload item image: %s", imageSource)
                }
            )
        }
    } catch (outOfMemoryError: OutOfMemoryError) {
        Timber.e(outOfMemoryError, "Out of memory while loading upload item image: %s", imageSource)
        setImageResource(placeholderResId)
    } catch (exception: Exception) {
        Timber.e(exception, "Unable to start loading upload item image: %s", imageSource)
        setImageResource(placeholderResId)
    }
}
