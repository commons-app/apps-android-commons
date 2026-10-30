package fr.free.nrw.commons.wikidata.model.gallery

import com.google.gson.JsonArray
import com.google.gson.JsonElement

/**
 * Contains the playback sources and subtitle tracks available for a media file.
 */
class MediaInfo {
    private val url: String? = null
    private val mime: String? = null
    private val size: Long = 0
    private val width = 0
    private val height = 0
    private val duration = 0.0
    private val metadata: JsonArray? = null
    private val derivatives: List<MediaDerivative>? = null
    private val timedtext: List<TimedTextTrack>? = null

    /**
     * Returns the available transcodes, or an empty list when none are provided.
     */
    fun derivatives(): List<MediaDerivative> = derivatives.orEmpty()

    /**
     * Returns the available timed text tracks, or an empty list when none are provided.
     */
    fun timedTextTracks(): List<TimedTextTrack> = timedtext.orEmpty()

    /**
     * Returns the transcodes followed by the original file.
     */
    fun playbackSources(): List<MediaDerivative> = derivatives() + listOfNotNull(originalSource())

    /**
     * Returns the original file as a playback source.
     *
     * Codecs come from the file's stream metadata and bandwidth from its
     * size and duration. The source has no transcode key, which is how
     * it is told apart from the transcodes. Returns null without a URL
     * or MIME type.
     */
    fun originalSource(): MediaDerivative? {
        val url = url ?: return null
        val mime = mime ?: return null

        val codecs = originalCodecs()
        val mediaMimeType =
            when {
                mime != "application/ogg" -> mime
                "theora" in codecs -> "video/ogg"
                else -> "audio/ogg"
            }
        val type =
            if (codecs.isEmpty()) mediaMimeType else "$mediaMimeType; codecs=\"${codecs.joinToString(", ")}\""
        val bandwidth = if (duration > 0) (size * 8 / duration).toInt() else 0

        return MediaDerivative(url, type, width, height, bandwidth)
    }

    /**
     * Returns the codec names of the original file's streams, video first.
     *
     * Ogg files list their streams with a 'type' each. Other containers
     * report a 'dataformat' for their video and audio streams.
     */
    private fun originalCodecs(): List<String> {
        val metadata = metadata ?: return emptyList()

        val oggStreams = metadata.entry("streams")?.entries()
        if (oggStreams != null) {
            return oggStreams.mapNotNull { stream ->
                stream
                    .value()
                    ?.entries()
                    ?.entry("type")
                    ?.string()
                    ?.let { codecName(it) }
            }
        }

        return listOf("video", "audio").mapNotNull { section ->
            metadata
                .entry(section)
                ?.entries()
                ?.entry("dataformat")
                ?.string()
                ?.let { codecName(it) }
        }
    }

    /**
     * Returns the codec name for a stream type found in file metadata.
     */
    private fun codecName(streamType: String): String =
        when (streamType) {
            "V_VP9" -> "vp9"
            "V_AV1" -> "av01"
            "A_OPUS" -> "opus"
            else -> streamType.lowercase()
        }
}

/**
 * Returns the value of the metadata entry called 'name', or null.
 */
private fun JsonArray.entry(name: String): JsonElement? =
    firstOrNull { it.isJsonObject && it.asJsonObject["name"]?.string() == name }?.value()

/**
 * Returns the value of a metadata entry, or null when this is not an entry.
 */
private fun JsonElement.value(): JsonElement? = takeIf { isJsonObject }?.asJsonObject?.get("value")

/**
 * Returns the nested metadata entries, or null when this is not a list.
 */
private fun JsonElement.entries(): JsonArray? = takeIf { isJsonArray }?.asJsonArray

private fun JsonElement.string(): String? = takeIf { isJsonPrimitive }?.asString
