package fr.free.nrw.commons.wikidata.model.page

import android.location.Location
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import org.json.JSONException
import org.json.JSONObject
import java.util.Date

/**
 * Immutable class that contains metadata associated with a PageTitle.
 */
class PageProperties private constructor(parcel: Parcel) : Parcelable {
    val pageId: Int = parcel.readInt()
    private val namespace = Namespace.of(parcel.readInt())
    private val revisionId = parcel.readLong()
    private val lastModified = Date(parcel.readLong())
    private val displayTitleText = parcel.readString()
    private val editProtectionStatus = parcel.readString()
    private val languageCount = parcel.readInt()
    val isMainPage: Boolean = parcel.readInt() == 1
    val isDisambiguationPage: Boolean = parcel.readInt() == 1

    /** Nullable URL with no scheme. For example, foo.bar.com/ instead of http://foo.bar.com/.  */
    private val leadImageUrl = parcel.readString()
    private val leadImageName = parcel.readString()
    private val titlePronunciationUrl = parcel.readString()
    private val geo = unmarshal(parcel.readString())
    private val wikiBaseItem = parcel.readString()
    private val descriptionSource = parcel.readString()

    /**
     * True if the user who first requested this page can edit this page
     * FIXME: This is not a true page property, since it depends on current user.
     */
    private val canEdit = parcel.readInt() == 1

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(pageId)
        parcel.writeInt(namespace.code())
        parcel.writeLong(revisionId)
        parcel.writeLong(lastModified.time)
        parcel.writeString(displayTitleText)
        parcel.writeString(titlePronunciationUrl)
        parcel.writeString(marshal(geo))
        parcel.writeString(editProtectionStatus)
        parcel.writeInt(languageCount)
        parcel.writeInt(if (canEdit) 1 else 0)
        parcel.writeInt(if (isMainPage) 1 else 0)
        parcel.writeInt(if (isDisambiguationPage) 1 else 0)
        parcel.writeString(leadImageUrl)
        parcel.writeString(leadImageName)
        parcel.writeString(wikiBaseItem)
        parcel.writeString(descriptionSource)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }

        val that = o as PageProperties

        return pageId == that.pageId &&
                namespace === that.namespace &&
                revisionId == that.revisionId &&
                lastModified == that.lastModified &&
                displayTitleText == that.displayTitleText &&
                TextUtils.equals(titlePronunciationUrl, that.titlePronunciationUrl) &&
                (geo === that.geo || geo != null && geo == that.geo) &&
                languageCount == that.languageCount &&
                canEdit == that.canEdit &&
                isMainPage == that.isMainPage &&
                isDisambiguationPage == that.isDisambiguationPage &&
                TextUtils.equals(editProtectionStatus, that.editProtectionStatus) &&
                TextUtils.equals(leadImageUrl, that.leadImageUrl) &&
                TextUtils.equals(leadImageName, that.leadImageName) &&
                TextUtils.equals(wikiBaseItem, that.wikiBaseItem)
    }

    override fun hashCode(): Int {
        var result = lastModified.hashCode()
        result = 31 * result + displayTitleText.hashCode()
        result = 31 * result + (titlePronunciationUrl?.hashCode() ?: 0)
        result = 31 * result + (geo?.hashCode() ?: 0)
        result = 31 * result + (editProtectionStatus?.hashCode() ?: 0)
        result = 31 * result + languageCount
        result = 31 * result + (if (isMainPage) 1 else 0)
        result = 31 * result + (if (isDisambiguationPage) 1 else 0)
        result = 31 * result + (leadImageUrl?.hashCode() ?: 0)
        result = 31 * result + (leadImageName?.hashCode() ?: 0)
        result = 31 * result + (wikiBaseItem?.hashCode() ?: 0)
        result = 31 * result + (if (canEdit) 1 else 0)
        result = 31 * result + pageId
        result = 31 * result + namespace.code()
        result = 31 * result + revisionId.toInt()
        return result
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PageProperties> = object : Parcelable.Creator<PageProperties> {
                override fun createFromParcel(parcel: Parcel): PageProperties {
                    return PageProperties(parcel)
                }

                override fun newArray(size: Int): Array<PageProperties?> {
                    return arrayOfNulls(size)
                }
            }
    }
}

private const val LATITUDE: String = "latitude"
private const val LONGITUDE: String = "longitude"

private fun marshal(location: Location?): String? {
    if (location == null) {
        return null
    }

    val jsonObj = JSONObject().apply {
        try {
            put(LATITUDE, location.latitude)
            put(LONGITUDE, location.longitude)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
    }

    return jsonObj.toString()
}

private fun unmarshal(json: String?): Location? {
    if (json == null) {
        return null
    }

    return try {
        val jsonObject = JSONObject(json)
        Location(null as String?).apply {
            latitude = jsonObject.optDouble(LATITUDE)
            longitude = jsonObject.optDouble(LONGITUDE)
        }
    } catch (e: JSONException) {
        null
    }
}