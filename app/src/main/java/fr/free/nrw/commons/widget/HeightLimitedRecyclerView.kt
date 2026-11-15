package fr.free.nrw.commons.widget

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics

import androidx.annotation.Nullable
import androidx.recyclerview.widget.RecyclerView


/**
 * Created by Ilgaz Er on 8/7/2018.
 */
class HeightLimitedRecyclerView : RecyclerView {
    private var height: Int = 0

    constructor(context: Context) : super(context) {
        initializeHeight(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initializeHeight(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        initializeHeight(context)
    }

    private fun initializeHeight(context: Context) {
        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        height = displayMetrics.heightPixels
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val limitedHeightSpec = MeasureSpec.makeMeasureSpec(
            (height * 0.3).toInt(),
            MeasureSpec.AT_MOST
        )
        super.onMeasure(widthSpec, limitedHeightSpec)
    }
}
