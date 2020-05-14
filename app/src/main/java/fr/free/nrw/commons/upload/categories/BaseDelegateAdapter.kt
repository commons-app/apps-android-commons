package fr.free.nrw.commons.upload.categories

import androidx.recyclerview.widget.DiffUtil
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter


abstract class BaseDelegateAdapter<T>(
    vararg delegates: AdapterDelegate<List<T>>,
    areItemsTheSame: (T, T) -> Boolean,
    areContentsTheSame: (T, T) -> Boolean = { old, new -> old == new }
) : AsyncListDifferDelegationAdapter<T>(
    object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T) =
            areItemsTheSame(oldItem, newItem)

        override fun areContentsTheSame(oldItem: T, newItem: T) =
            areContentsTheSame(oldItem, newItem)
    },
    *delegates
) {

    fun addAll(newResults: List<T>) {
        items = itemsOrEmpty + newResults
    }

    fun clear() {
        items = emptyList()
    }

    fun add(item: T) {
        items = itemsOrEmpty + item
    }

    fun remove(item: T) {
        items = itemsOrEmpty - item
    }

    private val itemsOrEmpty get() = items ?: emptyList<T>()
}

