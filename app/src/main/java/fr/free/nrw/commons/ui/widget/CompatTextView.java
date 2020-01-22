package fr.free.nrw.commons.ui.widget;

/*
 *Created by mikel on 07/08/2017.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatDrawableManager;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.ViewCompat;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.utils.UiUtils;

/**
 * a text view compatible with older versions of the platform
 */
public class CompatTextView extends AppCompatTextView {

    /**
     * Constructs a new instance of CompatTextView
     *
     * @param context the view context
     */
    public CompatTextView(Context context) {
        super(context);
        init(null);
    }

    /**
     * Constructs a new instance of CompatTextView
     *
     * @param context the view context
     * @param attrs   the set of attributes for the view
     */
    public CompatTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    /**
     * Constructs a new instance of CompatTextView
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public CompatTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    /**
     * initializes the view
     *
     * @param attrs the attribute set of the view, which can be null
     */
    private void init(@Nullable AttributeSet attrs) {
        if (attrs != null) {
            Context context = getContext();
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CompatTextView);

            // Obtain DrawableManager used to pull Drawables safely, and check if we're in RTL
            AppCompatDrawableManager dm = AppCompatDrawableManager.get();
            boolean rtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;

            // Grab the compat drawable padding from the XML
            float drawablePadding = a.getDimension(R.styleable.CompatTextView_drawablePadding, 0);

            // Grab the compat drawable resources from the XML
            int startDrawableRes = a.getResourceId(R.styleable.CompatTextView_drawableStart, 0);
            int topDrawableRes = a.getResourceId(R.styleable.CompatTextView_drawableTop, 0);
            int endDrawableRes = a.getResourceId(R.styleable.CompatTextView_drawableEnd, 0);
            int bottomDrawableRes = a.getResourceId(R.styleable.CompatTextView_drawableBottom, 0);

            // Load the used drawables, fall back to whatever was set in an "android:"
            Drawable[] currentDrawables = getCompoundDrawables();
            Drawable left = startDrawableRes != 0
                    ? dm.getDrawable(context, startDrawableRes) : currentDrawables[0];
            Drawable right = endDrawableRes != 0
                    ? dm.getDrawable(context, endDrawableRes) : currentDrawables[1];
            Drawable top = topDrawableRes != 0
                    ? dm.getDrawable(context, topDrawableRes) : currentDrawables[2];
            Drawable bottom = bottomDrawableRes != 0
                    ? dm.getDrawable(context, bottomDrawableRes) : currentDrawables[3];

            // Account for RTL and apply the compound Drawables
            Drawable start = rtl ? right : left;
            Drawable end = rtl ? left : right;
            setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
            setCompoundDrawablePadding((int) UiUtils.convertDpToPixel(drawablePadding, getContext()));

            a.recycle();
        }
    }
}
