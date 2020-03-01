package fr.free.nrw.commons.contributions;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

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
