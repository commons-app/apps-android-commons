package fr.free.nrw.commons.ui.widget

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet

import androidx.appcompat.widget.AppCompatTextView

import fr.free.nrw.commons.utils.StringUtil

/**
 * An [AppCompatTextView] which formats the text to HTML displayable text and makes any
 * links clickable.
 */
class HtmlTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    init {
        movementMethod = LinkMovementMethod.getInstance()
        text = StringUtil.fromHtml(text.toString())
    }

    /**
     * Sets the text to be displayed
     * @param newText the text to be displayed
     */
    fun setHtmlText(newText: String) {
        text = StringUtil.fromHtml(newText)
    }
}
