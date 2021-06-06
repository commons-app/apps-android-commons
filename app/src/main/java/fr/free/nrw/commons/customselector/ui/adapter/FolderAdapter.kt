package fr.free.nrw.commons.customselector.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.listeners.FolderClickListener
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.ui.selector.ImageLoader

class FolderAdapter(
    context: Context,
    private val itemClickListener: FolderClickListener
) : RecyclerViewAdapter<FolderAdapter.FolderViewHolder?>(context) {
    private val imageLoader = ImageLoader()
    private val folders: MutableList<Folder> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val itemView = inflater.inflate(R.layout.item_custom_selector_folder, parent,false)
        return FolderViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folders[position]
        val count = folder.images.size
        val previewImage = folder.images[0]
        holder.name.text = folder.name
        holder.count.text= count.toString()
        holder.itemView.setOnClickListener{
            itemClickListener.onFolderClick(folder)
        }
    }

    fun init(folders: List<Folder>){
        this.folders.clear()
        this.folders.addAll(folders)
        notifyDataSetChanged()
    }


    override fun getItemCount(): Int {
        return folders.size
    }

    class FolderViewHolder(itemView:View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.folder_thumbnail)
        val name: TextView = itemView.findViewById(R.id.folder_name)
        val count: TextView = itemView.findViewById(R.id.folder_count)
    }

}