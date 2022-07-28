package fr.free.nrw.commons.customselector.ui.adapter

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
import fr.free.nrw.commons.customselector.helper.ImageHelper.CUSTOM_SELECTOR_PREFERENCE_KEY
import fr.free.nrw.commons.customselector.helper.ImageHelper.SWITCH_STATE_PREFERENCE_KEY
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.selector.ImageLoader
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * Custom selector ImageAdapter.
 */
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
     * Number of selected not for upload images
     */
    private var selectedNotForUploadImages = 0

    /**
     * List of all images in adapter.
     */
    private var images: ArrayList<Image> = ArrayList()

    /**
     * Stores all images
     */
    private var allImages: List<Image> = ArrayList()

    private var actionable: TreeMap<Int, Image> = TreeMap()
    private var already: ArrayList<Int> = ArrayList()
    private var nextImage = 0

    /**
     * Coroutine Dispatchers and Scope.
     */
    private var defaultDispatcher : CoroutineDispatcher = Dispatchers.Default
    private var ioDispatcher : CoroutineDispatcher = Dispatchers.IO
    private val scope : CoroutineScope = MainScope()

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
        var image=images[position]
//        holder.image.setImageDrawable (null)
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
                holder.itemSelected(selectedImages.size)
            } else {
                holder.itemUnselected();
            }
//            Glide.with(holder.image).load(image.uri).thumbnail(0.3f).into(holder.image)
            scope.launch {
                Log.d("hahaa", "onBindViewHolder1: "+(position))
                val isActionedImage =
                    imageLoader.queryAndSetView(
                        holder, image, allImages, ioDispatcher, defaultDispatcher, position
                    )
                val sharedPreferences: SharedPreferences =
                    context.getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, 0)
                val switchState =
                    sharedPreferences.getBoolean(SWITCH_STATE_PREFERENCE_KEY, true)
                if (!switchState) {
                    // only call if already not get the position
                    Log.d("hahaa", "onBindViewHolder2: "+(position))

                    Log.d("haha", "onBindViewHolder: "+(already)+" d "+(actionable.keys))
                    if(!already.contains(position)) {
                        val next = imageLoader.nextActionedImage(
                            allImages, position, ioDispatcher,
                            defaultDispatcher, nextImage
                        )
                        if (next > -1) {
                            nextImage = next+1
                            if (!actionable.containsKey(next)) {
                                actionable[next] = allImages[next]
                                already.add(position)
                                Glide.with(holder.image).load(allImages[next].uri).thumbnail(0.3f).into(holder.image)
                                notifyItemInserted(position)
                                notifyItemRangeChanged(position, itemCount+1)
                                // increase item count
                                // notifydataadded
                                val a: List<Image> = ArrayList(actionable.values)
                                image = a[position]
                            }
//                            else if (actionable.containsKey(next) && !(already.contains(position))) {
//
//                            }
                        }
                    } else {
                        val a: List<Image> = ArrayList(actionable.values)
                        image = a[position]
                        Glide.with(holder.image).load(image.uri).thumbnail(0.3f).into(holder.image)
                    }
//                    if (isActionedImage>=0) {
//                        images.remove(image)
//                        notifyItemRemoved(position)
//                        notifyItemRangeChanged(position, itemCount)
//                    } else if (isActionedImage == -1) {
//
//                    }

                } else {
                    Glide.with(holder.image).load(image.uri).thumbnail(0.3f).into(holder.image)
                }
            }
            holder.itemView.setOnClickListener {
                selectOrRemoveImage(holder, position)
            }

            // launch media preview on long click.
            holder.itemView.setOnLongClickListener {
                imageSelectListener.onLongPress(image.uri)
                true
            }
        }
    }

    /**
     * Provides filtered images
     */
    fun getFilteredImages(): ArrayList<Image> {
        return images
    }

    /**
     * Handle click event on an image, update counter on images.
     */
    private fun selectOrRemoveImage(holder: ImageViewHolder, position: Int){
        val clickedIndex = ImageHelper.getIndex(selectedImages, images[position])
        if (clickedIndex != -1) {
            selectedImages.removeAt(clickedIndex)
            if (holder.isItemNotForUpload()) {
                selectedNotForUploadImages--
            }
            notifyItemChanged(position, ImageUnselected())
            val indexes = ImageHelper.getIndexList(selectedImages, images)
            for (index in indexes) {
                notifyItemChanged(index, ImageSelectedOrUpdated())
            }
        } else {
            if(holder.isItemUploaded()){
                Toast.makeText(context, R.string.custom_selector_already_uploaded_image_text, Toast.LENGTH_SHORT).show()
            } else {
                if (holder.isItemNotForUpload()) {
                    selectedNotForUploadImages++
                }
                selectedImages.add(images[position])
                val indexes = ImageHelper.getIndexList(selectedImages, images)
                for (index in indexes) {
                    notifyItemChanged(index, ImageSelectedOrUpdated())
                }
            }
        }
        imageSelectListener.onSelectedImagesChanged(selectedImages, selectedNotForUploadImages)
    }

    /**
     * Initialize the data set.
     */
    fun init(newImages: List<Image>, fixedImages: List<Image>, emptyMap: TreeMap<Int, Image>) {
        allImages = fixedImages
        val oldImageList:ArrayList<Image> = images
        val newImageList:ArrayList<Image> = ArrayList(newImages)
        actionable = emptyMap
        already = ArrayList()
        nextImage = 0
        val diffResult = DiffUtil.calculateDiff(
            ImagesDiffCallback(oldImageList, newImageList)
        )
        images = newImageList
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * Refresh the data in the adapter
     */
    fun refresh(newImages: List<Image>, fixedImages: List<Image>) {
        selectedNotForUploadImages = 0
        selectedImages.clear()
        images.clear()
        selectedImages = arrayListOf()
        init(newImages, fixedImages, TreeMap())
        notifyDataSetChanged()
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, 0)
        val switchState =
            sharedPreferences.getBoolean(SWITCH_STATE_PREFERENCE_KEY, true)
        return if(switchState) {
            allImages.size
        } else {
            actionable.size+1
        }
    }

    fun getImageIdAt(position: Int): Long {
        return images.get(position).id
    }

    /**
     * CleanUp function.
     */
    fun cleanUp() {
        scope.cancel()
    }

    /**
     * Image view holder.
     */
    class ImageViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.image_thumbnail)
        private val selectedNumber: TextView = itemView.findViewById(R.id.selected_count)
        private val uploadedGroup: Group = itemView.findViewById(R.id.uploaded_group)
        private val notForUploadGroup: Group = itemView.findViewById(R.id.not_for_upload_group)
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

        /**
         * Item is not for upload view
         */
        fun itemNotForUpload() {
            notForUploadGroup.visibility = View.VISIBLE
        }

        fun isItemUploaded():Boolean {
            return uploadedGroup.visibility == View.VISIBLE
        }

        /**
         * Item is not for upload
         */
        fun isItemNotForUpload():Boolean {
            return notForUploadGroup.visibility == View.VISIBLE
        }

        /**
         * Item Not Uploaded view.
         */
        fun itemNotUploaded() {
            uploadedGroup.visibility = View.GONE
        }

        /**
         * Item can be uploaded view
         */
        fun itemForUpload() {
            notForUploadGroup.visibility = View.GONE
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