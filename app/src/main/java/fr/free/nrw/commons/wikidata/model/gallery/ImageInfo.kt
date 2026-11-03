package fr.free.nrw.commons.wikidata.model.gallery

import com.google.gson.annotations.SerializedName

/**
 * Gson POJO for a standard image info object as returned by the API ImageInfo module
 */
@Suppress("UnusedPrivateProperty") // Fields deserialized by Gson via reflection
open class ImageInfo {
    private val size = 0
    private val width = 0
    private val height = 0
    private var source: String? = null

    @SerializedName("thumburl")
    private var thumbUrl: String? = null

    @SerializedName("thumbwidth")
    private var thumbWidth = 0

    @SerializedName("thumbheight")
    private var thumbHeight = 0

    @SerializedName("url")
    private val originalUrl: String? = null

    @SerializedName("descriptionurl")
    private val descriptionUrl: String? = null

    @SerializedName("descriptionshorturl")
    private val descriptionShortUrl: String? = null

    @SerializedName("mime")
    private val mimeType: String? = null

    @SerializedName("extmetadata")
    private val metadata: ExtMetadata? = null
    private val user: String? = null
    private val timestamp: String? = null

    fun getSource(): String = source ?: ""

    fun setSource(source: String?) {
        this.source = source
    }

    fun getSize(): Int = size

    fun getWidth(): Int = width

    fun getHeight(): Int = height

    fun getThumbWidth(): Int = thumbWidth

    fun getThumbHeight(): Int = thumbHeight

    fun getMimeType(): String = mimeType ?: "*/*"

    fun getThumbUrl(): String = thumbUrl ?: ""

    fun getOriginalUrl(): String = originalUrl ?: ""

    fun getUser(): String = user ?: ""

    fun getTimestamp(): String = timestamp ?: ""

    fun getMetadata(): ExtMetadata? = metadata
}
