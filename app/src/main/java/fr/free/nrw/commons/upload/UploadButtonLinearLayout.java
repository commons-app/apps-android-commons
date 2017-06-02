package fr.free.nrw.commons.upload;


import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class UploadButtonLinearLayout extends LinearLayout {


    public UploadButtonLinearLayout(Context context) {
        super(context);
    }

    public UploadButtonLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public UploadButtonLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public UploadButtonLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        setMeasuredDimension(width, width);
    }
}