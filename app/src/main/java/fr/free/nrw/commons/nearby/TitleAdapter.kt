package fr.free.nrw.commons.nearby

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.nearby.model.Title

class TitleAdapter(private val titles: List<Title>) :
    RecyclerView.Adapter<TitleAdapter.TitleViewHolder>() {

    class TitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val descriptionsRecyclerView: RecyclerView = itemView.findViewById(R.id.descriptionsRecyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TitleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_wikitalk_item, parent, false)
        return TitleViewHolder(view)
    }

    override fun onBindViewHolder(holder: TitleViewHolder, position: Int) {
        val title = titles[position]
        holder.titleTextView.text = title.title

        holder.descriptionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(holder.itemView.context)
            adapter = DescriptionAdapter(title.descriptions)
        }
    }

    override fun getItemCount(): Int {
        return titles.size
    }
}
