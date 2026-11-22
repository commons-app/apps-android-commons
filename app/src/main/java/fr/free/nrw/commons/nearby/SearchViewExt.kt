package fr.free.nrw.commons.nearby

import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import fr.free.nrw.commons.R

internal fun SearchView.enableSearchIconFocus(
    @StringRes descriptionRes: Int,
) {
    val searchView = this
    findViewById<View>(androidx.appcompat.R.id.search_mag_icon)?.apply {
        isClickable = true
        isFocusable = true
        contentDescription = context.getString(descriptionRes)
        setOnClickListener {
            searchView.findViewById<View>(androidx.appcompat.R.id.search_src_text)?.requestFocus()
        }
    }
    findViewById<TextView>(androidx.appcompat.R.id.search_src_text)?.apply {
        setTextColor(ContextCompat.getColor(context, R.color.primaryDarkColor))
        setHintTextColor(ContextCompat.getColor(context, R.color.secondaryTextColor))
    }
}
