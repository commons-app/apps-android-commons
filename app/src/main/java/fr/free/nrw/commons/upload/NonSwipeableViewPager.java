package fr.free.nrw.commons.upload;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class NonSwipeableViewPager extends ViewPager {

    public NonSwipeableViewPager(Context context) {
        super(context);
    }


    /**
     * Is swipe enabled
     */
    private boolean enabled;

    public NonSwipeableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.enabled = false; // By default swiping is disabled
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return this.enabled ? super.onTouchEvent(event) : false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.enabled ? super.onInterceptTouchEvent(event) : false;
    }

    @Override
    public boolean executeKeyEvent(KeyEvent event) {
        return this.enabled ? super.executeKeyEvent(event) : false;
    }

    public void setSwipeEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}