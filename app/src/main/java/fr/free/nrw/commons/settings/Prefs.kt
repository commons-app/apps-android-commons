package fr.free.nrw.commons.settings

object Prefs {
    const val GLOBAL_PREFS = "fr.free.nrw.commons.preferences"

    const val TRACKING_ENABLED = "eventLogging"
    const val DEFAULT_LICENSE = "defaultLicense"
    const val UPLOADS_SHOWING = "uploadsShowing"
    const val MANAGED_EXIF_TAGS = "managed_exif_tags"
    const val DESCRIPTION_LANGUAGE = "languageDescription"
    const val APP_UI_LANGUAGE = "appUiLanguage"
    const val KEY_THEME_VALUE = "appThemePref"

    object Licenses {
        const val CC_BY_SA_3 = "CC BY-SA 3.0"
        const val CC_BY_3 = "CC BY 3.0"
        const val CC_BY_SA_4 = "CC BY-SA 4.0"
        const val CC_BY_4 = "CC BY 4.0"
        const val CC0 = "CC0"
    }
}
