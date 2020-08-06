package fr.free.nrw.commons.nearby

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView


class CustomBorderTextView : AppCompatTextView{
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onDraw(canvas: Canvas?) {
        val strokePaint = Paint()
        strokePaint.setARGB(255, 0, 0, 0)
        strokePaint.textAlign = Paint.Align.CENTER
        strokePaint.typeface = Typeface.DEFAULT_BOLD
        strokePaint.style = Paint.Style.STROKE
        strokePaint.color=context.resources.getColor(android.R.color.black)
        strokePaint.strokeWidth = 20f
        val textPaint = Paint()
        textPaint.setARGB(255, 255, 255, 255)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.color=context.resources.getColor(android.R.color.white)
        canvas?.drawText(text.toString(), 100f, 100f, strokePaint)
        canvas?.drawText(text.toString(), 100f, 100f, textPaint)
        super.onDraw(canvas)
    }
}