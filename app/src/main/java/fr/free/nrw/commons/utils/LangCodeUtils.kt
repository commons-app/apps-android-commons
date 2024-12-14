package fr.free.nrw.commons.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/**
 * Utilities class for miscellaneous strings
 */
object LangCodeUtils {

    /**
     * Replaces the deprecated ISO-639 language codes used by Android with the updated ISO-639-1.
     * @param code Language code you want to update.
     * @return Updated language code. If not in the "deprecated list" returns the same code.
     */
    @JvmStatic
    fun fixLanguageCode(code: String): String {
        return when (code.lowercase()) {
            "iw" -> "he"
            "in" -> "id"
            "ji" -> "yi"
            else -> code
        }
    }

    /**
     * Returns configuration for locale of
     * our choice regardless of user's device settings
     */
    @JvmStatic
    fun getLocalizedResources(context: Context, desiredLocale: Locale): Resources {
        val conf = Configuration(context.resources.configuration).apply {
            setLocale(desiredLocale)
        }
        val localizedContext = context.createConfigurationContext(conf)
        return localizedContext.resources
    }
}
