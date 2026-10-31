package fr.free.nrw.commons.media.zoomControllers.zoomable

import android.graphics.Matrix
import android.graphics.RectF
import androidx.annotation.IntDef
import kotlin.math.abs

/**
 * Helper class that enforces transformation constraints such as
 * scale limiting, translation clamping, and identity/scrollability checks.
 */
class TransformLimiter(
    var minScale: Float = 1.0f,
    var maxScale: Float = 2.0f,
) {
    @IntDef(
        flag = true,
        value = [LIMIT_NONE, LIMIT_TRANSLATION_X, LIMIT_TRANSLATION_Y, LIMIT_SCALE, LIMIT_ALL],
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class LimitFlag

    companion object {
        const val LIMIT_NONE = 0
        const val LIMIT_TRANSLATION_X = 1
        const val LIMIT_TRANSLATION_Y = 2
        const val LIMIT_SCALE = 4
        const val LIMIT_ALL = LIMIT_TRANSLATION_X or LIMIT_TRANSLATION_Y or LIMIT_SCALE

        private const val EPS = 1e-3f
        private const val MATRIX_VALUES_SIZE = 9
    }

    private val tempValues = FloatArray(MATRIX_VALUES_SIZE)
    private val tempRect = RectF()

    /**
     * Keeps the scaling factor within the specified limits.
     *
     * @param pivotX x coordinate of the pivot point
     * @param pivotY y coordinate of the pivot point
     * @param limitTypes whether to limit scale.
     * @return whether limiting has been applied or not
     */
    fun limitScale(
        transform: Matrix,
        pivotX: Float,
        pivotY: Float,
        @LimitFlag limitTypes: Int,
    ): Boolean {
        if (!shouldLimit(limitTypes, LIMIT_SCALE)) {
            return false
        }
        val currentScale = getMatrixScaleFactor(transform)
        val targetScale = limit(currentScale, minScale, maxScale)
        return if (targetScale != currentScale) {
            val scale = targetScale / currentScale
            transform.postScale(scale, scale, pivotX, pivotY)
            true
        } else {
            false
        }
    }

    /**
     * Limits the translation so that there are no empty spaces on the sides if possible.
     */
    fun limitTranslation(
        transform: Matrix,
        @LimitFlag limitTypes: Int,
        imageBounds: RectF,
        viewBounds: RectF,
    ): Boolean {
        if (!shouldLimit(limitTypes, LIMIT_TRANSLATION_X or LIMIT_TRANSLATION_Y)) {
            return false
        }
        val b = tempRect
        b.set(imageBounds)
        transform.mapRect(b)
        val offsetLeft =
            if (shouldLimit(limitTypes, LIMIT_TRANSLATION_X)) {
                getOffset(
                    b.left,
                    b.right,
                    viewBounds.left,
                    viewBounds.right,
                    imageBounds.centerX(),
                )
            } else {
                0f
            }
        val offsetTop =
            if (shouldLimit(limitTypes, LIMIT_TRANSLATION_Y)) {
                getOffset(
                    b.top,
                    b.bottom,
                    viewBounds.top,
                    viewBounds.bottom,
                    imageBounds.centerY(),
                )
            } else {
                0f
            }

        return if (offsetLeft != 0f || offsetTop != 0f) {
            transform.postTranslate(offsetLeft, offsetTop)
            true
        } else {
            false
        }
    }

    /**
     * Checks whether the specified limit flag is present in the limits provided.
     */
    private fun shouldLimit(
        @LimitFlag limits: Int,
        @LimitFlag flag: Int,
    ): Boolean = (limits and flag) != LIMIT_NONE

    /**
     * Returns the offset necessary to make sure that:
     * - The image is centered if it's smaller than the limit
     * - There is no empty space if the image is bigger than the limit
     */
    private fun getOffset(
        imageStart: Float,
        imageEnd: Float,
        limitStart: Float,
        limitEnd: Float,
        limitCenter: Float,
    ): Float {
        val imageWidth = imageEnd - imageStart
        val limitWidth = limitEnd - limitStart
        val limitInnerWidth = minOf(limitCenter - limitStart, limitEnd - limitCenter) * 2

        return when {
            imageWidth < limitInnerWidth -> limitCenter - (imageEnd + imageStart) / 2
            imageWidth < limitWidth ->
                if (limitCenter < (limitStart + limitEnd) / 2) {
                    limitStart - imageStart
                } else {
                    limitEnd - imageEnd
                }
            imageStart > limitStart -> limitStart - imageStart
            imageEnd < limitEnd -> limitEnd - imageEnd
            else -> 0f
        }
    }

    /** Limits the value to the given min and max range. */
    private fun limit(
        value: Float,
        min: Float,
        max: Float,
    ): Float = min.coerceAtLeast(value).coerceAtMost(max)

    /**
     * Gets the scale factor for the given matrix. Assumes equal scaling for X and Y axis.
     */
    fun getMatrixScaleFactor(transform: Matrix): Float {
        transform.getValues(tempValues)
        return tempValues[Matrix.MSCALE_X]
    }

    /** Checks if the matrix is an identity matrix within a given tolerance `eps`. */
    fun isMatrixIdentity(
        transform: Matrix,
        eps: Float = EPS,
    ): Boolean {
        transform.getValues(tempValues)
        tempValues[Matrix.MSCALE_X] -= 1.0f
        tempValues[Matrix.MSCALE_Y] -= 1.0f
        tempValues[Matrix.MPERSP_2] -= 1.0f
        return tempValues.all { abs(it) <= eps }
    }

    /** Returns whether the scroll can happen in all directions. */
    fun canScrollInAllDirection(
        transformedImageBounds: RectF,
        viewBounds: RectF,
        eps: Float = EPS,
    ): Boolean =
        transformedImageBounds.left < viewBounds.left - eps &&
            transformedImageBounds.top < viewBounds.top - eps &&
            transformedImageBounds.right > viewBounds.right + eps &&
            transformedImageBounds.bottom > viewBounds.bottom + eps
}
