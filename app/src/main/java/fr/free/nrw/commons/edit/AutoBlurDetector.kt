package fr.free.nrw.commons.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.commons.ml.common.DetectionResult
import org.commons.ml.vision.CommonsVision
import timber.log.Timber

/**
 * Finds the regions to blur automatically in an image, such as faces and licence plates,
 * with [CommonsVision].
 */
class AutoBlurDetector(
    private val context: Context,
) {
    /**
     * Detects the regions to blur in the image at [pathOrUri].
     *
     * @param pathOrUri The content URI or file path of the image.
     * @param onProgress Called with the detection progress in percent, off the main thread.
     * @return The detected regions in the coordinates of the decoded image,
     *         or null if the image could not be loaded.
     */
    suspend fun detect(
        pathOrUri: String,
        onProgress: (Int) -> Unit,
    ): Detections? {
        val decodedBitmap = loadBitmapForDetection(pathOrUri) ?: return null

        val detector = CommonsVision(context)
        val detectionResult =
            withContext(Dispatchers.Default) {
                detector.detect(decodedBitmap) { progress ->
                    onProgress((progress * PERCENT).toInt())
                }
            }
        val rawDetections =
            when (detectionResult) {
                is DetectionResult.Success -> detectionResult.detections
                is DetectionResult.Partial -> {
                    Timber.w(
                        "Partial detection result: unavailable capabilities = " +
                            "${detectionResult.detections}",
                    )
                    detectionResult.detections
                }
                else -> emptyList()
            }
        return Detections(decodedBitmap.width, decodedBitmap.height, rawDetections.map { it.bounds })
    }

    /**
     * Safely decodes a bitmap from a content URI or file path off the main thread.
     */
    private suspend fun loadBitmapForDetection(pathOrUri: String): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = pathOrUri.toUri()
                if (uri.scheme == "content") {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } else {
                    val filePath = if (uri.scheme == "file") uri.path ?: pathOrUri else pathOrUri
                    BitmapFactory.decodeFile(filePath)
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to load bitmap for detection from pathOrUri: $pathOrUri")
            }.getOrNull()
        }

    /**
     * Regions found by [detect].
     *
     * @property bitmapWidth The width of the image the regions were detected in.
     * @property bitmapHeight The height of the image the regions were detected in.
     * @property bounds The detected regions, in the coordinates of that image.
     */
    data class Detections(
        val bitmapWidth: Int,
        val bitmapHeight: Int,
        val bounds: List<RectF>,
    ) {
        /**
         * Returns [bounds] scaled to an image of [width] x [height],
         * or an empty list if the detected image has no size.
         */
        fun scaledTo(
            width: Int,
            height: Int,
        ): List<RectF> {
            if (bitmapWidth <= 0 || bitmapHeight <= 0) return emptyList()
            val scaleX = width.toFloat() / bitmapWidth
            val scaleY = height.toFloat() / bitmapHeight
            return bounds.map { b ->
                RectF(
                    b.left * scaleX,
                    b.top * scaleY,
                    b.right * scaleX,
                    b.bottom * scaleY,
                )
            }
        }
    }

    private companion object {
        const val PERCENT = 100
    }
}
