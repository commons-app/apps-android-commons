package fr.free.nrw.commons.explore.depictions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_item_load_more.*

class FooterAdapter(private val onRefreshClicked: () -> Unit) :
    ListAdapter<FooterItem, FooterViewHolder>(object :
        DiffUtil.ItemCallback<FooterItem>() {
        override fun areItemsTheSame(oldItem: FooterItem, newItem: FooterItem) = oldItem == newItem

        override fun areContentsTheSame(oldItem: FooterItem, newItem: FooterItem) =
            oldItem == newItem
    }) {

    override fun getItemViewType(position: Int): Int {
        return getItem(position).ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        when (FooterItem.values()[viewType]) {
            FooterItem.LoadingItem -> LoadingViewHolder(parent.inflate(R.layout.list_item_progress))
            FooterItem.RefreshItem -> RefreshViewHolder(
                parent.inflate(R.layout.list_item_load_more),
                onRefreshClicked
            )
        }

    override fun onBindViewHolder(holder: FooterViewHolder, position: Int) {}
}

open class FooterViewHolder(override val containerView: View) :
    RecyclerView.ViewHolder(containerView),
    LayoutContainer

class LoadingViewHolder(containerView: View) : FooterViewHolder(containerView)
class RefreshViewHolder(containerView: View, onRefreshClicked: () -> Unit) :
    FooterViewHolder(containerView) {
    init {
        listItemLoadMoreButton.setOnClickListener { onRefreshClicked() }
    }
}

enum class FooterItem { LoadingItem, RefreshItem }

fun ViewGroup.inflate(@LayoutRes layoutId: Int, attachToRoot: Boolean = false): View =
    LayoutInflater.from(context).inflate(layoutId, this, attachToRoot)
