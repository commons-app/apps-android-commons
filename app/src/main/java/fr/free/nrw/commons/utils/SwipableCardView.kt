package fr.free.nrw.commons.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import timber.log.Timber;

/**
 * A card view which informs onSwipe events to its child
 */
public abstract class SwipableCardView extends CardView {
    float x1, x2;
    private static final float MINIMUM_THRESHOLD_FOR_SWIPE = 100;

    public SwipableCardView(@NonNull Context context) {
        super(context);
        interceptOnTouchListener();
    }

    public SwipableCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        interceptOnTouchListener();
    }

    public SwipableCardView(@NonNull Context context, @Nullable AttributeSet attrs,
        int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        interceptOnTouchListener();
    }

    private void interceptOnTouchListener() {
        this.setOnTouchListener((v, event) -> {
            boolean isSwipe = false;
            float deltaX = 0.0f;
            Timber.e(event.getAction() + "");
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x1 = event.getX();
                    break;
                case MotionEvent.ACTION_UP:
                    x2 = event.getX();
                    deltaX = x2 - x1;
                    if (deltaX < 0) {
                        //Right to left swipe
                        isSwipe = true;
                    } else if (deltaX > 0) {
                        //Left to right swipe
                        isSwipe = true;
                    }
                    break;
            }
            if (isSwipe && (pixelToDp(Math.abs(deltaX)) > MINIMUM_THRESHOLD_FOR_SWIPE)) {
                return onSwipe(v);
            }
            return false;
        });
    }

    /**
     * abstract function which informs swipe events to those who have inherited from it
     */
    public abstract boolean onSwipe(View view);

    private float pixelToDp(float pixels) {
        return (pixels / Resources.getSystem().getDisplayMetrics().density);
    }
}
