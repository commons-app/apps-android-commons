package fr.free.nrw.commons.contributions;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class UnswipableViewPager extends ViewPager{
    public UnswipableViewPager(@NonNull Context context) {
        super(context);
    }

    public UnswipableViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // Unswipable
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Unswipable
        return false;
    }
}
