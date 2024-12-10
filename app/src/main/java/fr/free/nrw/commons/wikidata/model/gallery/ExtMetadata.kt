package fr.free.nrw.commons.wikidata.model.gallery

import com.google.gson.annotations.SerializedName
import org.apache.commons.lang3.StringUtils

class ExtMetadata {
    @SerializedName("DateTime") private val dateTime: Values? = null
    @SerializedName("ObjectName") private val objectName: Values? = null
    @SerializedName("CommonsMetadataExtension") private val commonsMetadataExtension: Values? = null
    @SerializedName("Categories") private val categories: Values? = null
    @SerializedName("Assessments") private val assessments: Values? = null
    @SerializedName("GPSLatitude") private val gpsLatitude: Values? = null
    @SerializedName("GPSLongitude") private val gpsLongitude: Values? = null
    @SerializedName("ImageDescription") private val imageDescription: Values? = null
    @SerializedName("DateTimeOriginal") private val dateTimeOriginal: Values? = null
    @SerializedName("Artist") private val artist: Values? = null
    @SerializedName("Credit") private val credit: Values? = null
    @SerializedName("Permission") private val permission: Values? = null
    @SerializedName("AuthorCount") private val authorCount: Values? = null
    @SerializedName("LicenseShortName") private val licenseShortName: Values? = null
    @SerializedName("UsageTerms") private val usageTerms: Values? = null
    @SerializedName("LicenseUrl") private val licenseUrl: Values? = null
    @SerializedName("AttributionRequired") private val attributionRequired: Values? = null
    @SerializedName("Copyrighted") private val copyrighted: Values? = null
    @SerializedName("Restrictions") private val restrictions: Values? = null
    @SerializedName("License") private val license: Values? = null

    fun licenseShortName(): String = licenseShortName?.value ?: ""

    fun licenseUrl(): String = licenseUrl?.value ?: ""

    fun license(): String = license?.value ?: ""

    fun imageDescription(): String = imageDescription?.value ?: ""

    fun imageDescriptionSource(): String = imageDescription?.source ?: ""

    fun objectName(): String = objectName?.value ?: ""

    fun usageTerms(): String = usageTerms?.value ?: ""

    fun dateTimeOriginal(): String = dateTimeOriginal?.value ?: ""

    fun dateTime(): String = dateTime?.value ?: ""

    fun artist(): String = artist?.value ?: ""

    fun categories(): String = categories?.value ?: ""

    fun gpsLatitude(): String = gpsLatitude?.value ?: ""

    fun gpsLongitude(): String = gpsLongitude?.value ?: ""

    fun credit(): String = credit?.value ?: ""

    class Values {
        val value: String? = null
        val source: String? = null
        val hidden: String? = null
    }
}
