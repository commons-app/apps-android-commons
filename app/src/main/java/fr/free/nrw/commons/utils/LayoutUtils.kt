package fr.free.nrw.commons.utils

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewTreeObserver

/**
 * Utility class for layout-related operations.
 */
object LayoutUtils {

    /**
     * Can be used for keeping aspect ratios suggested by material guidelines. See:
     * https://material.io/design/layout/spacing-methods.html#containers-aspect-ratios
     * In some cases, we don't know the exact width, for such cases this method measures
     * width and sets height by multiplying the width with height.
     * @param rate Aspect ratios, i.e., 1 for 1:1 (width * rate = height)
     * @param view View to change height
     */
    @JvmStatic
    fun setLayoutHeightAlignedToWidth(rate: Double, view: View) {
        val vto = view.viewTreeObserver
        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val layoutParams = view.layoutParams
                layoutParams.height = (view.width * rate).toInt()
                view.layoutParams = layoutParams
            }
        })
    }

    /**
     * Calculates and returns the screen width multiplied by the provided rate.
     * @param context Context used to access display metrics.
     * @param rate Multiplier for screen width.
     * @return Calculated screen width multiplied by the rate.
     */
    @JvmStatic
    fun getScreenWidth(context: Context, rate: Double): Double {
        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels * rate
    }
}