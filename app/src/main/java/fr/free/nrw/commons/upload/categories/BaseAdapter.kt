package fr.free.nrw.commons.upload.categories

import androidx.recyclerview.widget.DiffUtil
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter


abstract class BaseAdapter<T>(
    itemCallback: DiffUtil.ItemCallback<T>,
    vararg adapterDelegate: AdapterDelegate<List<T>>
) : AsyncListDifferDelegationAdapter<T>(itemCallback, *adapterDelegate) {
    fun addAll(newResults: List<T>) {
        items = (items ?: emptyList<T>()) + newResults
    }

    fun clear() {
        items = emptyList()
    }
}

