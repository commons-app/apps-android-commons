package fr.free.nrw.commons.fileusages

import android.net.Uri
import timber.log.Timber

/**
 * Data model for displaying file usage information in the UI, including the title and link to the page.
 */
data class FileUsagesUiModel(
    val title: String,
    val link: String?
)

/**
 * Converts a FileUsage object to a UI model for Commons file usages.
 * Creates a link to the file's page on Commons.
 */
fun FileUsage.toUiModel(): FileUsagesUiModel {
    // Replace spaces with underscores and URL-encode the title for the link
    val encodedTitle = Uri.encode(title.replace(" ", "_"))
    return FileUsagesUiModel(
        title = title,
        link = "https://commons.wikimedia.org/wiki/$encodedTitle"
    )
}

/**
 * Converts a GlobalFileUsage object to a UI model for file usages on other wikis.
 * Generates a link to the page and prefixes the title with the wiki code (e.g., "(en) Title").
 */
fun GlobalFileUsage.toUiModel(): FileUsagesUiModel {
    // Log input values for debugging
    Timber.d("Converting GlobalFileUsage: wiki=$wiki, title=$title")

    // Check for invalid or empty inputs
    if (wiki.isBlank() || title.isBlank()) {
        Timber.w("Invalid input: wiki=$wiki, title=$title")
        return FileUsagesUiModel(title = title, link = null)
    }

    // Extract wiki code for prefix (e.g., "en" from "en.wikipedia.org" or "enwiki")
    val wikiCode = when {
        wiki.contains(".") -> wiki.substringBefore(".") // e.g., "en" from "en.wikipedia.org"
        wiki == "commonswiki" -> "commons"
        wiki.endsWith("wiki") -> wiki.removeSuffix("wiki")
        else -> wiki
    }

    // Create prefixed title, e.g., "(en) Changi East Depot"
    val prefixedTitle = "($wikiCode) $title"

    // Determine the domain for the URL
    val domain = when {
        wiki.contains(".") -> wiki // Already a full domain, e.g., "en.wikipedia.org"
        wiki == "commonswiki" -> "commons.wikimedia.org"
        wiki.endsWith("wiki") -> wiki.removeSuffix("wiki") + ".wikipedia.org"
        else -> "$wiki.wikipedia.org" // Fallback for simple codes like "en"
    }

    // Normalize title: replace spaces with underscores and URL-encode
    val encodedTitle = Uri.encode(title.replace(" ", "_"))

    // Build the full URL
    val url = "https://$domain/wiki/$encodedTitle"
    Timber.d("Generated URL: $url")

    return FileUsagesUiModel(title = prefixedTitle, link = url)
}