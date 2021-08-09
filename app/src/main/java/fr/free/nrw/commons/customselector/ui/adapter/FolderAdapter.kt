package fr.free.nrw.commons.customselector.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.listeners.FolderClickListener
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Image

class   FolderAdapter(
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
     * List of folders.
     */
    private var folders: MutableList<Folder> = mutableListOf()

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
        val toBeRemoved = ArrayList<Image>()

        for(image in folder.images) {
            // Remove all the top images that do not exist anymore
            if(context.contentResolver.getType(image.uri) == null){
                // File not found
                toBeRemoved.add(image)
            } else {
                break
            }
        }
        holder.image.setImageDrawable (null)
        folder.images.removeAll(toBeRemoved)
        val count = folder.images.size

        if(count == 0) {
            // Folder is empty, remove folder from the adapter.
            holder.itemView.post{
                val updatePosition = folders.indexOf(folder)
                folders.removeAt(updatePosition)
                notifyItemRemoved(updatePosition)
                notifyItemRangeChanged(updatePosition, folders.size)
            }
        } else {
            val previewImage = folder.images[0]
            Glide.with(holder.image).load(previewImage.uri).into(holder.image)
            holder.name.text = folder.name
            holder.count.text = count.toString()
            holder.itemView.setOnClickListener {
                itemClickListener.onFolderClick(folder.bucketId, folder.name, 0)
            }
        }
    }

    /**
     * Initialise the data set.
     */
    fun init(newFolders: List<Folder>) {
        val oldFolderList: MutableList<Folder> = folders
        val newFolderList = newFolders.toMutableList()
        val diffResult = DiffUtil.calculateDiff(
            FoldersDiffCallback(oldFolderList, newFolderList)
        )
        folders = newFolderList
        diffResult.dispatchUpdatesTo(this)
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

    /**
     * DiffUtilCallback.
     */
    class FoldersDiffCallback(
        var oldFolders: MutableList<Folder>,
        var newFolders: MutableList<Folder>
    ) : DiffUtil.Callback() {
        /**
         * Returns the size of the old list.
         */
        override fun getOldListSize(): Int {
            return oldFolders.size
        }

        /**
         * Returns the size of the new list.
         */
        override fun getNewListSize(): Int {
            return newFolders.size
        }

        /**
         * Called by the DiffUtil to decide whether two object represent the same Item.
         */
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldFolders.get(oldItemPosition).bucketId == newFolders.get(newItemPosition).bucketId
        }

        /**
         * Called by the DiffUtil when it wants to check whether two items have the same data.
         * DiffUtil uses this information to detect if the contents of an item has changed.
         */
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldFolders.get(oldItemPosition).equals(newFolders.get(newItemPosition))
        }

    }

}