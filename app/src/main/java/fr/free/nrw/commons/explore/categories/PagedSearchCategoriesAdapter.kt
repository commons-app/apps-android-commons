package fr.free.nrw.commons.explore.categories

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CATEGORY_PREFIX
import fr.free.nrw.commons.explore.BaseViewHolder
import fr.free.nrw.commons.explore.inflate
import kotlinx.android.synthetic.main.item_recent_searches.*


class PagedSearchCategoriesAdapter(val onCategoryClicked: (String) -> Unit) :
    PagedListAdapter<String, CategoryItemViewHolder>(
        object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String) =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: String, newItem: String) =
                oldItem == newItem
        }
    ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryItemViewHolder {
        return CategoryItemViewHolder(
            parent.inflate(R.layout.item_recent_searches),
            onCategoryClicked
        )
    }

    override fun onBindViewHolder(holder: CategoryItemViewHolder, position: Int) {
        holder.bind(getItem(position)!!)
    }
}

class CategoryItemViewHolder(containerView: View, val onCategoryClicked: (String) -> Unit) :
    BaseViewHolder<String>(containerView) {

    override fun bind(item: String) {
        containerView.setOnClickListener { onCategoryClicked(item) }
        textView1.text = item.substringAfter(CATEGORY_PREFIX)
    }
}


