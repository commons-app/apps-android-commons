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
    /**
     * Application context.
     */
    context: Context,

    /**
     * Folder Click listener for click events.
     */
    private val itemClickListener: FolderClickListener
) : RecyclerViewAdapter<FolderAdapter.FolderViewHolder?>(context) {

    /**
     * Image Loader for loading images.
     */
    private val imageLoader = ImageLoader()

    /**
     * List of folders.
     */
    private val folders: MutableList<Folder> = mutableListOf()

    /**
     * Create view holder, returns View holder item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val itemView = inflater.inflate(R.layout.item_custom_selector_folder, parent, false)
        return FolderViewHolder(itemView)
    }

    /**
     * Bind view holder, setup the item view, title, count and click listener
     */
    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folders[position]
        val count = folder.images.size
        val previewImage = folder.images[0]
        holder.name.text = folder.name
        holder.count.text= count.toString()
        holder.itemView.setOnClickListener{
            itemClickListener.onFolderClick(folder)
        }

        //todo load image thumbnail.
    }

    /**
     * Initialise the data set.
     */
    fun init(folders: List<Folder>) {
        this.folders.clear()
        this.folders.addAll(folders)
        notifyDataSetChanged()
    }


    /**
     * returns item count.
     */
    override fun getItemCount(): Int {
        return folders.size
    }

    /**
     * Folder view holder.
     */
    class FolderViewHolder(itemView:View) : RecyclerView.ViewHolder(itemView) {

        /**
         * Folder thumbnail image view.
         */
        val image: ImageView = itemView.findViewById(R.id.folder_thumbnail)

        /**
         * Folder/album name
         */
        val name: TextView = itemView.findViewById(R.id.folder_name)

        /**
         * Item count in Folder/Item
         */
        val count: TextView = itemView.findViewById(R.id.folder_count)
    }

}