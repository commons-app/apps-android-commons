package fr.free.nrw.commons.media.zoomControllers.gestures

import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Pivot, translation, scale and rotation calculated from [PointerSlots].
 *
 * @param slots pointer coordinates for the gesture in progress
 */
class GestureTransform(
    private val slots: PointerSlots,
) {
    /** Gets the X coordinate of the pivot point */
    fun getPivotX(): Float = calcAverage(slots.getStartX(), slots.pointerCount)

    /** Gets the Y coordinate of the pivot point */
    fun getPivotY(): Float = calcAverage(slots.getStartY(), slots.pointerCount)

    /** Gets the X component of the translation */
    fun getTranslationX(): Float =
        calcAverage(slots.getCurrentX(), slots.pointerCount) -
            calcAverage(slots.getStartX(), slots.pointerCount)

    /** Gets the Y component of the translation */
    fun getTranslationY(): Float =
        calcAverage(slots.getCurrentY(), slots.pointerCount) -
            calcAverage(slots.getStartY(), slots.pointerCount)

    /** Gets the scale */
    fun getScale(): Float =
        if (slots.pointerCount < 2) {
            1f
        } else {
            val startDeltaX = slots.getStartX()[1] - slots.getStartX()[0]
            val startDeltaY = slots.getStartY()[1] - slots.getStartY()[0]
            val currentDeltaX = slots.getCurrentX()[1] - slots.getCurrentX()[0]
            val currentDeltaY = slots.getCurrentY()[1] - slots.getCurrentY()[0]
            val startDist = hypot(startDeltaX, startDeltaY)
            val currentDist = hypot(currentDeltaX, currentDeltaY)
            currentDist / startDist
        }

    /** Gets the rotation in radians */
    fun getRotation(): Float =
        if (slots.pointerCount < 2) {
            0f
        } else {
            val startDeltaX = slots.getStartX()[1] - slots.getStartX()[0]
            val startDeltaY = slots.getStartY()[1] - slots.getStartY()[0]
            val currentDeltaX = slots.getCurrentX()[1] - slots.getCurrentX()[0]
            val currentDeltaY = slots.getCurrentY()[1] - slots.getCurrentY()[0]
            val startAngle = atan2(startDeltaY, startDeltaX)
            val currentAngle = atan2(currentDeltaY, currentDeltaX)
            currentAngle - startAngle
        }

    private fun calcAverage(
        arr: FloatArray,
        len: Int,
    ): Float {
        val sum = arr.take(len).sum()
        return if (len > 0) sum / len else 0f
    }
}
