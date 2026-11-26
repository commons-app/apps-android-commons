package fr.free.nrw.commons.edit

import kotlin.math.max
import kotlin.math.min

/**
 * Maps the blur-strength SeekBar (0–100%) to a soft-blur radius.
 *
 * 100% is the privacy default: the selection is replaced with one solid average
 * color. Lower values keep the area recognizable but increasingly obscured.
 */
object BlurStrength {
    const val MAX_PROGRESS = 100

    /** Fully obscured privacy default. */
    const val DEFAULT_PROGRESS = MAX_PROGRESS

    /** Upper cap on soft-blur radius in pixels. */
    private const val ABSOLUTE_MAX_RADIUS_PX = 80

    /** Soft-blur radius floor used for small selections. */
    private const val MIN_SOFT_MAX_RADIUS_PX = 28

    /** Divides the shortest region side to scale max radius with selection size. */
    private const val REGION_SIDE_RADIUS_DIVISOR = 5

    /** Lightest non-zero soft blur. */
    private const val MIN_RADIUS_PX = 1

    /**
     * Returns the stack-blur radius for [strength], or `null` when the selection
     * should use the solid average (100%).
     *
     * @param strength SeekBar progress in `0`..[MAX_PROGRESS]
     * @param regionWidth width of the selection in pixels
     * @param regionHeight height of the selection in pixels
     */
    fun radiusPx(
        strength: Int,
        regionWidth: Int,
        regionHeight: Int,
    ): Int? {
        val clamped = strength.coerceIn(0, MAX_PROGRESS)
        if (clamped >= MAX_PROGRESS) {
            return null
        }
        val shortest = min(regionWidth, regionHeight).coerceAtLeast(MIN_RADIUS_PX)
        // Larger regions get a higher max radius so mid/high settings still look soft.
        val maxRadius =
            min(
                ABSOLUTE_MAX_RADIUS_PX,
                max(MIN_SOFT_MAX_RADIUS_PX, shortest / REGION_SIDE_RADIUS_DIVISOR),
            )
        // Keep at least a light blur at 0% so the control always softens the area.
        return max(MIN_RADIUS_PX, (clamped * maxRadius) / MAX_PROGRESS)
    }
}
