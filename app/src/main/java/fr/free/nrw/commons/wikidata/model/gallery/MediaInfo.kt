package fr.free.nrw.commons.wikidata.model.gallery

/**
 * Contains the playback sources and subtitle tracks available for a media file.
 */
class MediaInfo {
    private val derivatives: List<MediaDerivative>? = null
    private val timedtext: List<TimedTextTrack>? = null

    /**
     * Returns the available playback sources, or an empty list when none are provided.
     */
    fun derivatives(): List<MediaDerivative> = derivatives.orEmpty()

    /**
     * Returns the available timed text tracks, or an empty list when none are provided.
     */
    fun timedTextTracks(): List<TimedTextTrack> = timedtext.orEmpty()
}
