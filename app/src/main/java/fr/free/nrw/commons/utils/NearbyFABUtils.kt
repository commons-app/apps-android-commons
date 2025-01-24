package fr.free.nrw.commons.utils

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

object NearbyFABUtils {

    /*
     * Add anchors back before making them visible again.
     */
    @JvmStatic
    fun addAnchorToBigFABs(floatingActionButton: FloatingActionButton, anchorID: Int) {
        val params = CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.anchorId = anchorID
        params.anchorGravity = Gravity.TOP or Gravity.RIGHT or Gravity.END
        floatingActionButton.layoutParams = params
    }

    /*
     * Add anchors back before making them visible again. Big and small fabs have different anchor
     * gravities, therefore there are two methods.
     */
    @JvmStatic
    fun addAnchorToSmallFABs(floatingActionButton: FloatingActionButton, anchorID: Int) {
        val params = CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.anchorId = anchorID
        params.anchorGravity = Gravity.CENTER_HORIZONTAL
        floatingActionButton.layoutParams = params
    }

    /*
     * We are not able to hide FABs without removing anchors, this method removes anchors.
     */
    @JvmStatic
    fun removeAnchorFromFAB(floatingActionButton: FloatingActionButton) {
        // get rid of anchors
        // Somehow this was the only way https://stackoverflow.com/questions/32732932
        // floatingactionbutton-visible-for-sometime-even-if-visibility-is-set-to-gone
        val params = floatingActionButton.layoutParams as CoordinatorLayout.LayoutParams
        params.anchorId = View.NO_ID
        // If we don't set them to zero, then they become visible for a moment on upper left side
        params.width = 0
        params.height = 0
        floatingActionButton.layoutParams = params
    }
}
