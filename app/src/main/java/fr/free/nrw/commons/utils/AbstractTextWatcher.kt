package fr.free.nrw.commons.utils

import android.text.Editable
import android.text.TextWatcher

class AbstractTextWatcher(
    private val textChange: TextChange
) : TextWatcher {

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // No-op
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        textChange.onTextChanged(s.toString())
    }

    override fun afterTextChanged(s: Editable?) {
        // No-op
    }

    interface TextChange {
        fun onTextChanged(value: String)
    }
}
