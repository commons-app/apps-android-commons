package fr.free.nrw.commons.explore;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.viewpager.widget.ViewPager;

public class ParentViewPager extends ViewPager {

  private boolean canScroll = true;

  public ParentViewPager(Context context) {
    super(context);
  }

  public ParentViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setCanScroll(boolean canScroll) {
    this.canScroll = canScroll;
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    return canScroll && super.onTouchEvent(ev);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    return canScroll && super.onInterceptTouchEvent(ev);
  }

  public boolean isCanScroll() {
    return canScroll;
  }
}
