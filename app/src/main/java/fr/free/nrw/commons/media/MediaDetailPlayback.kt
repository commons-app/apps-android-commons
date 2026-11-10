package fr.free.nrw.commons.media

import android.app.AlertDialog
import android.view.View
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import fr.free.nrw.commons.MediaType
import fr.free.nrw.commons.R
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.wikidata.model.gallery.MediaDerivative
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

/** Prepares audio or video playback for the current media item. */
internal fun MediaDetailFragment.prepareMedia() {
    val title = media?.filename ?: return
    if (mediaRequestInProgress) {
        return
    }

    // Play the media if the player is initialized.
    if (mediaDetailPlayer.player != null) {
        showMediaPlayer()
        mediaDetailPlayer.resume()
    } else {
        requestMedia(title)
    }
}

/** Fetches available playback sources and timed text tracks for the given file title. */
internal fun MediaDetailFragment.requestMedia(title: String) {
    mediaRequestInProgress = true
    showMediaPoster()

    viewDisposables.add(
        mediaClient
            .getMediaInfo(title)
            .subscribeOn(Schedulers.io())
            .map { mediaInfo ->
                playableMediaPlaybackSources(mediaInfo.playbackSources()) to mediaInfo.timedTextTracks()
            }.observeOn(AndroidSchedulers.mainThread())
            .doFinally { mediaRequestInProgress = false }
            .subscribe(
                { (playbackSources, timedTextTracks) ->
                    mediaDetailPlayer.setSources(playbackSources, timedTextTracks)
                    updateSubtitleButtonVisibility()
                    val maxResolution =
                        maxPlaybackResolution(
                            applicationKvStore.getString(Prefs.VIDEO_QUALITY, "auto"),
                            resources.displayMetrics,
                        )
                    mediaDetailPlayer.preferredSource(maxResolution)?.let(::playMedia)
                        ?: handleMediaUnavailable()
                },
                {
                    Timber.e(it)
                    handleMediaUnavailable()
                },
            ),
    )
}

/** Starts playback of the selected media derivative in the active player view. */
internal fun MediaDetailFragment.playMedia(playbackSource: MediaDerivative) {
    val playerView =
        if (mediaFullscreen) binding.mediaDetailFullscreenPlayerView else binding.mediaDetailPlayerView
    mediaDetailPlayer.play(playbackSource, playerView)

    if (media?.mediaType == MediaType.VIDEO) {
        updateAspectRatio(binding.mediaDetailScrollView.width)
    }
    showMediaPlayer()
}

/** Wires the media quality selector button on the given player view. */
internal fun MediaDetailFragment.configureMediaPlayerView(playerView: PlayerView) {
    playerView
        .findViewById<View>(R.id.mediaDetailMediaQualityButton)
        .setOnClickListener { showMediaQualitySelector() }
}

/** Configures the fullscreen toggle button for video media items. */
internal fun MediaDetailFragment.configureFullscreenButton(mediaType: MediaType) {
    if (mediaType == MediaType.VIDEO) {
        binding.mediaDetailPlayerView.setFullscreenButtonClickListener(::setMediaFullscreen)
        binding.mediaDetailFullscreenPlayerView.setFullscreenButtonClickListener(::setMediaFullscreen)
    }
}

/** Updates subtitle button visibility based on whether timed text tracks are available. */
@OptIn(UnstableApi::class)
internal fun MediaDetailFragment.updateSubtitleButtonVisibility() {
    val showSubtitleButton = mediaDetailPlayer.timedTextTracks.isNotEmpty()
    binding.mediaDetailPlayerView.setShowSubtitleButton(showSubtitleButton)
    binding.mediaDetailFullscreenPlayerView.setShowSubtitleButton(showSubtitleButton)
}

/** Enters or exits fullscreen video playback mode. */
@OptIn(UnstableApi::class)
internal fun MediaDetailFragment.setMediaFullscreen(shouldBeFullscreen: Boolean) {
    if (mediaFullscreen == shouldBeFullscreen) {
        return
    }

    mediaFullscreen = shouldBeFullscreen
    (parentFragment as? MediaDetailPagerFragment)?.setPagerSwipeEnabled(!shouldBeFullscreen)
    binding.mediaDetailPlayerView.setFullscreenButtonState(shouldBeFullscreen)
    binding.mediaDetailFullscreenPlayerView.setFullscreenButtonState(shouldBeFullscreen)

    val activity = requireActivity()
    val windowInsetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)

    if (shouldBeFullscreen) {
        binding.mediaDetailScrollView.visibility = View.GONE
        binding.mediaDetailFullscreenPlayerView.visibility = View.VISIBLE
        PlayerView.switchTargetView(
            mediaDetailPlayer.player!!,
            binding.mediaDetailPlayerView,
            binding.mediaDetailFullscreenPlayerView,
        )
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onBackPressedCallback.isEnabled = true
    } else {
        binding.mediaDetailScrollView.visibility = View.VISIBLE
        PlayerView.switchTargetView(
            mediaDetailPlayer.player!!,
            binding.mediaDetailFullscreenPlayerView,
            binding.mediaDetailPlayerView,
        )
        binding.mediaDetailFullscreenPlayerView.visibility = View.GONE
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        onBackPressedCallback.isEnabled = binding.dummyCaptionDescriptionContainer.isVisible
    }
}

/** Shows a dialog allowing the user to select the playback quality. */
internal fun MediaDetailFragment.showMediaQualitySelector() {
    val context = requireContext()
    AlertDialog
        .Builder(context)
        .setTitle(R.string.playback_quality)
        .setSingleChoiceItems(
            mediaDetailPlayer.sources.map { it.qualityLabel(context) }.toTypedArray(),
            mediaDetailPlayer.sources.indexOf(mediaDetailPlayer.selected),
        ) { dialog, selectedIndex ->
            playMedia(mediaDetailPlayer.sources[selectedIndex])
            dialog.dismiss()
        }.show()
}

/** Cleans up the player and displays an unavailable message when playback fails. */
internal fun MediaDetailFragment.handleMediaUnavailable() {
    mediaDetailPlayer.saveResumeState()
    setMediaFullscreen(false)
    cleanupMediaPlayer()
    showMediaUnavailable()
}

/** Releases the media player and detaches it from the player view. */
internal fun MediaDetailFragment.cleanupMediaPlayer() {
    mediaDetailPlayer.release()
    binding.mediaDetailPlayerView.player = null
}
