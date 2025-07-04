package fr.free.nrw.commons.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE

object ClipboardUtils {
    // Convenience for Java usages - remove when they are converted.
    @JvmStatic
    fun copy(label: String?, text: String?, context: Context) {
        context.copyToClipboard(label, text)
    }
}

fun Context.copyToClipboard(label: String?, text: String?) {
    with(getSystemService(CLIPBOARD_SERVICE) as ClipboardManager) {
        setPrimaryClip(ClipData.newPlainText(label, text))
    }
}