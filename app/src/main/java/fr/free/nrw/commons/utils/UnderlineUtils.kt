package fr.free.nrw.commons.utils

import android.widget.TextView
import androidx.core.text.buildSpannedString
import androidx.core.text.underline

object UnderlineUtils {
    // Convenience method for Java usages - remove when those classes are converted
    @JvmStatic
    fun setUnderlinedText(textView: TextView, stringResourceName: Int) {
        textView.setUnderlinedText(stringResourceName)
    }
}

fun TextView.setUnderlinedText(stringResourceName: Int) {
    text = buildSpannedString {
        underline { append(context.getString(stringResourceName)) }
    }
}
