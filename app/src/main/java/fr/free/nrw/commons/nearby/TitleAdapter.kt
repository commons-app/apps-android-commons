package fr.free.nrw.commons.nearby

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.nearby.model.TalkItem

class TitleAdapter(private val titles: List<TalkItem>) :
    RecyclerView.Adapter<TitleAdapter.TitleViewHolder>() {

    class TitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        val detailsTextView: TextView = itemView.findViewById(R.id.detailsTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TitleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_wikitalk_item, parent, false)
        return TitleViewHolder(view)
    }

    override fun onBindViewHolder(holder: TitleViewHolder, position: Int) {
        val talkItem = titles[position]
        holder.detailsTextView.text = "  " + talkItem.detail
        holder.descriptionTextView.text = "• " + talkItem.description

    }

    override fun getItemCount(): Int {
        return titles.size
    }
}
