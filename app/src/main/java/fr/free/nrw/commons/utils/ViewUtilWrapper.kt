package fr.free.nrw.commons.utils

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViewUtilWrapper @Inject constructor() {

    fun showShortToast(context: Context, text: String) {
        ViewUtil.showShortToast(context, text)
    }

    fun showLongToast(context: Context, text: String) {
        ViewUtil.showLongToast(context, text)
    }
}
