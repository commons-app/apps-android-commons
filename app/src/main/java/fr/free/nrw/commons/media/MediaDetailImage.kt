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
import fr.free.nrw.commons.R
import fr.free.nrw.commons.media.MediaDetailFragment.Companion.DEFAULT_IMAGE_BACKGROUND_COLOR

private const val IMAGE_BACKGROUND_COLOR_KEY = "image_background_color"

/** Opens the full-size image. */
internal fun MediaDetailFragment.launchZoomActivity(view: View) {
    launchZoomActivityAfterPermissionCheck(view)
}

/**
 * launch zoom acitivity after permission check
 * @param view as ImageView
 */
internal fun MediaDetailFragment.launchZoomActivityAfterPermissionCheck(view: View) {
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
    if (imageInfoCache != null) {
        var finalHeight: Int = (scrollWidth * imageInfoCache!!.height) / imageInfoCache!!.width
        val params: ViewGroup.LayoutParams = binding.mediaDetailImageView.layoutParams
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
        binding.mediaDetailImageView.layoutParams = params
        binding.mediaDetailImageViewSpacer.layoutParams = spacerParams
    }
}

/**
 * Uses two image sources.
 * - low resolution thumbnail is shown initially
 * - when the high resolution image is available, it replaces the low resolution image
 */
internal fun MediaDetailFragment.setupImageView() {
    val imageBackgroundColor: Int = imageBackgroundColor
    if (imageBackgroundColor != DEFAULT_IMAGE_BACKGROUND_COLOR) {
        binding.mediaDetailImageView.setBackgroundColor(imageBackgroundColor)
    }

    binding.mediaDetailImageView.hierarchy.setPlaceholderImage(R.drawable.image_placeholder)
    binding.mediaDetailImageView.hierarchy.setFailureImage(R.drawable.image_placeholder)
    binding.mediaDetailImageProgressBar.visibility = View.VISIBLE

    val controller: DraweeController =
        Fresco
            .newDraweeControllerBuilder()
            .setLowResImageRequest(ImageRequest.fromUri(if (media != null) media!!.thumbUrl else null))
            .setRetainImageOnFailure(true)
            .setImageRequest(ImageRequest.fromUri(if (media != null) media!!.imageUrl else null))
            .setControllerListener(aspectRatioListener)
            .setOldController(binding.mediaDetailImageView.controller)
            .build()
    binding.mediaDetailImageView.controller = controller
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
            val params = currentBinding.mediaDetailImageView.layoutParams
            val spacerParams = currentBinding.mediaDetailImageViewSpacer.layoutParams
            params.width = scrollWidth
            params.height = height
            spacerParams.height = height
            currentBinding.mediaDetailImageView.layoutParams = params
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
