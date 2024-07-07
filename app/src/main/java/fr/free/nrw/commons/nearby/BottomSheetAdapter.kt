package fr.free.nrw.commons.nearby

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.nearby.model.BottomSheetItem

/**
 * RecyclerView Adapter for displaying items in a bottom sheet.
 *
 * @property context The context used for inflating layout resources.
 * @property itemList The list of BottomSheetItem objects to display.
 * @constructor Creates an instance of BottomSheetAdapter.
 */
class BottomSheetAdapter(context: Context?, private val itemList: List<BottomSheetItem>) :
    RecyclerView.Adapter<BottomSheetAdapter.ViewHolder>() {
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private var itemClickListener: ItemClickListener? = null

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = layoutInflater.inflate(R.layout.bottom_sheet_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: ViewHolder, position: Int) {
        val item = itemList[position]
        holder.imageView.setImageDrawable(
            ContextCompat.getDrawable(
                getContext(),
                item.imageResourceId
            )
        )
        holder.title.setText(item.title)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int {
        return itemList.size
    }

    /**
     * Updates the icon for bookmark item.
     *
     * @param icon The resource ID of the new icon to set.
     */
    fun updateBookmarkIcon(icon: Int) {
        itemList.forEachIndexed { index, item ->
            if (item.imageResourceId == R.drawable.ic_round_star_filled_24px || item.imageResourceId == R.drawable.ic_round_star_border_24px) {
                item.imageResourceId = icon
                this.notifyItemChanged(index)
                return
            }
        }
    }

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener, OnLongClickListener {
        var imageView: ImageView = itemView.findViewById(R.id.buttonImage)
        var title: TextView = itemView.findViewById(R.id.buttonText)

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(view: View) {
            if (itemClickListener != null) itemClickListener!!.onBottomSheetItemClick(
                view,
                adapterPosition
            )
        }

        override fun onLongClick(view: View): Boolean {
            if (itemClickListener != null) itemClickListener!!.onBottomSheetItemLongClick(
                view,
                adapterPosition
            )
            return true
        }
    }

    fun setClickListener(itemClickListener: ItemClickListener?) {
        this.itemClickListener = itemClickListener
    }

    fun getContext(): Context {
        return layoutInflater.context
    }

    interface ItemClickListener {
        fun onBottomSheetItemClick(view: View?, position: Int)
        fun onBottomSheetItemLongClick(view: View?, position: Int)
    }
}

