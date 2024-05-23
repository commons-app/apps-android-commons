package fr.free.nrw.commons.nearby

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.nearby.model.Description

class DescriptionAdapter(private val descriptions: List<Description>) :
    RecyclerView.Adapter<DescriptionAdapter.DescriptionViewHolder>() {

    class DescriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val descTextView: TextView = itemView.findViewById(R.id.descTextView)
        val userTextView: TextView = itemView.findViewById(R.id.userTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DescriptionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_wikitalk_item_description, parent, false)
        return DescriptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: DescriptionViewHolder, position: Int) {
        val description = descriptions[position]
        holder.descTextView.text = description.text
        holder.userTextView.text = description.user
        holder.dateTextView.text = description.time
    }

    override fun getItemCount(): Int {
        return descriptions.size
    }
}
