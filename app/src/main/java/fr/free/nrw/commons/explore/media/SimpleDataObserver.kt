package fr.free.nrw.commons.explore.media

import androidx.recyclerview.widget.RecyclerView

class SimpleDataObserver(private val onAnyChange: () -> Unit) : RecyclerView.AdapterDataObserver() {
    override fun onChanged() {
        super.onChanged()
        onAnyChange.invoke()
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        super.onItemRangeRemoved(positionStart, itemCount)
        onAnyChange.invoke()
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        super.onItemRangeMoved(fromPosition, toPosition, itemCount)
        onAnyChange.invoke()
    }

    override fun onStateRestorationPolicyChanged() {
        super.onStateRestorationPolicyChanged()
        onAnyChange.invoke()
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        super.onItemRangeInserted(positionStart, itemCount)
        onAnyChange.invoke()
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        super.onItemRangeChanged(positionStart, itemCount)
        onAnyChange.invoke()
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
        super.onItemRangeChanged(positionStart, itemCount, payload)
        onAnyChange.invoke()
    }
}
