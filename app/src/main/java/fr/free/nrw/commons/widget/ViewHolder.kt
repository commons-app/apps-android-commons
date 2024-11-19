package fr.free.nrw.commons.widget

import android.content.Context

interface ViewHolder<T> {
    fun bindModel(context: Context, model: T)
}