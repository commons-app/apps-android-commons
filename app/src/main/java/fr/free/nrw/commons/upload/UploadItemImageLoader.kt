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
    } catch (error: OutOfMemoryError) {
        Timber.e(error, "Out of memory while preparing upload item image load: %s", imageSource)
        setImageResource(placeholderResId)
    } catch (error: Exception) {
        Timber.e(error, "Unable to start upload item image load: %s", imageSource)
        setImageResource(placeholderResId)
    }
}
