package fr.free.nrw.commons.upload

import android.net.Uri
import android.webkit.URLUtil
import android.widget.ImageView
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import fr.free.nrw.commons.R
import timber.log.Timber
import java.io.File

internal fun ImageView.loadUploadItemImage(imageSource: String?) {
    if (imageSource.isNullOrBlank()) {
        setImageResource(R.drawable.ic_image_black_24dp)
        return
    }

    val data: Any = when {
        URLUtil.isHttpUrl(imageSource) || URLUtil.isHttpsUrl(imageSource) -> imageSource
        URLUtil.isFileUrl(imageSource) -> Uri.parse(imageSource)
        else -> File(imageSource)
    }

    load(data) {
        placeholder(R.drawable.ic_image_black_24dp)
        error(R.drawable.ic_image_black_24dp)
        listener(
            onError = { _, result ->
                Timber.e(result.throwable, "Unable to load upload item image: %s", imageSource)
            }
        )
    }
}
