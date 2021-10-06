package fr.free.nrw.commons.settings

import android.content.Context
import androidx.annotation.VisibleForTesting
import fr.free.nrw.commons.R
import fr.free.nrw.commons.settings.Licenses.Constants.CC0_ID
import fr.free.nrw.commons.settings.Licenses.Constants.CC0_TEMPLATE
import fr.free.nrw.commons.settings.Licenses.Constants.CC0_URL
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_3_ID
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_3_TEMPLATE
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_3_URL
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_4_ID
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_4_TEMPLATE
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_4_URL
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_SA_3_ID
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_SA_3_TEMPLATE
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_SA_3_URL
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_SA_4_ID
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_SA_4_TEMPLATE
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_SA_4_URL

sealed class Licenses(val id: String, val name: Int, val url: String, val template: String) {
    object CC_BY_3 : Licenses(CC_BY_3_ID, R.string.license_name_cc_by, CC_BY_3_URL, CC_BY_3_TEMPLATE)
    object CC_BY_4 : Licenses(CC_BY_4_ID, R.string.license_name_cc_by_four, CC_BY_4_URL, CC_BY_4_TEMPLATE)
    object CC_BY_SA_3 : Licenses(CC_BY_SA_3_ID, R.string.license_name_cc_by_sa, CC_BY_SA_3_URL, CC_BY_SA_3_TEMPLATE)
    object CC_BY_SA_4 : Licenses(CC_BY_SA_4_ID, R.string.license_name_cc_by_sa_four, CC_BY_SA_4_URL, CC_BY_SA_4_TEMPLATE)
    object CC0 : Licenses(CC0_ID, R.string.license_name_cc0, CC0_URL, CC0_TEMPLATE)

    companion object {
        fun findById(id: String?): Licenses = when (id) {
            CC_BY_3.id -> CC_BY_3
            CC_BY_4.id -> CC_BY_4
            CC_BY_SA_3.id -> CC_BY_SA_3
            CC_BY_SA_4.id -> CC_BY_SA_4
            CC0.id -> CC0
            else -> throw IllegalStateException("Unrecognized license value: $id")
        }

        fun mapByName(context: Context) = mapOf(
            context.getString(CC0.name) to CC0.id,
            context.getString(CC_BY_3.name) to CC_BY_3.id,
            context.getString(CC_BY_SA_3.name) to CC_BY_SA_3.id,
            context.getString(CC_BY_4.name) to CC_BY_4.id,
            context.getString(CC_BY_SA_4.name) to CC_BY_SA_4.id
        )

        fun names(context: Context) = asList().map { context.getString(it.name) }

        fun asList() = listOf(CC0, CC_BY_3, CC_BY_SA_3, CC_BY_4, CC_BY_SA_4)
    }

    @VisibleForTesting
    object Constants {
        const val CC_BY_3_ID = "CC BY 3.0"
        const val CC_BY_4_ID = "CC BY 4.0"
        const val CC_BY_SA_3_ID = "CC BY-SA 3.0"
        const val CC_BY_SA_4_ID = "CC BY-SA 4.0"
        const val CC0_ID = "CC0"

        const val CC_BY_3_URL = "https://creativecommons.org/licenses/by/3.0/"
        const val CC_BY_4_URL = "https://creativecommons.org/licenses/by/4.0/"
        const val CC_BY_SA_3_URL = "https://creativecommons.org/licenses/by-sa/3.0/"
        const val CC_BY_SA_4_URL = "https://creativecommons.org/licenses/by-sa/4.0/"
        const val CC0_URL = "https://creativecommons.org/publicdomain/zero/1.0/"

        const val CC_BY_3_TEMPLATE = "{{self|cc-by-3.0}}"
        const val CC_BY_4_TEMPLATE = "{{self|cc-by-4.0}}"
        const val CC_BY_SA_3_TEMPLATE = "{{self|cc-by-sa-3.0}}"
        const val CC_BY_SA_4_TEMPLATE = "{{self|cc-by-sa-4.0}}"
        const val CC0_TEMPLATE = "{{self|cc-zero}}"
    }
}



