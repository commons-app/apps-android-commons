package fr.free.nrw.commons.media

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import fr.free.nrw.commons.wikidata.model.gallery.MediaDerivative
import fr.free.nrw.commons.wikidata.model.gallery.TimedTextTrack
import okhttp3.OkHttpClient

/**
 * Owns the ExoPlayer used on the media detail screen.
 *
 * Holds the available playback sources, the selected source, and the
 * position to resume from after an error. 'onError' is called when
 * playback fails.
 */
class MediaDetailPlayer(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val onError: () -> Unit,
) {
    var player: ExoPlayer? = null
        private set

    var sources: List<MediaDerivative> = emptyList()
        private set

    var timedTextTracks: List<TimedTextTrack> = emptyList()
        private set

    var selected: MediaDerivative? = null
        private set

    private var resumeSource: MediaDerivative? = null
    private var resumePosition: Long = 0

    fun setSources(sources: List<MediaDerivative>, timedTextTracks: List<TimedTextTrack>) {
        this.sources = sources
        this.timedTextTracks = timedTextTracks
    }

    /**
     * Returns the source to play, preferring the one interrupted by an error.
     */
    fun preferredSource(maxResolution: Int): MediaDerivative? =
        preferredMediaPlaybackSource(sources, maxResolution, resumeSource)

    /**
     * Plays the source in the given view, creating the player if needed.
     *
     * The current position is kept when switching between sources.
     */
    fun play(source: MediaDerivative, playerView: PlayerView) {
        val previousSource = selected
        val position = player?.currentPosition ?: resumePosition
        selected = source
        resumeSource = null
        resumePosition = 0

        val player = player ?: createPlayer().also { player = it }

        val url = source.src()
        if (previousSource?.src() != url) {
            player.setMediaItem(mediaItem(url))
            player.prepare()
            player.seekTo(position)
        }

        playerView.player = player
        player.play()
    }

    fun resume() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    /**
     * Remembers the selected source and position so playback can resume
     * after the player is released.
     */
    fun saveResumeState() {
        if (selected != null) {
            resumeSource = selected
            resumePosition = player?.currentPosition ?: 0
        }
    }

    /**
     * Releases the player and forgets the sources.
     */
    fun release() {
        player?.release()
        player = null
        sources = emptyList()
        timedTextTracks = emptyList()
        selected = null
    }

    @OptIn(UnstableApi::class)
    private fun createPlayer(): ExoPlayer =
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(OkHttpDataSource.Factory(httpClient)))
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        10_000,
                        30_000,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                    )
                    .build(),
            )
            .build()
            .also {
                it.addListener(
                    object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            onError()
                        }
                    },
                )
            }

    private fun mediaItem(mediaUrl: String): MediaItem =
        MediaItem.Builder()
            .setUri(mediaUrl)
            .setSubtitleConfigurations(
                timedTextTracks.map { timedTextTrack ->
                    MediaItem.SubtitleConfiguration.Builder(timedTextTrack.src().toUri())
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .setLanguage(timedTextTrack.language())
                        .setLabel(timedTextTrack.label())
                        .build()
                },
            )
            .build()
}
