package fr.free.nrw.commons.media.zoomControllers.zoomable

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Animatable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent

import androidx.core.view.ScrollingView
import com.facebook.common.internal.Preconditions
import com.facebook.common.logging.FLog
import com.facebook.drawee.controller.AbstractDraweeController
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.generic.GenericDraweeHierarchy
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.generic.GenericDraweeHierarchyInflater
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.drawee.view.DraweeView

/**
 * DraweeView that has zoomable capabilities.
 *
 * <p>Once the image loads, pinch-to-zoom and translation gestures are enabled.
 */
open class ZoomableDraweeView : DraweeView<GenericDraweeHierarchy>, ScrollingView {

    companion object {
        private val TAG = ZoomableDraweeView::class.java
        private const val HUGE_IMAGE_SCALE_FACTOR_THRESHOLD = 1.1f
    }

    private val imageBounds = RectF()
    private val viewBounds = RectF()

    private var hugeImageController: DraweeController? = null
    private var zoomableController: ZoomableController = createZoomableController()
    private var tapGestureDetector: GestureDetector? = null
    private var allowTouchInterceptionWhileZoomed = true
    private var isDialToneEnabled = false
    private var zoomingEnabled = true
    private var transformationListener: TransformationListener? = null

    private val controllerListener = object : BaseControllerListener<Any>() {
        override fun onFinalImageSet(id: String, imageInfo: Any?, animatable: Animatable?) {
            this@ZoomableDraweeView.onFinalImageSet()
        }

        override fun onRelease(id: String) {
            this@ZoomableDraweeView.onRelease()
        }
    }

    private val zoomableListener = object : ZoomableController.Listener {
        override fun onTransformBegin(transform: Matrix) {}

        override fun onTransformChanged(transform: Matrix) {
            this@ZoomableDraweeView.onTransformChanged(transform)
        }

        override fun onTransformEnd(transform: Matrix) {
            transformationListener?.onTransformationEnd()
        }
    }

    private val tapListenerWrapper = GestureListenerWrapper()

    constructor(context: Context, hierarchy: GenericDraweeHierarchy) : super(context) {
        setHierarchy(hierarchy)
        init()
    }

    constructor(context: Context) : super(context) {
        inflateHierarchy(context, null)
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        inflateHierarchy(context, attrs)
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int)
            : super(context, attrs, defStyle) {
        inflateHierarchy(context, attrs)
        init()
    }

    fun setTransformationListener(transformationListener: TransformationListener) {
        this.transformationListener = transformationListener
    }

    protected fun inflateHierarchy(context: Context, attrs: AttributeSet?) {
        val resources: Resources = context.resources
        val builder = GenericDraweeHierarchyBuilder(resources)
            .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
        GenericDraweeHierarchyInflater.updateBuilder(builder, context, attrs)
        aspectRatio = builder.desiredAspectRatio
        setHierarchy(builder.build())
    }

    private fun init() {
        zoomableController.setListener(zoomableListener)
        tapGestureDetector = GestureDetector(context, tapListenerWrapper)
    }

    fun setIsDialToneEnabled(isDialtoneEnabled: Boolean) {
        this.isDialToneEnabled = isDialtoneEnabled
    }

    protected fun getImageBounds(outBounds: RectF) {
        hierarchy.getActualImageBounds(outBounds)
    }

    protected fun getLimitBounds(outBounds: RectF) {
        outBounds.set(0f, 0f, width.toFloat(), height.toFloat())
    }

    fun setZoomableController(zoomableController: ZoomableController) {
        Preconditions.checkNotNull(zoomableController)
        this.zoomableController.setListener(null)
        this.zoomableController = zoomableController
        this.zoomableController.setListener(zoomableListener)
    }

    fun getZoomableController(): ZoomableController = zoomableController

    fun allowsTouchInterceptionWhileZoomed(): Boolean = allowTouchInterceptionWhileZoomed

    fun setAllowTouchInterceptionWhileZoomed(allow: Boolean) {
        allowTouchInterceptionWhileZoomed = allow
    }

    fun setTapListener(tapListener: GestureDetector.SimpleOnGestureListener) {
        tapListenerWrapper.setListener(tapListener)
    }

    fun setIsLongpressEnabled(enabled: Boolean) {
        tapGestureDetector?.setIsLongpressEnabled(enabled)
    }

    fun setZoomingEnabled(zoomingEnabled: Boolean) {
        this.zoomingEnabled = zoomingEnabled
        zoomableController.setEnabled(false)
    }

    override fun setController(controller: DraweeController?) {
        setControllers(controller, null)
    }

    fun setControllers(controller: DraweeController?, hugeImageController: DraweeController?) {
        setControllersInternal(null, null)
        zoomableController.setEnabled(false)
        setControllersInternal(controller, hugeImageController)
    }

