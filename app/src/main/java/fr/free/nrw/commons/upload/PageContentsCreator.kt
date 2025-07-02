package fr.free.nrw.commons.upload

import android.content.Context
import fr.free.nrw.commons.utils.Utils
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.filepicker.UploadableFile.DateTimeWithSource
import fr.free.nrw.commons.settings.Prefs.Licenses
import fr.free.nrw.commons.utils.ConfigUtils.getVersionNameWithSha
import org.apache.commons.lang3.StringUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class PageContentsCreator @Inject constructor(private val context: Context) {
    fun createFrom(contribution: Contribution?): String = buildString {
        val media = contribution?.media
        append("== {{int:filedesc}} ==\n")
        append("{{Information\n")
        append("|description=").append(media?.fallbackDescription).append("\n")
        if (contribution?.wikidataPlace != null) {
            append("{{ on Wikidata|").append(contribution.wikidataPlace!!.id)
            append("}}")
        }
        append("|source=").append("{{own}}\n")
        append("|author=[[User:").append(media?.author).append("|")
        append(media?.author).append("]]\n")

        val templatizedCreatedDate = getTemplatizedCreatedDate(
            contribution?.dateCreatedString,
            contribution?.dateCreated,
            contribution?.dateCreatedSource
        )
        if (!StringUtils.isBlank(templatizedCreatedDate)) {
            append("|date=").append(templatizedCreatedDate)
        }

        append("}}").append("\n")

        //Only add Location template (e.g. {{Location|37.51136|-77.602615}} ) if coords is not null
        val decimalCoords = contribution?.decimalCoords
        if (decimalCoords != null) {
            append("{{Location|").append(decimalCoords).append("}}").append("\n")
        }

        if (contribution?.wikidataPlace != null && contribution.wikidataPlace!!.isMonumentUpload) {
            append(
                String.format(
                    Locale.ENGLISH,
                    "{{Wiki Loves Monuments %d|1= %s}}\n",
                    Utils.getWikiLovesMonumentsYear(Calendar.getInstance()),
                    contribution.countryCode
                )
            )
        }

        append("\n")
        append("== {{int:license-header}} ==\n")
        append(licenseTemplateFor(media?.license!!)).append("\n\n")
        append("{{Uploaded from Mobile|platform=Android|version=")
        append(context.getVersionNameWithSha()).append("}}\n")
        val categories = media.categories
        if (!categories.isNullOrEmpty()) {
            categories.indices.forEach {
                append("\n[[Category:").append(categories[it]).append("]]")
            }
        } else {
            append("{{subst:unc}}")
        }
    }

    /**
     * Returns upload date in either TEMPLATE_DATE_ACC_TO_EXIF or TEMPLATE_DATA_OTHER_SOURCE
     */
    private fun getTemplatizedCreatedDate(
        dateCreatedString: String?, dateCreated: Date?, dateCreatedSource: String?
    ) = dateCreated?.let {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        String.format(
            Locale.ENGLISH,
            if (isExif(dateCreatedSource)) TEMPLATE_DATE_ACC_TO_EXIF else TEMPLATE_DATA_OTHER_SOURCE,
            if (isExif(dateCreatedSource)) dateCreatedString else dateFormat.format(dateCreated)
        ) + "\n"
    } ?: ""

    private fun isExif(dateCreatedSource: String?): Boolean =
        DateTimeWithSource.EXIF_SOURCE == dateCreatedSource

    private fun licenseTemplateFor(license: String) = when (license) {
        Licenses.CC_BY_3 -> "{{self|cc-by-3.0}}"
        Licenses.CC_BY_4 -> "{{self|cc-by-4.0}}"
        Licenses.CC_BY_SA_3 -> "{{self|cc-by-sa-3.0}}"
        Licenses.CC_BY_SA_4 -> "{{self|cc-by-sa-4.0}}"
        Licenses.CC0 -> "{{self|cc-zero}}"
        else -> throw RuntimeException("Unrecognized license value: $license")
    }

    companion object {
        //{{According to Exif data|2009-01-09}}
        private const val TEMPLATE_DATE_ACC_TO_EXIF = "{{According to Exif data|%s}}"

        //2009-01-09 → 9 January 2009
        private const val TEMPLATE_DATA_OTHER_SOURCE = "%s"
    }
}
