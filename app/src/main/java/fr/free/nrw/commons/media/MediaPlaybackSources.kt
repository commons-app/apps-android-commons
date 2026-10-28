package fr.free.nrw.commons.media

import android.content.Context
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import fr.free.nrw.commons.MediaType
import fr.free.nrw.commons.R
import fr.free.nrw.commons.wikidata.model.gallery.MediaDerivative

/**
 * Returns the preferred source in the list of media derivatives.
 *
 * A source matching 'resume' wins, then the first transcode, then the
 * first source.
 *
 * TODO: This should be based on the screen resolution or user
 * preference instead.
 */
fun preferredMediaPlaybackSource(
    playbackSources: List<MediaDerivative>,
    resume: MediaDerivative? = null,
): MediaDerivative? =
    resume?.let { resumed ->
        playbackSources.firstOrNull { it.src() == resumed.src() }
    }
        ?: playbackSources.firstOrNull { it.transcodeKey().isNullOrBlank().not() }
        ?: playbackSources.firstOrNull()

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

/**
 * Returns the label shown for this source in the quality selector.
 *
 * Audio sources are labeled by bitrate, video sources by transcode
 * resolution.
 */
fun MediaDerivative.qualityLabel(context: Context, mediaType: MediaType?): String {
    val originalLabel = context.getString(R.string.original)

    if (mediaType == MediaType.AUDIO) {
        val bitrate = "${(bandwidth() + 500) / 1_000} kbps"
        if (transcodeKey() == null) {
            return "$bitrate ($originalLabel)"
        }
        return bitrate
    }

    return transcodeKey()?.substringBefore('.') ?: originalLabel
}
