package fr.free.nrw.commons.ui

import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.os.Build.VERSION
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputEditText
import fr.free.nrw.commons.R


class PasteSensitiveTextInputEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : TextInputEditText(context, attrs) {

    private var formattingAllowed: Boolean = true

    init {
        if (attrs != null) {
            formattingAllowed = extractFormattingAttribute(context, attrs)
        }
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        // if not paste command, or formatting is allowed, return default
        if (id != android.R.id.paste || formattingAllowed) {
            return super.onTextContextMenuItem(id)
        }

        // if it's paste and formatting not allowed
        val proceeded: Boolean = if (VERSION.SDK_INT >= 23) {
            super.onTextContextMenuItem(android.R.id.pasteAsPlainText)
        } else {
            val success = super.onTextContextMenuItem(id)
            if (success && text != null) {
                // rewrite with plain text so formatting is lost
                setText(text.toString())
                setSelection(text?.length ?: 0)
            }
            success
        }
        return proceeded
    }

    private fun extractFormattingAttribute(context: Context, attrs: AttributeSet): Boolean {
        val a = context.theme.obtainStyledAttributes(
            attrs, R.styleable.PasteSensitiveTextInputEditText, 0, 0
        )
        return try {
            a.getBoolean(R.styleable.PasteSensitiveTextInputEditText_allowFormatting, true)
        } finally {
            a.recycle()
        }
    }

    fun setFormattingAllowed(formattingAllowed: Boolean) {
        this.formattingAllowed = formattingAllowed
    }
}
