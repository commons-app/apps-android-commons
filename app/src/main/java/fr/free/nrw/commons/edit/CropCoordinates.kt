package fr.free.nrw.commons.edit

import android.graphics.RectF
import timber.log.Timber
import kotlin.math.roundToInt

/**
 * Data class to hold crop coordinates.
 */
internal data class CropCoordinates(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
) {
    companion object {
        /**
         * Converts crop rectangle from view coordinates to rotated image coordinates.
         * Uses percentage-based mapping with actual file dimensions.
         *
         * @param viewCropRect The crop rectangle in view coordinates
         * @param displayedRect The displayed image bounds, in the same coordinates as [viewCropRect]
         * @param actualFileWidth The actual width of the (possibly rotated) file
         * @param actualFileHeight The actual height of the (possibly rotated) file
         * @return The crop coordinates in the file, or null if [displayedRect] has no area
         */
        fun fromViewCrop(
            viewCropRect: RectF,
            displayedRect: RectF,
            actualFileWidth: Int,
            actualFileHeight: Int,
        ): CropCoordinates? {
            // Guard against zero-size displayed rect
            if (displayedRect.width() <= 0 || displayedRect.height() <= 0) return null

            // Calculate crop as percentage of displayed bounds
            val leftPercent =
                ((viewCropRect.left - displayedRect.left) / displayedRect.width())
                    .coerceIn(0f, 1f)
            val topPercent =
                ((viewCropRect.top - displayedRect.top) / displayedRect.height())
                    .coerceIn(0f, 1f)
            val rightPercent =
                ((viewCropRect.right - displayedRect.left) / displayedRect.width())
                    .coerceIn(0f, 1f)
            val bottomPercent =
                ((viewCropRect.bottom - displayedRect.top) / displayedRect.height())
                    .coerceIn(0f, 1f)

            // Apply percentages to ACTUAL file dimensions
            val cropLeft = (leftPercent * actualFileWidth).roundToInt().coerceIn(0, actualFileWidth - 1)
            val cropTop = (topPercent * actualFileHeight).roundToInt().coerceIn(0, actualFileHeight - 1)
            val cropRight = (rightPercent * actualFileWidth).roundToInt().coerceIn(1, actualFileWidth)
            val cropBottom = (bottomPercent * actualFileHeight).roundToInt().coerceIn(1, actualFileHeight)

            val width = (cropRight - cropLeft).coerceAtLeast(1)
            val height = (cropBottom - cropTop).coerceAtLeast(1)

            // Final validation: ensure crop doesn't exceed file bounds
            val validLeft = cropLeft.coerceIn(0, actualFileWidth - width)
            val validTop = cropTop.coerceIn(0, actualFileHeight - height)

            Timber.d(
                "%s%s",
                "Crop conversion: file=${actualFileWidth}x$actualFileHeight, ",
                "crop=($validLeft, $validTop, $width, $height)",
            )

            return CropCoordinates(validLeft, validTop, width, height)
        }
    }
}
