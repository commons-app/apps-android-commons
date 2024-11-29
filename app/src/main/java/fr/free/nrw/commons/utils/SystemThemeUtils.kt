package fr.free.nrw.commons.utils

import android.content.Context
import android.content.res.Configuration

import javax.inject.Inject
import javax.inject.Named

import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.settings.Prefs


class SystemThemeUtils @Inject constructor(
    private val context: Context,
    @Named("default_preferences") private val applicationKvStore: JsonKvStore
) {

    companion object {
        const val THEME_MODE_DEFAULT = "0"
        const val THEME_MODE_DARK = "1"
        const val THEME_MODE_LIGHT = "2"
    }

    // Return true if system wide dark theme is enabled else false
    private fun getSystemDefaultThemeBool(theme: String): Boolean {
        return when (theme) {
            THEME_MODE_DARK -> true
            THEME_MODE_DEFAULT -> getSystemDefaultThemeBool(getSystemDefaultTheme())
            else -> false
        }
    }

    // Returns the default system wide theme
    private fun getSystemDefaultTheme(): String {
        return if (
            (
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES
            ) {
            THEME_MODE_DARK
        } else {
            THEME_MODE_LIGHT
        }
    }

    // Returns true if the device is in night mode or false otherwise
    fun isDeviceInNightMode(): Boolean {
        return getSystemDefaultThemeBool(
            applicationKvStore.getString(Prefs.KEY_THEME_VALUE, getSystemDefaultTheme())!!
        )
    }
}