    private fun setControllersInternal(
        controller: DraweeController?,
        hugeImageController: DraweeController?
    ) {
        removeControllerListener(getController())
        addControllerListener(controller)
        this.hugeImageController = hugeImageController
        super.setController(controller)
    }

    private fun maybeSetHugeImageController() {
        if (
            hugeImageController != null
            &&
            zoomableController.getScaleFactor() > HUGE_IMAGE_SCALE_FACTOR_THRESHOLD
        ) {
            setControllersInternal(hugeImageController, null)
        }
    }

    private fun removeControllerListener(controller: DraweeController?) {
        if (controller is AbstractDraweeController<*, *>) {
            controller.removeControllerListener(controllerListener)
        }
    }

    private fun addControllerListener(controller: DraweeController?) {
        if (controller is AbstractDraweeController<*, *>) {
            controller.addControllerListener(controllerListener)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val saveCount = canvas.save()
        canvas.concat(zoomableController.getTransform())
        try {
            super.onDraw(canvas)
        } catch (e: Exception) {
            val controller = controller
            if (controller is AbstractDraweeController<*, *>) {
                val callerContext = controller.callerContext
                if (callerContext != null) {
                    throw RuntimeException("Exception in onDraw, callerContext=${callerContext}", e)
                }
            }
            throw e
        }
        canvas.restoreToCount(saveCount)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var action = event.actionMasked
        FLog.v(getLogTag(), "onTouchEvent: $action, view ${hashCode()}, received")

        if (!isDialToneEnabled && tapGestureDetector?.onTouchEvent(event) == true) {
            FLog.v(
                getLogTag(),
                "onTouchEvent: $action, view ${hashCode()}, handled by tap gesture detector"
            )
            return true
        }

        if (!isDialToneEnabled && zoomableController.onTouchEvent(event)) {
            FLog.v(
                getLogTag(),
                "onTouchEvent: $action, view ${hashCode()}, handled by zoomable controller"
            )
            if (!allowTouchInterceptionWhileZoomed && !zoomableController.isIdentity()) {
                parent.requestDisallowInterceptTouchEvent(true)
            }
            return true
        }

        if (super.onTouchEvent(event)) {
            FLog.v(
                getLogTag(),
                "onTouchEvent: $action, view ${hashCode()}, handled by the super"
            )
            return true
        }

        // If none of our components handled the event, we send a cancel event to avoid unwanted actions.
        val cancelEvent = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
        tapGestureDetector?.onTouchEvent(cancelEvent)
        zoomableController.onTouchEvent(cancelEvent)
        cancelEvent.recycle()

        return false
    }

    override fun computeHorizontalScrollRange(): Int =
        zoomableController.computeHorizontalScrollRange()

    override fun computeHorizontalScrollOffset(): Int =
        zoomableController.computeHorizontalScrollOffset()

    override fun computeHorizontalScrollExtent(): Int =
        zoomableController.computeHorizontalScrollExtent()

    override fun computeVerticalScrollRange(): Int =
        zoomableController.computeVerticalScrollRange()

    override fun computeVerticalScrollOffset(): Int =
        zoomableController.computeVerticalScrollOffset()

    override fun computeVerticalScrollExtent(): Int =
        zoomableController.computeVerticalScrollExtent()


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        FLog.v(getLogTag(), "onLayout: view ${hashCode()}")
        super.onLayout(changed, left, top, right, bottom)
        updateZoomableControllerBounds()
    }

    private fun onFinalImageSet() {
        FLog.v(getLogTag(), "onFinalImageSet: view ${hashCode()}")
        if (!zoomableController.isEnabled() && zoomingEnabled) {
            zoomableController.setEnabled(true)
            updateZoomableControllerBounds()
        }
    }

    private fun onRelease() {
        FLog.v(getLogTag(), "onRelease: view ${hashCode()}")
        zoomableController.setEnabled(false)
    }

    protected fun onTransformChanged(transform: Matrix) {
        FLog.v(getLogTag(), "onTransformChanged: view ${hashCode()}, transform: $transform")
        maybeSetHugeImageController()
        invalidate()
    }

    protected fun updateZoomableControllerBounds() {
        getImageBounds(imageBounds)
        getLimitBounds(viewBounds)
        zoomableController.setImageBounds(imageBounds)
        zoomableController.setViewBounds(viewBounds)

        FLog.v(
            getLogTag(),
            "updateZoomableControllerBounds: view ${hashCode()}, " +
                    "view bounds: $viewBounds, image bounds: $imageBounds"
        )
    }

    protected fun getLogTag(): Class<*> = TAG

    protected fun createZoomableController(): ZoomableController = AnimatedZoomableController.newInstance()

    /**
     * Interface to listen for scale change events.
     */
    interface TransformationListener {
        fun onTransformationEnd()
    }
}