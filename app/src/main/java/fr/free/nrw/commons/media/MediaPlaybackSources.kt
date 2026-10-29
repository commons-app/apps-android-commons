package fr.free.nrw.commons.media

import android.util.DisplayMetrics
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import fr.free.nrw.commons.MediaType
import fr.free.nrw.commons.wikidata.model.gallery.MediaDerivative
import kotlin.math.min

/**
 * Returns the transcode resolution cap for a video quality preference.
 *
 * 'higher' has no cap and 'data_saver' allows nothing, so the lowest
 * transcode is used. Any other value caps at the shorter screen side.
 */
fun maxPlaybackResolution(videoQuality: String?, displayMetrics: DisplayMetrics): Int =
    when (videoQuality) {
        "higher" -> Int.MAX_VALUE
        "data_saver" -> 0
        else -> min(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

/**
 * Returns the preferred source in the list of media derivatives.
 *
 * A source matching 'resume' wins. Video then takes the transcode with
 * the largest resolution at or below 'maxResolution', or the smallest
 * transcode when none fits. Audio takes the first transcode. Without
 * any transcode the first source is used.
 */
fun preferredMediaPlaybackSource(
    playbackSources: List<MediaDerivative>,
    maxResolution: Int,
    resume: MediaDerivative? = null,
): MediaDerivative? {
    if (resume != null) {
        val resumed = playbackSources.firstOrNull { it.src() == resume.src() }
        if (resumed != null) {
            return resumed
        }
    }

    val transcodes = playbackSources.filter { !it.transcodeKey().isNullOrBlank() }
    val firstTranscode = transcodes.firstOrNull() ?: return playbackSources.firstOrNull()
    if (firstTranscode.mediaType() != MediaType.VIDEO) {
        return firstTranscode
    }

    return transcodes
        .filter { it.transcodeResolution() <= maxResolution }
        .maxByOrNull { it.transcodeResolution() }
        ?: transcodes.minByOrNull { it.transcodeResolution() }
}

/**
 * Returns the resolution named by the transcode key, such as 480 for
 * '480p.vp9.webm', falling back to the height.
 */
private fun MediaDerivative.transcodeResolution(): Int =
    transcodeKey()?.substringBefore('p')?.toIntOrNull() ?: height()

/**
 * Returns supported playback sources with transcodes before originals.
 *
 * Sources without a decoder on the device are omitted.
 */
fun playableMediaPlaybackSources(
    playbackSources: List<MediaDerivative>,
): List<MediaDerivative> {
    val (originalSources, transcodeSources) = playbackSources
        .filter(::isMediaPlaybackSupported)
        .partition { it.transcodeKey().isNullOrBlank() }
    return transcodeSources + originalSources
}

/**
 * Returns whether the device has a decoder that supports the playback source's format.
 */
@OptIn(UnstableApi::class)
fun isMediaPlaybackSupported(playbackSource: MediaDerivative): Boolean {
    val type = playbackSource.type()
    val codecs = type.substringAfter("codecs=\"", "").substringBefore('"').ifEmpty { null }

    val mediaMimeType =
        when (playbackSource.mediaType()) {
            MediaType.VIDEO -> MimeTypes.getVideoMediaMimeType(codecs)
            else -> MimeTypes.getAudioMediaMimeType(codecs)
        } ?: MimeTypes.normalizeMimeType(type.substringBefore(';').trim())

    val format = Format.Builder()
        .setSampleMimeType(mediaMimeType)
        .setCodecs(codecs)
        .setWidth(playbackSource.width())
        .setHeight(playbackSource.height())
        .build()

    return MediaCodecUtil.getDecoderInfos(mediaMimeType, false, false)
        .any { it.isFormatSupported(format) }
}
