package fr.free.nrw.commons.wikidata.mwapi

import androidx.annotation.VisibleForTesting
import com.google.gson.annotations.SerializedName
import fr.free.nrw.commons.wikidata.model.BaseModel
import fr.free.nrw.commons.wikidata.model.gallery.ImageInfo
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage.GlobalUsage

/**
 * A class representing a standard page object as returned by the MediaWiki API.
 */
class MwQueryPage : BaseModel() {
    private val pageid = 0
    private val index = 0
    private var title: String? = null
    private val categoryinfo: CategoryInfo? = null
    private val revisions: List<Revision>? = null

    @SerializedName("fileusage")
    private val fileUsages: List<FileUsage>? = null

    @SerializedName("globalusage")
    private val globalUsages: List<GlobalUsage>? = null
    private val coordinates: MutableList<Coordinates?>? = null
    private val categories: List<Category>? = null
    private val thumbnail: Thumbnail? = null
    private val description: String? = null

    @SerializedName("imageinfo")
    private val imageInfo: List<ImageInfo>? = null
    private var redirectFrom: String? = null
    private var convertedFrom: String? = null
    private var convertedTo: String? = null

    fun title(): String = title!!

    fun categoryInfo(): CategoryInfo? = categoryinfo

    fun index(): Int = index

    fun revisions(): List<Revision>? = revisions

    fun categories(): List<Category>? = categories

    // TODO: Handle null values in lists during deserialization, perhaps with a new
    // @RequiredElements annotation and corresponding TypeAdapter
    fun coordinates(): List<Coordinates?>? = coordinates?.filterNotNull()

    fun pageId(): Int = pageid

    fun thumbUrl(): String? = thumbnail?.source()

    fun description(): String? = description

    fun imageInfo(): ImageInfo? = imageInfo?.get(0)

    fun redirectFrom(from: String?) {
        redirectFrom = from
    }

    fun convertedFrom(from: String?) {
        convertedFrom = from
    }

    fun convertedTo(to: String?) {
        convertedTo = to
    }

    fun appendTitleFragment(fragment: String?) {
        title += "#$fragment"
    }

    fun checkWhetherFileIsUsedInWikis(): Boolean {
        return checkWhetherFileIsUsedInWikis(globalUsages, fileUsages)
    }

    class Revision {
        @SerializedName("revid")
        private val revisionId: Long = 0
        private val user: String? = null
        @SerializedName("contentformat")
        private val contentFormat: String? = null
        @SerializedName("contentmodel")
        private val contentModel: String? = null
        @SerializedName("timestamp")
        private val timeStamp: String? = null
        private val content: String? = null

        fun revisionId(): Long = revisionId

        fun user(): String = user ?: ""

        fun content(): String = content!!

        fun timeStamp(): String = timeStamp ?: ""
    }

    class Coordinates {
        private val lat: Double? = null
        private val lon: Double? = null

        fun lat(): Double? = lat

        fun lon(): Double? = lon
    }

    class CategoryInfo {
        val isHidden: Boolean = false
        private val size = 0
        private val pages = 0
        private val files = 0
        private val subcats = 0
    }

    internal class Thumbnail {
        private val source: String? = null
        private val width = 0
        private val height = 0

        fun source(): String? = source
    }

    class GlobalUsage {
        @SerializedName("title")
        val title: String? = null

        @SerializedName("wiki")
        val wiki: String? = null

        @SerializedName("url")
        val url: String? = null
    }

    class FileUsage {
        @SerializedName("pageid")
        private val pageid = 0

        @SerializedName("ns")
        private val ns = 0

        @SerializedName("title")
        private var title: String? = null

        fun pageId(): Int = pageid

        fun ns(): Int = ns

        fun title(): String = title ?: ""

        fun setTitle(value: String) {
            title = value
        }
    }

    class Category {
        private val ns = 0
        private val title: String? = null
        private val hidden = false

        fun ns(): Int = ns

        fun title(): String = title ?: ""

        fun hidden(): Boolean = hidden
    }
}

@VisibleForTesting
fun checkWhetherFileIsUsedInWikis(
    globalUsages: List<GlobalUsage>?,
    fileUsages: List<MwQueryPage.FileUsage>?
): Boolean {
    if (!globalUsages.isNullOrEmpty()) {
        return true
    }

    if (fileUsages.isNullOrEmpty()) {
        return false
    }

    /* Ignore usage under https://commons.wikimedia.org/wiki/User:Didym/Mobile_upload/
       which has been a gallery of all of our uploads since 2014 */
    return fileUsages.filterNot {
        it.title().contains("User:Didym/Mobile upload")
    }.isNotEmpty()
}
