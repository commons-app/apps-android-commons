/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.Interpolator
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import com.nguyenhoanglam.imagepicker.R

class SnackBarView : RelativeLayout {

    private lateinit var messageText: TextView
    private lateinit var actionButton: Button
    var isShowing = false
        private set

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        View.inflate(context, R.layout.imagepicker_snackbar, this)
        if (isInEditMode) {
            return
        }
        setBackgroundColor(Color.parseColor("#323232"))
        translationY = height.toFloat()
        alpha = 0f
        isShowing = false
        val horizontalPadding = convertDpToPixels(context, 24f)
        val verticalPadding = convertDpToPixels(context, 14f)
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        messageText = findViewById(R.id.text_snackbar_message)
        actionButton = findViewById(R.id.button_snackbar_action)
    }

    private fun setText(textResId: Int) {
        messageText.setText(textResId)
    }

    private fun setOnActionClickListener(actionText: String, onClickListener: OnClickListener) {
        actionButton.text = actionText
        actionButton.setOnClickListener { view ->
            hide(Runnable { onClickListener.onClick(view) })
        }
    }

    fun show(textResId: Int, onClickListener: OnClickListener) {
        setText(textResId)
        setOnActionClickListener(context.getString(R.string.imagepicker_action_ok), onClickListener)
        ViewCompat.animate(this)
            .translationY(0f)
            .setDuration(ANIM_DURATION.toLong())
            .setInterpolator(INTERPOLATOR)
            .alpha(1f)
        isShowing = true
    }

    private fun hide(runnable: Runnable) {
        ViewCompat.animate(this)
            .translationY(height.toFloat())
            .setDuration(ANIM_DURATION.toLong())
            .alpha(0.5f)
            .withEndAction(runnable)
        isShowing = false
    }

    private fun convertDpToPixels(context: Context, dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
            .toInt()
    }

    companion object {
        private const val ANIM_DURATION = 200
        private val INTERPOLATOR: Interpolator = FastOutLinearInInterpolator()
    }
}