package fr.free.nrw.commons.utils

import android.content.Context
import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import timber.log.Timber
import java.util.IllegalFormatException
import java.util.Locale

/**
 * translatewiki overwrites values-strings.xml, so a broken placeholder has to be
 * handled here. A bad translation falls back to the English string instead of crashing.
 */
fun Context.getStringSafe(
    @StringRes id: Int,
    vararg formatArgs: Any?,
): String =
    formatOrEnglish(
        localized = { getString(id, *formatArgs) },
        english = {
            LangCodeUtils.getLocalizedResources(this, Locale.ENGLISH).getString(id, *formatArgs)
        },
        raw = { getString(id) },
        name = { resourceName(id) },
    )

fun Context.getQuantityStringSafe(
    @PluralsRes id: Int,
    quantity: Int,
    vararg formatArgs: Any?,
): String =
    formatOrEnglish(
        localized = { resources.getQuantityString(id, quantity, *formatArgs) },
        english = {
            LangCodeUtils
                .getLocalizedResources(this, Locale.ENGLISH)
                .getQuantityString(id, quantity, *formatArgs)
        },
        raw = { resources.getQuantityString(id, quantity) },
        name = { resourceName(id) },
    )

private fun formatOrEnglish(
    localized: () -> String,
    english: () -> String,
    raw: () -> String,
    name: () -> String,
): String =
    try {
        localized()
    } catch (error: IllegalFormatException) {
        val resourceName = name()
        Timber.e(error, "Unparseable placeholder in %s", resourceName)
        try {
            english()
        } catch (englishError: IllegalFormatException) {
            Timber.e(englishError, "English string %s is also unparseable", resourceName)
            raw()
        }
    }

private fun Context.resourceName(id: Int): String {
    val currentResources = resources ?: return id.toString()
    return try {
        currentResources.getResourceEntryName(id)
    } catch (error: Resources.NotFoundException) {
        Timber.e(error, "Missing resource name for %s", id)
        id.toString()
    }
}
