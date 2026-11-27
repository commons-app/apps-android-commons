package fr.free.nrw.commons.edit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import androidx.exifinterface.media.ExifInterface
import fr.free.nrw.commons.ajpegtran.blur.BlurRegion
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Soft-blurs rectangular regions on a JPEG by decoding, stack-blurring, and re-encoding.
 */
object SoftBlurApplier {
    private const val JPEG_QUALITY = 95

    /**
     * Soft-blurs [regions] in [imagePath] at [strength] and writes [output].
     */
    fun apply(
        imagePath: String,
        regions: List<BlurRegion>,
        strength: Int,
        output: File,
    ) {
        val source =
            BitmapFactory.decodeFile(imagePath)?.copy(Bitmap.Config.ARGB_8888, true)
                ?: error("Could not decode image for soft blur: $imagePath")
        try {
            val canvas = Canvas(source)
            for (region in regions) {
                val radius = radiusFor(region, strength) ?: continue
                blurRegionOnto(canvas, source, region, radius)
            }
            writeJpeg(source, imagePath, output)
        } finally {
            source.recycle()
        }
    }

    private fun radiusFor(
        region: BlurRegion,
        strength: Int,
    ): Int? = BlurStrength.radiusPx(strength, region.width, region.height)

    private fun blurRegionOnto(
        canvas: Canvas,
        source: Bitmap,
        region: BlurRegion,
        radius: Int,
    ) {
        val pad = radius
        val srcLeft = max(0, region.cornerX - pad)
        val srcTop = max(0, region.cornerY - pad)
        val srcRight = min(source.width, region.cornerX + region.width + pad)
        val srcBottom = min(source.height, region.cornerY + region.height + pad)
        val paddedWidth = srcRight - srcLeft
        val paddedHeight = srcBottom - srcTop
        if (paddedWidth <= 0 || paddedHeight <= 0) {
            return
        }
        val padded = Bitmap.createBitmap(source, srcLeft, srcTop, paddedWidth, paddedHeight)
        val blurred = StackBlur.blur(padded, radius)
        val insetLeft = region.cornerX - srcLeft
        val insetTop = region.cornerY - srcTop
        val srcRect =
            Rect(insetLeft, insetTop, insetLeft + region.width, insetTop + region.height)
        val dstRect =
            Rect(
                region.cornerX,
                region.cornerY,
                region.cornerX + region.width,
                region.cornerY + region.height,
            )
        canvas.drawBitmap(blurred, srcRect, dstRect, null)
        padded.recycle()
        blurred.recycle()
    }

    private fun writeJpeg(
        source: Bitmap,
        imagePath: String,
        output: File,
    ) {
        val sourceExif = runCatching { ExifInterface(imagePath) }.getOrNull()
        FileOutputStream(output).use { stream ->
            if (!source.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) {
                error("Failed to compress soft-blurred JPEG")
            }
        }
        if (sourceExif != null) {
            runCatching {
                val destExif = ExifInterface(output.absolutePath)
                copyExifAttributes(sourceExif, destExif)
                destExif.saveAttributes()
            }.onFailure { Timber.e(it, "Failed to copy EXIF after soft blur") }
        }
    }

    private fun copyExifAttributes(
        source: ExifInterface,
        dest: ExifInterface,
    ) {
        val tags =
            listOf(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_F_NUMBER,
                ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_IMAGE_DESCRIPTION,
                ExifInterface.TAG_SOFTWARE,
            )
        for (tag in tags) {
            source.getAttribute(tag)?.let { dest.setAttribute(tag, it) }
        }
    }
}
