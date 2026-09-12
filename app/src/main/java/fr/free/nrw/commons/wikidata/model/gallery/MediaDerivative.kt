package fr.free.nrw.commons.wikidata.model.gallery

import com.google.gson.annotations.SerializedName
import fr.free.nrw.commons.MediaType

/**
 * Describes an original or transcoded media playback source returned by MediaWiki.
 */
class MediaDerivative {
    private val src: String? = null
    private val type: String? = null
    private val width = 0
    private val height = 0
    private val bandwidth = 0

    @SerializedName("transcodekey")
    private val transcodeKey: String? = null

    fun src(): String = src!!

    fun type(): String = type!!

    fun width(): Int = width

    fun height(): Int = height

    fun bandwidth(): Int = bandwidth

    fun transcodeKey(): String? = transcodeKey

    /**
     * Returns the media type inferred from this playback source's MIME type.
     */
    fun mediaType(): MediaType =
        when {
            mediaMimeType().startsWith("video/", ignoreCase = true) -> MediaType.VIDEO
            mediaMimeType().startsWith("audio/", ignoreCase = true) -> MediaType.AUDIO
            else -> MediaType.OTHER
        }

    /**
     * Returns the MIME type without parameters such as codec information.
     */
    private fun mediaMimeType(): String = type().substringBefore(';').trim()
}
