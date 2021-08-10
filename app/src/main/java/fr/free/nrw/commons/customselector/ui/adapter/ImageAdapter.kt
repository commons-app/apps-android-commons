package fr.free.nrw.commons.customselector.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.helper.ImageHelper
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.selector.ImageLoader

class ImageAdapter(
    /**
     * Application Context.
     */
    context: Context,

    /**
     * Image select listener for click events on image.
     */
    private var imageSelectListener: ImageSelectListener,

    /**
     * ImageLoader queries images.
     */
    private var imageLoader: ImageLoader
):

    RecyclerViewAdapter<ImageAdapter.ImageViewHolder>(context) {

    /**
     * ImageSelectedOrUpdated payload class.
     */
    class ImageSelectedOrUpdated

    /**
     * ImageUnselected payload class.
     */
    class ImageUnselected

    /**
     * Currently selected images.
     */
    private var selectedImages = arrayListOf<Image>()

    /**
     * List of all images in adapter.
     */
    private var images: ArrayList<Image> = ArrayList()

    /**
     * Create View holder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val itemView = inflater.inflate(R.layout.item_custom_selector_image, parent, false)
        return ImageViewHolder(itemView)
    }

    /**
     * Bind View holder, load image, selected view, click listeners.
     */
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val image=images[position]
        holder.image.setImageDrawable (null)
        if (context.contentResolver.getType(image.uri) == null) {
            // Image does not exist anymore, update adapter.
            holder.itemView.post {
                val updatedPosition = images.indexOf(image)
                images.remove(image)
                notifyItemRemoved(updatedPosition)
                notifyItemRangeChanged(updatedPosition, images.size)
            }
        } else {
            val selectedIndex = ImageHelper.getIndex(selectedImages, image)
            val isSelected = selectedIndex != -1
            if (isSelected) {
                holder.itemSelected(selectedIndex + 1)
            } else {
                holder.itemUnselected();
            }
            Glide.with(holder.image).load(image.uri).thumbnail(0.3f).into(holder.image)
            imageLoader.queryAndSetView(holder, image)
            holder.itemView.setOnClickListener {
                selectOrRemoveImage(holder, position)
            }
        }
    }

    /**
     * Handle click event on an image, update counter on images.
     */
    private fun selectOrRemoveImage(holder: ImageViewHolder, position: Int){
        val clickedIndex = ImageHelper.getIndex(selectedImages, images[position])
        if (clickedIndex != -1) {
            selectedImages.removeAt(clickedIndex)
            notifyItemChanged(position, ImageUnselected())
            val indexes = ImageHelper.getIndexList(selectedImages, images)
            for (index in indexes) {
                notifyItemChanged(index, ImageSelectedOrUpdated())
            }
        } else {
            if(holder.isItemUploaded()){
                Toast.makeText(context, "Already Uploaded image", Toast.LENGTH_SHORT).show()
            } else {
                selectedImages.add(images[position])
                notifyItemChanged(position, ImageSelectedOrUpdated())
            }
        }
        imageSelectListener.onSelectedImagesChanged(selectedImages)
    }

    /**
     * Initialize the data set.
     */
    fun init(newImages: List<Image>) {
        val oldImageList:ArrayList<Image> = images
        val newImageList:ArrayList<Image> = ArrayList(newImages)
        val diffResult = DiffUtil.calculateDiff(
            ImagesDiffCallback(oldImageList, newImageList)
        )
        images = newImageList
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int {
        return images.size
    }

    fun getImageIdAt(position: Int): Long {
        return images.get(position).id
    }

    /**
     * Image view holder.
     */
    class ImageViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.image_thumbnail)
        private val selectedNumber: TextView = itemView.findViewById(R.id.selected_count)
        private val uploadedGroup: Group = itemView.findViewById(R.id.uploaded_group)
        private val selectedGroup: Group = itemView.findViewById(R.id.selected_group)

        /**
         * Item selected view.
         */
        fun itemSelected(index: Int) {
            selectedGroup.visibility = View.VISIBLE
            selectedNumber.text = index.toString()
        }

        /**
         * Item Unselected view.
         */
        fun itemUnselected() {
            selectedGroup.visibility = View.GONE
        }

        /**
         * Item Uploaded view.
         */
        fun itemUploaded() {
            uploadedGroup.visibility = View.VISIBLE
        }

        fun isItemUploaded():Boolean {
            return uploadedGroup.visibility == View.VISIBLE
        }
        /**
         * Item Not Uploaded view.
         */
        fun itemNotUploaded() {
            uploadedGroup.visibility = View.GONE
        }

    }

    /**
     * DiffUtilCallback.
     */
    class ImagesDiffCallback(
        var oldImageList: ArrayList<Image>,
        var newImageList: ArrayList<Image>
    ) : DiffUtil.Callback(){

        /**
         * Returns the size of the old list.
         */
        override fun getOldListSize(): Int {
            return oldImageList.size
        }

        /**
         * Returns the size of the new list.
         */
        override fun getNewListSize(): Int {
            return newImageList.size
        }

        /**
         * Called by the DiffUtil to decide whether two object represent the same Item.
         */
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return newImageList[newItemPosition].id == oldImageList[oldItemPosition].id
        }

        /**
         * Called by the DiffUtil when it wants to check whether two items have the same data.
         * DiffUtil uses this information to detect if the contents of an item has changed.
         */
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldImageList[oldItemPosition].equals(newImageList[newItemPosition])
        }

    }

}