package fr.free.nrw.commons.review;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.viewpager.widget.ViewPager;

public class ReviewViewPager extends ViewPager {

  public ReviewViewPager(Context context) {
    super(context);
  }

  public ReviewViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    // Never allow swiping to switch between pages
    return false;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    // Never allow swiping to switch between pages
    return false;
  }
}
