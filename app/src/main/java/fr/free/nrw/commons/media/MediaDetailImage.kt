package fr.free.nrw.commons.media

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.imagepipeline.request.ImageRequest
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.MediaType
import fr.free.nrw.commons.R
import fr.free.nrw.commons.media.MediaDetailFragment.Companion.DEFAULT_IMAGE_BACKGROUND_COLOR

private const val IMAGE_BACKGROUND_COLOR_KEY = "image_background_color"

/** Opens the full-size image. */
internal fun MediaDetailFragment.launchZoomActivity(view: View) {
    if (media!!.imageUrl != null) {
        val ctx: Context = view.context
        val zoomableIntent = Intent(ctx, ZoomableActivity::class.java)
        zoomableIntent.setData(Uri.parse(media!!.imageUrl))
        zoomableIntent.putExtra(
            ZoomableActivity.ZoomableActivityConstants.ORIGIN,
            "MediaDetails",
        )

        val backgroundColor: Int = imageBackgroundColor
        if (backgroundColor != DEFAULT_IMAGE_BACKGROUND_COLOR) {
            zoomableIntent.putExtra(
                ZoomableActivity.ZoomableActivityConstants.PHOTO_BACKGROUND_COLOR,
                backgroundColor,
            )
        }

        ctx.startActivity(
            zoomableIntent,
        )
    }
}

/**
 * The imageSpacer is Basically a transparent overlay for the SimpleDraweeView
 * which holds the image to be displayed( moreover this image is out of
 * the scroll view )
 *
 *
 * If the image is sufficiently large i.e. the image height extends the view height, we reduce
 * the height and change the width to maintain the aspect ratio, otherwise image takes up the
 * total possible width and height is adjusted accordingly.
 *
 * @param scrollWidth the current width of the scrollView
 */
internal fun MediaDetailFragment.updateAspectRatio(scrollWidth: Int) {
    val width = mediaDetailPlayer.selected?.width() ?: imageInfoCache?.width ?: 0
    val height = mediaDetailPlayer.selected?.height() ?: imageInfoCache?.height ?: 0
    if (width > 0 && height > 0) {
        var finalHeight: Int = (scrollWidth * height) / width
        val params: ViewGroup.LayoutParams = binding.mediaDetailMediaContainer.layoutParams
        val spacerParams: ViewGroup.LayoutParams =
            binding.mediaDetailImageViewSpacer.layoutParams
        params.width = scrollWidth
        if (finalHeight > frameLayoutHeight - minimumHeightOfMetadata) {
            // Adjust the height and width of image.

            val temp: Int = frameLayoutHeight - minimumHeightOfMetadata
            params.width = (scrollWidth * temp) / finalHeight
            finalHeight = temp
        }
        params.height = finalHeight
        spacerParams.height = finalHeight
        binding.mediaDetailMediaContainer.layoutParams = params
        binding.mediaDetailImageViewSpacer.layoutParams = spacerParams
    }
}

/**
 * Uses two image sources.
 * - low resolution thumbnail is shown initially
 * - when the high resolution image is available, it replaces the low resolution image
 */
internal fun MediaDetailFragment.setupImageView(media: Media? = this.media) {
    val imageBackgroundColor: Int = imageBackgroundColor
    if (imageBackgroundColor != DEFAULT_IMAGE_BACKGROUND_COLOR) {
        binding.mediaDetailImageView.setBackgroundColor(imageBackgroundColor)
    }

    binding.mediaDetailImageView.hierarchy.setPlaceholderImage(R.drawable.image_placeholder)
    binding.mediaDetailImageView.hierarchy.setFailureImage(R.drawable.image_placeholder)
    binding.mediaDetailImageProgressBar.visibility = View.VISIBLE

    val fullImageUrl =
        if (media?.mediaType == MediaType.VIDEO ||
            media?.mediaType == MediaType.AUDIO
        ) {
            media.thumbUrl
        } else {
            media?.imageUrl
        }
    val controller: DraweeController =
        Fresco
            .newDraweeControllerBuilder()
            .setLowResImageRequest(ImageRequest.fromUri(media?.thumbUrl))
            .setRetainImageOnFailure(true)
            .setImageRequest(ImageRequest.fromUri(fullImageUrl))
            .setControllerListener(aspectRatioListener)
            .setOldController(binding.mediaDetailImageView.controller)
            .build()
    binding.mediaDetailImageView.controller = controller
}

/**
 * Sets up the media view based on the media type.
 *
 * For audio and video, a play button and thumbnail are shown until playback starts. Images
 * render without an overlay.
 */
internal fun MediaDetailFragment.setupMediaView(media: Media) {
    // A thumbnail is always shown initially because audio and video have thumbnails.
    setupImageView(media)
    configureFullscreenButton(media.mediaType)

    when (media.mediaType) {
        MediaType.VIDEO,
        MediaType.AUDIO,
        -> showMediaPoster(playButton = true)
        MediaType.IMAGE,
        MediaType.OTHER,
        -> Unit
    }
}

