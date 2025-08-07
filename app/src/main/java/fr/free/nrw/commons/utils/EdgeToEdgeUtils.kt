package fr.free.nrw.commons.utils

import androidx.core.graphics.Insets
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

fun View.applyEdgeToEdgeInsets(
    typeMask: Int = WindowInsetsCompat.Type.systemBars(),
    block: MarginLayoutParams.(Insets) -> Unit
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
        val insets = windowInsets.getInsets(typeMask)

        v.updateLayoutParams<MarginLayoutParams> {
            apply { block(insets) }
        }

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