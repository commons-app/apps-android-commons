package fr.free.nrw.commons.utils

import fr.free.nrw.commons.R
import fr.free.nrw.commons.settings.Prefs

/**
 * Generates licence name with given ID
 * @return Name of license
 */
fun String.toLicenseName(): Int = when (this) {
    Prefs.Licenses.CC_BY_3 -> R.string.license_name_cc_by
    Prefs.Licenses.CC_BY_4 -> R.string.license_name_cc_by_four
    Prefs.Licenses.CC_BY_SA_3 -> R.string.license_name_cc_by_sa
    Prefs.Licenses.CC_BY_SA_4 -> R.string.license_name_cc_by_sa_four
    Prefs.Licenses.CC0 -> R.string.license_name_cc0
    else -> throw IllegalStateException("Unrecognized license value: $this")
}

/**
 * Generates license url with given ID
 * @return Url of license
 */
fun String.toLicenseUrl(): String = when (this) {
    Prefs.Licenses.CC_BY_3 -> "https://creativecommons.org/licenses/by/3.0/"
    Prefs.Licenses.CC_BY_4 -> "https://creativecommons.org/licenses/by/4.0/"
    Prefs.Licenses.CC_BY_SA_3 -> "https://creativecommons.org/licenses/by-sa/3.0/"
    Prefs.Licenses.CC_BY_SA_4 -> "https://creativecommons.org/licenses/by-sa/4.0/"
    Prefs.Licenses.CC0 -> "https://creativecommons.org/publicdomain/zero/1.0/"
    else -> throw IllegalStateException("Unrecognized license value: $this")
}

