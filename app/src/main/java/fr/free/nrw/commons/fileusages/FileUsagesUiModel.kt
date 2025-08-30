package fr.free.nrw.commons.fileusages

import android.net.Uri
import timber.log.Timber

/**
 * shows where file is being used on Commons and other wikis.
 */
data class FileUsagesUiModel(
    val title: String,
    val link: String?
)

fun FileUsage.toUiModel(): FileUsagesUiModel {
    return FileUsagesUiModel(
        title = title,
        link = "https://commons.wikimedia.org/wiki/${Uri.encode(title.replace(" ", "_"))}"
    )
}

fun GlobalFileUsage.toUiModel(): FileUsagesUiModel {
    Timber.d("GlobalFileUsage: wiki=%s, title=%s", wiki, title)

    // handles the  empty or invalid wiki/title
    if (wiki.isEmpty() || title.isEmpty()) {
        Timber.w("Invalid GlobalFileUsage: wiki=%s, title=%s", wiki, title)
        return FileUsagesUiModel(title = title, link = null)
    }

    // determines the domain
    val domain = when {
        wiki.contains(".") -> wiki // Already a full domain like "en.wikipedia.org"
        wiki == "commonswiki" -> "commons.wikimedia.org"
        wiki.endsWith("wiki") -> {
            val code = wiki.removeSuffix("wiki")
            "$code.wikipedia.org"
        }
        else -> "$wiki.wikipedia.org" // fallback for codes like "en"
    }

    val normalizedTitle = Uri.encode(title.replace(" ", "_"))

    // construct full URL
    val url = "https://$domain/wiki/$normalizedTitle"
    Timber.d("Generated URL for GlobalFileUsage: %s", url)

    return FileUsagesUiModel(title = title, link = url)
}