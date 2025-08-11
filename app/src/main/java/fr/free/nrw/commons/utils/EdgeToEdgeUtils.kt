package fr.free.nrw.commons.utils

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import fr.free.nrw.commons.R

fun View.applyEdgeToEdgeInsets(
    typeMask: Int = WindowInsetsCompat.Type.systemBars(),
    block: MarginLayoutParams.(InsetsAccumulator) -> Unit
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(typeMask)

        val initialTop = if (view.getTag(R.id.initial_margin_top) != null) {
            view.getTag(R.id.initial_margin_top) as Int
        } else {
            view.setTag(R.id.initial_margin_top, view.marginTop)
            view.marginTop
        }

        val initialBottom = if (view.getTag(R.id.initial_margin_bottom) != null) {
            view.getTag(R.id.initial_margin_bottom) as Int
        } else {
            view.setTag(R.id.initial_margin_bottom, view.marginBottom)
            view.marginBottom
        }

        val initialLeft = if (view.getTag(R.id.initial_margin_left) != null) {
            view.getTag(R.id.initial_margin_left) as Int
        } else {
            view.setTag(R.id.initial_margin_left, view.marginLeft)
            view.marginLeft
        }

        val initialRight = if (view.getTag(R.id.initial_margin_right) != null) {
            view.getTag(R.id.initial_margin_right) as Int
        } else {
            view.setTag(R.id.initial_margin_right, view.marginRight)
            view.marginRight
        }

        val accumulator = InsetsAccumulator(
            initialTop,
            insets.top,
            initialBottom,
            insets.bottom,
            initialLeft,
            insets.left,
            initialRight,
            insets.right
        )

        view.updateLayoutParams<MarginLayoutParams> {
            apply { block(accumulator) }
        }

        WindowInsetsCompat.CONSUMED
    }
}

fun View.applyEdgeToEdgeTopPaddingInsets(
    typeMask: Int = WindowInsetsCompat.Type.systemBars(),
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(typeMask)

        view.updatePadding(
            left = insets.left,
            right = insets.right,
            top = insets.top
        )

        WindowInsetsCompat.CONSUMED
    }
}

fun View.applyEdgeToEdgeBottomPaddingInsets(
    typeMask: Int = WindowInsetsCompat.Type.systemBars(),
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(typeMask)

        view.updatePadding(
            left = insets.left,
            right = insets.right,
            bottom = insets.bottom
        )

        WindowInsetsCompat.CONSUMED
    }
}

fun applyEdgeToEdgeAllInsets(view: View) = view.applyEdgeToEdgeInsets { insets ->
    leftMargin = insets.left
    rightMargin = insets.right
    topMargin = insets.top
    bottomMargin = insets.bottom
}

fun applyEdgeToEdgeTopInsets(view: View) = view.applyEdgeToEdgeInsets { insets ->
    leftMargin = insets.left
    rightMargin = insets.right
    topMargin = insets.top
}

fun applyEdgeToEdgeBottomInsets(view: View) = view.applyEdgeToEdgeInsets { insets ->
    leftMargin = insets.left
    rightMargin = insets.right
    bottomMargin = insets.bottom
}

data class InsetsAccumulator(
    private val initialTop: Int,
    private val insetTop: Int,
    private val initialBottom: Int,
    private val insetBottom: Int,
    private val initialLeft: Int,
    private val insetLeft: Int,
    private val initialRight: Int,
    private val insetRight: Int
) {
    val top = initialTop + insetTop
    val bottom = initialBottom + insetBottom
    val left = initialLeft + insetLeft
    val right = initialRight + insetRight
}