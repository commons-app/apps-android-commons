package fr.free.nrw.commons.contributions

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class UnswipableViewPager : ViewPager {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        // Unswipable
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Unswipable
        return false
    }
}
