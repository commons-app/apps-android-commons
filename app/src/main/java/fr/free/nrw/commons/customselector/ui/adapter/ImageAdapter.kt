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

class ImageAdapter(context: Context, private val imageSelectListener: ImageSelectListener ):
    RecyclerViewAdapter<ImageAdapter.ImageViewHolder>(context) {
    private val selectedImages = arrayListOf<Image>()
    private val images: ArrayList<Image> = ArrayList()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageAdapter.ImageViewHolder {
        val itemView = inflater.inflate(R.layout.item_custom_selector_image,parent,false)
        return ImageViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ImageAdapter.ImageViewHolder, position: Int) {
        val image=images[position]
        val selectedIndex = 1 // todo get selected index of the image in selected images
        val isSelected = selectedIndex !=-1
        // todo load image thumbnail
        holder.itemView.setOnClickListener {
            selectOrRemoveImage(image, position)
        }
    }

    private fun selectOrRemoveImage(image:Image, position:Int){
        //todo select the image if not selected and remove it if already selected
    }

    fun init(images:List<Image>){
        this.images.clear()
        this.images.addAll(images)
        notifyDataSetChanged()
    }

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