/** Handles clicks on the media view container by starting playback or opening the zoom view. */
internal fun MediaDetailFragment.onMediaClicked() {
    val media = media ?: return

    when (media.mediaType) {
        MediaType.VIDEO,
        MediaType.AUDIO,
        -> prepareMedia()
        MediaType.IMAGE -> launchZoomActivity(binding.mediaDetailImageView)
        MediaType.OTHER -> Unit
    }
}

/** Displays the media poster thumbnail with an optional play button overlay. */
internal fun MediaDetailFragment.showMediaPoster(playButton: Boolean = false) {
    val isAudio = media?.mediaType == MediaType.AUDIO
    val playLabel = if (isAudio) R.string.play_audio else R.string.play_video

    binding.mediaDetailMediaContainer.contentDescription =
        if (playButton) getString(playLabel) else null
    binding.mediaDetailPlayerView.visibility = View.GONE
    binding.mediaDetailPlayButton.visibility = if (playButton) View.VISIBLE else View.GONE
    binding.mediaDetailMediaUnavailable.visibility = View.GONE
    binding.mediaDetailImageView.visibility = View.VISIBLE
}

/** Displays the active media player view in place of the poster thumbnail. */
internal fun MediaDetailFragment.showMediaPlayer() {
    binding.mediaDetailMediaContainer.contentDescription = null
    binding.mediaDetailImageView.visibility = View.GONE
    binding.mediaDetailPlayButton.visibility = View.GONE
    binding.mediaDetailMediaUnavailable.visibility = View.GONE
    binding.mediaDetailPlayerView.visibility = View.VISIBLE
}

/** Displays an unavailable media overlay over the poster thumbnail. */
internal fun MediaDetailFragment.showMediaUnavailable() {
    val isAudio = media?.mediaType == MediaType.AUDIO
    val unavailableLabel =
        if (isAudio) R.string.audio_unavailable else R.string.video_unavailable
    binding.mediaDetailMediaUnavailable.setText(unavailableLabel)
    binding.mediaDetailMediaContainer.contentDescription = getString(unavailableLabel)
    binding.mediaDetailPlayerView.visibility = View.GONE
    binding.mediaDetailPlayButton.visibility = View.GONE
    binding.mediaDetailMediaUnavailable.visibility = View.VISIBLE
    binding.mediaDetailImageView.visibility = View.VISIBLE
}

/**
 * Shows the stored caption when the image never loads.
 * The scroll view stays invisible until an image reports its size, so a 404 would otherwise
 * leave this screen blank.
 */
internal fun MediaDetailFragment.revealMediaDetailWithoutImage(allowRetry: Boolean = true) {
    val currentBinding = this.currentBinding
    val scrollView = currentBinding?.mediaDetailScrollView
    if (currentBinding != null && scrollView != null && scrollView.isAttachedToWindow) {
        val scrollWidth = scrollView.width
        if (scrollWidth <= 0 && allowRetry) {
            scrollView.post { revealMediaDetailWithoutImage(allowRetry = false) }
        } else if (scrollWidth > 0) {
            val height = resources.getDimensionPixelSize(R.dimen.dimen_250)
            val params = currentBinding.mediaDetailMediaContainer.layoutParams
            val spacerParams = currentBinding.mediaDetailImageViewSpacer.layoutParams
            params.width = scrollWidth
            params.height = height
            spacerParams.height = height
            currentBinding.mediaDetailMediaContainer.layoutParams = params
            currentBinding.mediaDetailImageViewSpacer.layoutParams = spacerParams
            scrollView.visibility = View.VISIBLE
        }
    }
}

/**
 * Called when the image background color is changed.
 * You should pass a useable color, not a resource id.
 * @param color
 */
internal fun MediaDetailFragment.onImageBackgroundChanged(color: Int) {
    val currentColor: Int = imageBackgroundColor
    if (currentColor == color) {
        return
    }

    binding.mediaDetailImageView.setBackgroundColor(color)
    imageBackgroundColorPref.edit().putInt(IMAGE_BACKGROUND_COLOR_KEY, color).apply()
}

private val MediaDetailFragment.imageBackgroundColorPref: SharedPreferences
    get() =
        requireContext().getSharedPreferences(
            IMAGE_BACKGROUND_COLOR_KEY + media!!.pageId,
            Context.MODE_PRIVATE,
        )

private val MediaDetailFragment.imageBackgroundColor: Int
    get() {
        val imageBackgroundColorPref: SharedPreferences =
            imageBackgroundColorPref
        return imageBackgroundColorPref.getInt(
            IMAGE_BACKGROUND_COLOR_KEY,
            DEFAULT_IMAGE_BACKGROUND_COLOR,
        )
    }
