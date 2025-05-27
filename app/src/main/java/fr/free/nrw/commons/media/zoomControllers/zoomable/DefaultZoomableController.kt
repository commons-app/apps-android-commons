package fr.free.nrw.commons.media.zoomControllers.zoomable

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import androidx.annotation.IntDef
import com.facebook.common.logging.FLog
import fr.free.nrw.commons.media.zoomControllers.gestures.TransformGestureDetector
import kotlin.math.abs

/** Zoomable controller that calculates transformation based on touch events. */
open class DefaultZoomableController(
    private val mGestureDetector: TransformGestureDetector
) : ZoomableController, TransformGestureDetector.Listener {

    /** Interface for handling call backs when the image bounds are set. */
    fun interface ImageBoundsListener {
        fun onImageBoundsSet(imageBounds: RectF)
    }

    @IntDef(
        flag = true,
        value = [LIMIT_NONE, LIMIT_TRANSLATION_X, LIMIT_TRANSLATION_Y, LIMIT_SCALE, LIMIT_ALL]
    )
    @Retention
    annotation class LimitFlag

    companion object {
        const val LIMIT_NONE = 0
        const val LIMIT_TRANSLATION_X = 1
        const val LIMIT_TRANSLATION_Y = 2
        const val LIMIT_SCALE = 4
        const val LIMIT_ALL = LIMIT_TRANSLATION_X or LIMIT_TRANSLATION_Y or LIMIT_SCALE

        private const val EPS = 1e-3f

        private val TAG: Class<*> = DefaultZoomableController::class.java

        private val IDENTITY_RECT = RectF(0f, 0f, 1f, 1f)

        fun newInstance(): DefaultZoomableController {
            return DefaultZoomableController(TransformGestureDetector.newInstance())
        }
    }

    private var mImageBoundsListener: ImageBoundsListener? = null

    private var mListener: ZoomableController.Listener? = null

    private var mIsEnabled = false
    private var mIsRotationEnabled = false
    private var mIsScaleEnabled = true
    private var mIsTranslationEnabled = true
    private var mIsGestureZoomEnabled = true

    private var mMinScaleFactor = 1.0f
    private var mMaxScaleFactor = 2.0f

    // View bounds, in view-absolute coordinates
    private val mViewBounds = RectF()
    // Non-transformed image bounds, in view-absolute coordinates
    private val mImageBounds = RectF()
    // Transformed image bounds, in view-absolute coordinates
    private val mTransformedImageBounds = RectF()

    private val mPreviousTransform = Matrix()
    private val mActiveTransform = Matrix()
    private val mActiveTransformInverse = Matrix()
    private val mTempValues = FloatArray(9)
    private val mTempRect = RectF()
    private var mWasTransformCorrected = false

    init {
        mGestureDetector.setListener(this)
    }

    /** Rests the controller. */
    open fun reset() {
        FLog.v(TAG, "reset")
        mGestureDetector.reset()
        mPreviousTransform.reset()
        mActiveTransform.reset()
        onTransformChanged()
    }

    /** Sets the zoomable listener. */
    override fun setListener(listener: ZoomableController.Listener?) {
        mListener = listener
    }

    /** Sets whether the controller is enabled or not. */
    override fun setEnabled(enabled: Boolean) {
        mIsEnabled = enabled
        if (!enabled) {
            reset()
        }
    }

    /** Gets whether the controller is enabled or not. */
    override fun isEnabled(): Boolean {
        return mIsEnabled
    }

    /** Sets whether the rotation gesture is enabled or not. */
    fun setRotationEnabled(enabled: Boolean) {
        mIsRotationEnabled = enabled
    }

    /** Gets whether the rotation gesture is enabled or not. */
    fun isRotationEnabled(): Boolean {
        return mIsRotationEnabled
    }

    /** Sets whether the scale gesture is enabled or not. */
    fun setScaleEnabled(enabled: Boolean) {
        mIsScaleEnabled = enabled
    }

    /** Gets whether the scale gesture is enabled or not. */
    fun isScaleEnabled(): Boolean {
        return mIsScaleEnabled
    }

    /** Sets whether the translation gesture is enabled or not. */
    fun setTranslationEnabled(enabled: Boolean) {
        mIsTranslationEnabled = enabled
    }

    /** Gets whether the translations gesture is enabled or not. */
    fun isTranslationEnabled(): Boolean {
        return mIsTranslationEnabled
    }

    /**
     * Sets the minimum scale factor allowed.
     *
     * <p>Hierarchy's scaling (if any) is not taken into account.
     */
    fun setMinScaleFactor(minScaleFactor: Float) {
        mMinScaleFactor = minScaleFactor
    }

    /** Gets the minimum scale factor allowed. */
    fun getMinScaleFactor(): Float {
        return mMinScaleFactor
    }

    /**
     * Sets the maximum scale factor allowed.
     *
     * <p>Hierarchy's scaling (if any) is not taken into account.
     */
    fun setMaxScaleFactor(maxScaleFactor: Float) {
        mMaxScaleFactor = maxScaleFactor
    }

    /** Gets the maximum scale factor allowed. */
    fun getMaxScaleFactor(): Float {
        return mMaxScaleFactor
    }

    /** Sets whether gesture zooms are enabled or not. */
    fun setGestureZoomEnabled(isGestureZoomEnabled: Boolean) {
        mIsGestureZoomEnabled = isGestureZoomEnabled
    }

    /** Gets whether gesture zooms are enabled or not. */
    fun isGestureZoomEnabled(): Boolean {
        return mIsGestureZoomEnabled
    }

    /** Gets the current scale factor. */
    override fun getScaleFactor(): Float {
        return getMatrixScaleFactor(mActiveTransform)
    }

    /** Sets the image bounds, in view-absolute coordinates. */
    override fun setImageBounds(imageBounds: RectF) {
        if (imageBounds != mImageBounds) {
            mImageBounds.set(imageBounds)
            onTransformChanged()
            mImageBoundsListener?.onImageBoundsSet(mImageBounds)
        }
    }

    /** Gets the non-transformed image bounds, in view-absolute coordinates. */
    fun getImageBounds(): RectF {
        return mImageBounds
    }

    /** Gets the transformed image bounds, in view-absolute coordinates */
    private fun getTransformedImageBounds(): RectF {
        return mTransformedImageBounds
    }

    /** Sets the view bounds. */
    override fun setViewBounds(viewBounds: RectF) {
        mViewBounds.set(viewBounds)
    }

    /** Gets the view bounds. */
    fun getViewBounds(): RectF {
        return mViewBounds
    }

    /** Sets the image bounds listener. */
    fun setImageBoundsListener(imageBoundsListener: ImageBoundsListener?) {
        mImageBoundsListener = imageBoundsListener
    }

    /** Gets the image bounds listener. */
    fun getImageBoundsListener(): ImageBoundsListener? {
        return mImageBoundsListener
    }

    /** Returns true if the zoomable transform is identity matrix. */
    override fun isIdentity(): Boolean {
        return isMatrixIdentity(mActiveTransform, 1e-3f)
    }

    /**
     * Returns true if the transform was corrected during the last update.
     *
     * <p>We should rename this method to `wasTransformedWithoutCorrection` and just return the
     * internal flag directly. However, this requires interface change and negation of meaning.
     */
    override fun wasTransformCorrected(): Boolean {
        return mWasTransformCorrected
    }

    /**
     * Gets the matrix that transforms image-absolute coordinates to view-absolute coordinates. The
     * zoomable transformation is taken into account.
     *
     * <p>Internal matrix is exposed for performance reasons and is not to be modified by the
     * callers.
     */
    override fun getTransform(): Matrix {
        return mActiveTransform
    }

    /**
     * Gets the matrix that transforms image-relative coordinates to view-absolute coordinates. The
     * zoomable transformation is taken into account.
     */
    fun getImageRelativeToViewAbsoluteTransform(outMatrix: Matrix) {
        outMatrix.setRectToRect(IDENTITY_RECT, mTransformedImageBounds, Matrix.ScaleToFit.FILL)
    }

    /**
     * Maps point from view-absolute to image-relative coordinates. This takes into account the
     * zoomable transformation.
     */
    fun mapViewToImage(viewPoint: PointF): PointF {
        val points = mTempValues
        points[0] = viewPoint.x
        points[1] = viewPoint.y
        mActiveTransform.invert(mActiveTransformInverse)
        mActiveTransformInverse.mapPoints(points, 0, points, 0, 1)
        mapAbsoluteToRelative(points, points, 1)
        return PointF(points[0], points[1])
    }

    /**
     * Maps point from image-relative to view-absolute coordinates. This takes into account the
     * zoomable transformation.
     */
    fun mapImageToView(imagePoint: PointF): PointF {
        val points = mTempValues
        points[0] = imagePoint.x
        points[1] = imagePoint.y
        mapRelativeToAbsolute(points, points, 1)
        mActiveTransform.mapPoints(points, 0, points, 0, 1)
        return PointF(points[0], points[1])
    }

    /**
     * Maps array of 2D points from view-absolute to image-relative coordinates. This does NOT take
     * into account the zoomable transformation. Points are represented by a float array of [x0, y0,
     * x1, y1, ...].
     *
     * @param destPoints destination array (may be the same as source array)
     * @param srcPoints source array
     * @param numPoints number of points to map
     */
    private fun mapAbsoluteToRelative(
        destPoints: FloatArray,
        srcPoints: FloatArray,
        numPoints: Int
    ) {
        for (i in 0 until numPoints) {
            destPoints[i * 2] = (srcPoints[i * 2] - mImageBounds.left) / mImageBounds.width()
            destPoints[i * 2 + 1] =
                (srcPoints[i * 2 + 1] - mImageBounds.top) / mImageBounds.height()
        }
    }

    /**
     * Maps array of 2D points from image-relative to view-absolute coordinates. This does NOT take
     * into account the zoomable transformation. Points are represented by float array of
     * [x0, y0, x1, y1, ...].
     *
     * @param destPoints destination array (may be the same as source array)
     * @param srcPoints source array
     * @param numPoints number of points to map
     */
    private fun mapRelativeToAbsolute(
        destPoints: FloatArray,
        srcPoints: FloatArray,
        numPoints: Int
    ) {
        for (i in 0 until numPoints) {
            destPoints[i * 2] = srcPoints[i * 2] * mImageBounds.width() + mImageBounds.left
            destPoints[i * 2 + 1] = srcPoints[i * 2 + 1] * mImageBounds.height() + mImageBounds.top
        }
    }

    /**
     * Zooms to the desired scale and positions the image so that the given image point
     * corresponds to the given view point.
     *
     * @param scale desired scale, will be limited to {min, max} scale factor
     * @param imagePoint 2D point in image's relative coordinate system (i.e. 0 <= x, y <= 1)
     * @param viewPoint 2D point in view's absolute coordinate system
     */
    open fun zoomToPoint(scale: Float, imagePoint: PointF, viewPoint: PointF) {
        FLog.v(TAG, "zoomToPoint")
        calculateZoomToPointTransform(mActiveTransform, scale, imagePoint, viewPoint, LIMIT_ALL)
        onTransformChanged()
    }

    /**
     * Calculates the zoom transformation that would zoom to the desired scale and position
     * the image so that the given image point corresponds to the given view point.
     *
     * @param outTransform the matrix to store the result to
     * @param scale desired scale, will be limited to {min, max} scale factor
     * @param imagePoint 2D point in image's relative coordinate system (i.e. 0 <= x, y <= 1)
     * @param viewPoint 2D point in view's absolute coordinate system
     * @param limitFlags whether to limit translation and/or scale.
     * @return whether or not the transform has been corrected due to limitation
     */
    protected fun calculateZoomToPointTransform(
        outTransform: Matrix,
        scale: Float,
        imagePoint: PointF,
        viewPoint: PointF,
        @LimitFlag limitFlags: Int
    ): Boolean {
        val viewAbsolute = mTempValues
        viewAbsolute[0] = imagePoint.x
        viewAbsolute[1] = imagePoint.y
        mapRelativeToAbsolute(viewAbsolute, viewAbsolute, 1)
        val distanceX = viewPoint.x - viewAbsolute[0]
        val distanceY = viewPoint.y - viewAbsolute[1]
        var transformCorrected = false
        outTransform.setScale(scale, scale, viewAbsolute[0], viewAbsolute[1])
        transformCorrected = transformCorrected or
                limitScale(outTransform, viewAbsolute[0], viewAbsolute[1], limitFlags)
        outTransform.postTranslate(distanceX, distanceY)
        transformCorrected = transformCorrected or limitTranslation(outTransform, limitFlags)
        return transformCorrected
    }

    /** Sets a new zoom transformation. */
    fun setTransform(newTransform: Matrix) {
        FLog.v(TAG, "setTransform")
        mActiveTransform.set(newTransform)
        onTransformChanged()
    }

    /** Gets the gesture detector. */
    protected fun getDetector(): TransformGestureDetector {
        return mGestureDetector
    }

    /** Notifies controller of the received touch event. */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        FLog.v(TAG, "onTouchEvent: action: ", event.action)
        return if (mIsEnabled && mIsGestureZoomEnabled) {
            mGestureDetector.onTouchEvent(event)
        } else {
            false
        }
    }

    /* TransformGestureDetector.Listener methods  */

    override fun onGestureBegin(detector: TransformGestureDetector) {
        FLog.v(TAG, "onGestureBegin")
        mPreviousTransform.set(mActiveTransform)
        onTransformBegin()
        mWasTransformCorrected = !canScrollInAllDirection()
    }

    override fun onGestureUpdate(detector: TransformGestureDetector) {
        FLog.v(TAG, "onGestureUpdate")
        val transformCorrected = calculateGestureTransform(mActiveTransform, LIMIT_ALL)
        onTransformChanged()
        if (transformCorrected) {
            mGestureDetector.restartGesture()
        }
        mWasTransformCorrected = transformCorrected
    }

    override fun onGestureEnd(detector: TransformGestureDetector) {
        FLog.v(TAG, "onGestureEnd")
        onTransformEnd()
    }

    /**
     * Calculates the zoom transformation based on the current gesture.
     *
     * @param outTransform the matrix to store the result to
     * @param limitTypes whether to limit translation and/or scale.
     * @return whether or not the transform has been corrected due to limitation
     */
    private fun calculateGestureTransform(
        outTransform: Matrix,
        @LimitFlag limitTypes: Int
    ): Boolean {
        val detector = mGestureDetector
        var transformCorrected = false
        outTransform.set(mPreviousTransform)
        if (mIsRotationEnabled) {
            val angle = detector.getRotation() * (180 / Math.PI).toFloat()
            outTransform.postRotate(angle, detector.getPivotX(), detector.getPivotY())
        }
        if (mIsScaleEnabled) {
            val scale = detector.getScale()
            outTransform.postScale(scale, scale, detector.getPivotX(), detector.getPivotY())
        }
        transformCorrected = transformCorrected or limitScale(
            outTransform,
            detector.getPivotX(),
            detector.getPivotY(),
            limitTypes
        )
        if (mIsTranslationEnabled) {
            outTransform.postTranslate(detector.getTranslationX(), detector.getTranslationY())
        }
        transformCorrected = transformCorrected or limitTranslation(outTransform, limitTypes)
        return transformCorrected
    }

    private fun onTransformBegin() {
        if (mListener != null && isEnabled()) {
            mListener?.onTransformBegin(mActiveTransform)
        }
    }

    private fun onTransformChanged() {
        mActiveTransform.mapRect(mTransformedImageBounds, mImageBounds)
        if (mListener != null && isEnabled()) {
            mListener?.onTransformChanged(mActiveTransform)
        }
    }

    private fun onTransformEnd() {
        if (mListener != null && isEnabled()) {
            mListener?.onTransformEnd(mActiveTransform)
        }
    }

    /**
     * Keeps the scaling factor within the specified limits.
     *
     * @param pivotX x coordinate of the pivot point
     * @param pivotY y coordinate of the pivot point
     * @param limitTypes whether to limit scale.
     * @return whether limiting has been applied or not
     */
    private fun limitScale(
        transform: Matrix, pivotX: Float, pivotY: Float, @LimitFlag limitTypes: Int
    ): Boolean {
        if (!shouldLimit(limitTypes, LIMIT_SCALE)) {
            return false
        }
        val currentScale = getMatrixScaleFactor(transform)
        val targetScale = limit(currentScale, mMinScaleFactor, mMaxScaleFactor)
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
    private fun limitTranslation(transform: Matrix, @LimitFlag limitTypes: Int): Boolean {
        if (!shouldLimit(limitTypes, LIMIT_TRANSLATION_X or LIMIT_TRANSLATION_Y)) {
            return false
        }
        val b = mTempRect
        b.set(mImageBounds)
        transform.mapRect(b)
        val offsetLeft =
            if (shouldLimit(limitTypes, LIMIT_TRANSLATION_X)) getOffset(
                b.left, b.right, mViewBounds.left, mViewBounds.right, mImageBounds.centerX()
            ) else 0f
        val offsetTop =
            if (shouldLimit(limitTypes, LIMIT_TRANSLATION_Y)) getOffset(
                b.top, b.bottom, mViewBounds.top, mViewBounds.bottom, mImageBounds.centerY()
            ) else 0f

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
    private fun shouldLimit(@LimitFlag limits: Int, @LimitFlag flag: Int): Boolean {
        return (limits and flag) != LIMIT_NONE
    }

    /**
     * Returns the offset necessary to make sure that:
     * - The image is centered if it's smaller than the limit
     * - There is no empty space if the image is bigger than the limit
     */
    private fun getOffset(
        imageStart: Float, imageEnd: Float, limitStart: Float, limitEnd: Float, limitCenter: Float
    ): Float {
        val imageWidth = imageEnd - imageStart
        val limitWidth = limitEnd - limitStart
        val limitInnerWidth = minOf(limitCenter - limitStart, limitEnd - limitCenter) * 2

        return when {
            imageWidth < limitInnerWidth -> limitCenter - (imageEnd + imageStart) / 2
            imageWidth < limitWidth -> if (limitCenter < (limitStart + limitEnd) / 2) {
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
    private fun limit(value: Float, min: Float, max: Float): Float {
        return min.coerceAtLeast(value).coerceAtMost(max)
    }

    /**
     * Gets the scale factor for the given matrix. Assumes equal scaling for X and Y axis.
     */
    private fun getMatrixScaleFactor(transform: Matrix): Float {
        transform.getValues(mTempValues)
        return mTempValues[Matrix.MSCALE_X]
    }

    /** Checks if the matrix is an identity matrix within a given tolerance `eps`. */
    private fun isMatrixIdentity(transform: Matrix, eps: Float): Boolean {
        transform.getValues(mTempValues)
        mTempValues[0] -= 1.0f // m00
        mTempValues[4] -= 1.0f // m11
        mTempValues[8] -= 1.0f // m22
        return mTempValues.all { abs(it) <= eps }
    }

    /** Returns whether the scroll can happen in all directions. */
    private fun canScrollInAllDirection(): Boolean {
        return mTransformedImageBounds.left < mViewBounds.left - EPS &&
                mTransformedImageBounds.top < mViewBounds.top - EPS &&
                mTransformedImageBounds.right > mViewBounds.right + EPS &&
                mTransformedImageBounds.bottom > mViewBounds.bottom + EPS
    }

    override fun computeHorizontalScrollRange(): Int {
        return mTransformedImageBounds.width().toInt()
    }

    override fun computeHorizontalScrollOffset(): Int {
        return (mViewBounds.left - mTransformedImageBounds.left).toInt()
    }

    override fun computeHorizontalScrollExtent(): Int {
        return mViewBounds.width().toInt()
    }

    override fun computeVerticalScrollRange(): Int {
        return mTransformedImageBounds.height().toInt()
    }

    override fun computeVerticalScrollOffset(): Int {
        return (mViewBounds.top - mTransformedImageBounds.top).toInt()
    }

    override fun computeVerticalScrollExtent(): Int {
        return mViewBounds.height().toInt()
    }

    fun getListener(): ZoomableController.Listener? {
        return mListener
    }
}
