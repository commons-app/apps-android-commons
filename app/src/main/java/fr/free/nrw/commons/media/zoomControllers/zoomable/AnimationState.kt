package fr.free.nrw.commons.media.zoomControllers.zoomable

import android.graphics.Matrix

/**
 * Holds the animation state and interpolation helpers used while animating a zoomable transform.
 *
 * <p>This separates the animation machinery (the animation flag, the start/stop/current matrix
 * values and the linear interpolation) from the gesture and transform handling in
 * [AbstractAnimatedZoomableController], so that each class has a single, cohesive responsibility.
 */
class AnimationState {
    private var isAnimating: Boolean = false
    private val startValues = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    private val stopValues = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    private val currentValues = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    private val newTransform = Matrix()
    private val workingTransform = Matrix()

    /**
     * Gets whether an animation is currently in progress.
     *
     * @return `true` if an animation is running, `false` otherwise
     */
    fun getIsAnimating(): Boolean = isAnimating

    /**
     * Sets whether an animation is currently in progress.
     *
     * @param isAnimating whether an animation is running
     */
    fun setAnimating(isAnimating: Boolean) {
        this.isAnimating = isAnimating
    }

    /**
     * Gets the start matrix values of the current animation.
     *
     * @return the start values as a float array
     */
    fun getStartValues(): FloatArray = startValues

    /**
     * Gets the stop (target) matrix values of the current animation.
     *
     * @return the stop values as a float array
     */
    fun getStopValues(): FloatArray = stopValues

    /**
     * Gets the working transform used while interpolating between the start and stop transforms.
     *
     * @return the working transform matrix
     */
    fun getWorkingTransform(): Matrix = workingTransform

    /**
     * Gets the new (target) transform that the animation is moving towards.
     *
     * @return the new transform matrix
     */
    fun getNewTransform(): Matrix = newTransform

    /**
     * Linearly interpolates between the start and stop matrix values and stores the result in
     * [outMatrix].
     *
     * @param outMatrix the matrix to store the interpolated values in
     * @param fraction the interpolation fraction, where 0 is the start and 1 is the stop
     */
    fun calculateInterpolation(outMatrix: Matrix, fraction: Float) {
        for (i in startValues.indices) {
            currentValues[i] = (1 - fraction) * startValues[i] + fraction * stopValues[i]
        }
        outMatrix.setValues(currentValues)
    }
}
