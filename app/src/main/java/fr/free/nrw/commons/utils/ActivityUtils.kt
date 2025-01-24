package fr.free.nrw.commons.utils

import android.content.Context
import android.content.Intent

object ActivityUtils {

    @JvmStatic
    fun <T> startActivityWithFlags(context: Context, cls: Class<T>, vararg flags: Int) {
        val intent = Intent(context, cls)
        for (flag in flags) {
            intent.addFlags(flag)
        }
        context.startActivity(intent)
    }
}