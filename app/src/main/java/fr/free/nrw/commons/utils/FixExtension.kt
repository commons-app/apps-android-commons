package fr.free.nrw.commons.utils

import java.util.Locale
import java.util.regex.Pattern

private val jpegPattern = Pattern.compile("\\.jpeg$", Pattern.CASE_INSENSITIVE)

/**
 * Adds extension to filename. Converts to .jpg if system provides .jpeg, adds .jpg if no extension detected
 * @param theTitle File name
 * @param ext Correct extension
 * @return File with correct extension
 */
fun fixExtension(theTitle: String, ext: String?): String {
    var result = theTitle
    var extension = ext

    // People are used to ".jpg" more than ".jpeg" which the system gives us.
    if (extension != null && extension.lowercase() == "jpeg") {
        extension = "jpg"
    }

    result = jpegPattern.matcher(result).replaceFirst(".jpg")
    if (extension != null &&
        !result.lowercase(Locale.getDefault()).endsWith("." + extension.lowercase())
    ) {
        result += ".$extension"
    }

    // If extension is still null, make it jpg. (Hotfix for https://github.com/commons-app/apps-android-commons/issues/228)
    // If title has an extension in it, if won't be true
    if (extension == null && result.lastIndexOf(".") <= 0) {
        extension = "jpg"
        result += ".$extension"
    }

    return result
}
