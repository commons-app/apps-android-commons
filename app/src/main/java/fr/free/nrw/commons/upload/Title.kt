package fr.free.nrw.commons.upload

import android.text.TextUtils

class Title {
    private var titleText: String? = null
    var isSet = false
    override fun toString(): String {
        if (titleText == null) {
            return ""
        } else {
            return titleText!!
        }
    }

    fun setTitleText(titleText: String?) {
        this.titleText = titleText
        if (!TextUtils.isEmpty(titleText)) {
            isSet = true
        }
    }

    val isEmpty: Boolean
        get() = titleText == null || titleText!!.isEmpty()

    fun getTitleText(): String? {
        return titleText
    }
}