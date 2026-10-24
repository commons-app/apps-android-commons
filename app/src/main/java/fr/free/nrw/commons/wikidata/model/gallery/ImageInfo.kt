package fr.free.nrw.commons.wikidata.model.gallery

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Gson POJO for a standard image info object as returned by the API ImageInfo module
 */
open class ImageInfo : Serializable {
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

    /**
     * Updates the ThumbUrl if image dimensions are not sufficient. Specifically, in panoramic
     * images the height retrieved is less than required due to large width to height ratio, so we
     * update the thumb url keeping a minimum height threshold.
     */
    private fun updateThumbUrl() {
        // If thumbHeight retrieved from API is less than THRESHOLD_HEIGHT
        if (getThumbHeight() < THRESHOLD_HEIGHT) {
            // If thumbWidthRetrieved is same as queried width ( If not tells us that the image has no larger dimensions. )
            if (getThumbWidth() == QUERY_WIDTH) {
                // Calculate new width depending on the aspect ratio.
                val finalWidth =
                    (
                        THRESHOLD_HEIGHT * getThumbWidth() * 1.0 /
                            getThumbHeight()
                    ).toInt()
                thumbHeight = THRESHOLD_HEIGHT
                thumbWidth = finalWidth
                val toReplace = "/" + QUERY_WIDTH + "px"
                val position = thumbUrl!!.lastIndexOf(toReplace)
                thumbUrl =
                    (StringBuilder(thumbUrl ?: ""))
                        .replace(
                            position,
                            position + toReplace.length,
                            "/" + thumbWidth + "px",
                        ).toString()
            }
        }
    }

    companion object {
        /**
         * Query width, default width parameter of the API query in pixels.
         */
        private const val QUERY_WIDTH = 640

        /**
         * Threshold height, the minimum height of the image in pixels.
         */
        private const val THRESHOLD_HEIGHT = 220
    }
}
