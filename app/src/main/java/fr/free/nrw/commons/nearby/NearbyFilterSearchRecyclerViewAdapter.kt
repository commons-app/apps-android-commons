package fr.free.nrw.commons.nearby

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.nearby.Label.Companion.fromText
import fr.free.nrw.commons.nearby.NearbyFilterSearchRecyclerViewAdapter.RecyclerViewHolder

class NearbyFilterSearchRecyclerViewAdapter(
    context: Context, labels: MutableList<Label>
) : RecyclerView.Adapter<RecyclerViewHolder?>(), Filterable {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var labels: MutableList<Label>? = labels
    private var displayedLabels: MutableList<Label> = labels
    var callback: Callback? = null
    var selectedLabels: MutableList<Label> = mutableListOf()

    class RecyclerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var placeTypeLabel: TextView? = view.findViewById(R.id.place_text)
        var placeTypeIcon: ImageView? = view.findViewById(R.id.place_icon)
        var placeTypeLayout: LinearLayout? = view.findViewById(R.id.search_list_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        return RecyclerViewHolder(
            inflater.inflate(
                if (callback!!.isDarkTheme) R.layout.nearby_search_list_item_dark else R.layout.nearby_search_list_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val label = displayedLabels[position]
        holder.placeTypeIcon?.setImageResource(label.icon)
        holder.placeTypeLabel?.text = label.toString()
        holder.placeTypeLayout?.isSelected = label.isSelected

        holder.placeTypeLayout?.setOnClickListener {
            callback!!.setCheckboxUnknown()
            if (label.isSelected) {
                selectedLabels.remove(label)
            } else {
                selectedLabels.add(label)
            }

            label.isSelected = !label.isSelected
            holder.placeTypeLayout?.isSelected = label.isSelected

            NearbyFilterState.setSelectedLabels(selectedLabels)
            callback!!.filterByMarkerType(selectedLabels, 0, false, false)
        }
    }

    override fun getItemId(position: Int): Long = displayedLabels[position].hashCode().toLong()

    override fun getItemCount(): Int = displayedLabels.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                var constraint = constraint
                val results = FilterResults()
                val filteredArrayList = mutableListOf<Label>()

                if (labels == null) {
                    labels = mutableListOf()
                }

                if (constraint == null || constraint.length == 0) {
                    // set the Original result to return
                    results.count = labels!!.size
                    results.values = labels
                } else {
                    constraint = constraint.toString().lowercase()

                    for (label in labels!!) {
                        val data = label.toString()
                        if (data.lowercase().startsWith(constraint)) {
                            filteredArrayList.add(fromText(label.text))
                        }
                    }

                    // set the Filtered result to return
                    results.count = filteredArrayList.size
                    results.values = filteredArrayList
                }
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                displayedLabels = results.values as ArrayList<Label> // has the filtered values
                notifyDataSetChanged() // notifies the data with new filtered values
            }
        }
    }

    fun setRecyclerViewAdapterItemsGreyedOut() {
        for (label in labels!!) {
            label.isSelected = false
            selectedLabels.remove(label)
        }
        NearbyFilterState.setSelectedLabels(selectedLabels)
        notifyDataSetChanged()
    }

    fun setRecyclerViewAdapterAllSelected() {
        for (label in labels!!) {
            label.isSelected = true
            if (!selectedLabels.contains(label)) {
                selectedLabels.add(label)
            }
        }
        NearbyFilterState.setSelectedLabels(selectedLabels)
        notifyDataSetChanged()
    }

    interface Callback {
        fun setCheckboxUnknown()

        fun filterByMarkerType(
            selectedLabels: List<Label>, i: Int, filterForPlaceState: Boolean, filterForAllNoneType: Boolean
        )

        val isDarkTheme: Boolean
    }
}
