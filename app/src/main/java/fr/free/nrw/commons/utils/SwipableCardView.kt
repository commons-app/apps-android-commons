package fr.free.nrw.commons.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

import androidx.cardview.widget.CardView

import timber.log.Timber
import kotlin.math.abs

/**
 * A card view which informs onSwipe events to its child
 */
abstract class SwipableCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {


    companion object{
        const val MINIMUM_THRESHOLD_FOR_SWIPE = 100f
    }

    private var x1 = 0f
    private var x2 = 0f


    init {
        interceptOnTouchListener()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun interceptOnTouchListener() {
        this.setOnTouchListener { v, event ->
            var isSwipe = false
            var deltaX = 0f
            Timber.e(event.action.toString())
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    x1 = event.x
                }
                MotionEvent.ACTION_UP -> {
                    x2 = event.x
                    deltaX = x2 - x1
                    isSwipe = deltaX != 0f
                }
            }
            if (isSwipe && pixelToDp(abs(deltaX)) > MINIMUM_THRESHOLD_FOR_SWIPE) {
                onSwipe(v)
                return@setOnTouchListener true
            }
            false
        }
    }

    /**
     * abstract function which informs swipe events to those who have inherited from it
     */
    abstract fun onSwipe(view: View): Boolean

    private fun pixelToDp(pixels: Float): Float {
        return pixels / Resources.getSystem().displayMetrics.density
    }
}
