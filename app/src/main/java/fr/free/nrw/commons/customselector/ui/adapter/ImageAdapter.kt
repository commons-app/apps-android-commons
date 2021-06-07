package fr.free.nrw.commons.customselector.ui.adapter

import android.content.Context
import android.view.ViewGroup
import fr.free.nrw.commons.R
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.Image

class ImageAdapter(
    /**
     * Application Context.
     */
    context: Context,

    /**
     * Image select listener for click events on image.
     */
    private val imageSelectListener: ImageSelectListener ):

    RecyclerViewAdapter<ImageAdapter.ImageViewHolder>(context) {

    /**
     * Currently selected images.
     */
    private val selectedImages = arrayListOf<Image>()

    /**
     * List of all images in adapter.
     */
    private val images: ArrayList<Image> = ArrayList()

    /**
     * create View holder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val itemView = inflater.inflate(R.layout.item_custom_selector_image,parent, false)
        return ImageViewHolder(itemView)
    }

    /**
     * Bind View holder, load image, selected view, click listeners.
     */
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val image=images[position]
        // todo load image thumbnail, set selected view.
        holder.itemView.setOnClickListener {
            selectOrRemoveImage(image, position)
        }
    }

    /**
     * Handle click event on an image, update counter on images.
     */
    private fun selectOrRemoveImage(image:Image, position:Int){
        // todo select the image if not selected and remove it if already selected
    }

    /**
     * Initialize the data set.
     */
    fun init(images:List<Image>) {
        this.images.clear()
        this.images.addAll(images)
        notifyDataSetChanged()
    }

    /**
     * Image view holder.
     */
    class ImageViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.image_thumbnail)
        val selectedNumber: TextView = itemView.findViewById(R.id.selected_count)
        val uploadedGroup: Group = itemView.findViewById(R.id.uploaded_group)
        val selectedGroup: Group = itemView.findViewById(R.id.selected_group)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int {
        return images.size
    }

}