package fr.free.nrw.commons.wikidata.model.gallery

import com.google.gson.annotations.SerializedName
import fr.free.nrw.commons.MediaType

/**
 * Describes an original or transcoded media playback source returned by MediaWiki.
 */
class MediaDerivative(
    private val src: String? = null,
    private val type: String? = null,
    private val width: Int = 0,
    private val height: Int = 0,
    private val bandwidth: Int = 0,
    @field:SerializedName("transcodekey")
    private val transcodeKey: String? = null,
) {
    fun src(): String = src!!

    fun type(): String = type!!

    fun width(): Int = width

    fun height(): Int = height

    fun bandwidth(): Int = bandwidth

    /**
     * Returns the transcode key, such as '480p.vp9.webm', or null for the original file.
     */
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
