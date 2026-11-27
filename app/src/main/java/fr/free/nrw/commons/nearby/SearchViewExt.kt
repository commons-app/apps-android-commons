package fr.free.nrw.commons.nearby

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.SearchView
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
        setTextColor(context.themeColor(R.attr.nearbySearchTextColor))
        setHintTextColor(context.themeColor(R.attr.nearbySearchHintColor))
    }
}

private fun Context.themeColor(
    @AttrRes attr: Int,
): Int {
    val value = TypedValue()
    theme.resolveAttribute(attr, value, true)
    return value.data
}
