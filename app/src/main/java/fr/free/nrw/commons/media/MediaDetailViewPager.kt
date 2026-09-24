package fr.free.nrw.commons.media

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

/**
 * View pager that allows media detail swiping to be temporarily disabled.
 */
class MediaDetailViewPager : ViewPager {
    private var pagerSwipeEnabled: Boolean = true

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    /**
     * Controls whether this view pager responds to swipe gestures.
     */
    fun setPagerSwipeEnabled(enabled: Boolean) {
        pagerSwipeEnabled = enabled
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!pagerSwipeEnabled) {
            return false
        }
        return super.onTouchEvent(event)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (!pagerSwipeEnabled) {
            return false
        }
        return super.onInterceptTouchEvent(event)
    }
}
