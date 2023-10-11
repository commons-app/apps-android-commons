package fr.free.nrw.commons.customselector.ui.adapter

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.helper.ImageHelper
import fr.free.nrw.commons.customselector.helper.ImageHelper.CUSTOM_SELECTOR_PREFERENCE_KEY
import fr.free.nrw.commons.customselector.helper.ImageHelper.SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY
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

    RecyclerViewAdapter<ImageAdapter.ImageViewHolder>(context), FastScrollRecyclerView.SectionedAdapter {

    /**
     * ImageSelectedOrUpdated payload class.
     */
    class ImageSelectedOrUpdated

    /**
     * ImageUnselected payload class.
     */
    class ImageUnselected

    /**
     * Determines whether addition of all actionable images is done or not
     */
    private var reachedEndOfFolder: Boolean = false

    /**
     * Currently selected images.
     */
    private var selectedImages = arrayListOf<Image>()

    /**
     * Number of selected images that are marked as not for upload
     */
    private var numberOfSelectedImagesMarkedAsNotForUpload = 0

    /**
     * List of all images in adapter.
     */
    private var images: ArrayList<Image> = ArrayList()

    /**
     * Stores all images
     */
    private var allImages: List<Image> = ArrayList()

    /**
     * Map to store actionable images
     */
    private var actionableImagesMap: TreeMap<Int, Image> = TreeMap()

    /**
     * Stores already added positions of actionable images
     */
    private var alreadyAddedPositions: ArrayList<Int> = ArrayList()

    /**
     * Next starting index to initiate query to find next actionable image
     */
    private var nextImagePosition = 0

    /**
     * Helps to maintain the increasing sequence of the position. eg- 0, 1, 2, 3
     */
    private var imagePositionAsPerIncreasingOrder = 0

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
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, 0)
            val showAlreadyActionedImages =
                sharedPreferences.getBoolean(SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY, true)

            // Getting selected index when switch is on
            val selectedIndex: Int = if (showAlreadyActionedImages) {
                ImageHelper.getIndex(selectedImages, image)

                // Getting selected index when switch is off
            } else if (actionableImagesMap.size > position) {
                ImageHelper
                    .getIndex(selectedImages, ArrayList(actionableImagesMap.values)[position])

                // For any other case return -1
            } else {
                -1
            }

            val isSelected = selectedIndex != -1
            if (isSelected) {
                holder.itemSelected()
            } else {
                holder.itemUnselected()
            }

            imageLoader.queryAndSetView(
                holder, image, ioDispatcher, defaultDispatcher
            )
            scope.launch {
                val sharedPreferences: SharedPreferences =
                    context.getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, 0)
                val showAlreadyActionedImages =
                    sharedPreferences.getBoolean(SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY, true)
                if (!showAlreadyActionedImages) {
                    // If the position is not already visited, that means the position is new then
                    // finds the next actionable image position from all images
                    if (!alreadyAddedPositions.contains(position)) {
                        processThumbnailForActionedImage(holder, position)

                    // If the position is already visited, that means the image is already present
                    // inside map, so it will fetch the image from the map and load in the holder
                    } else {
                        val actionableImages: List<Image> = ArrayList(actionableImagesMap.values)
                        image = actionableImages[position]
                        Glide.with(holder.image).load(image.uri)
                            .thumbnail(0.3f).into(holder.image)
                    }

                // If switch is turned off, it just fetches the image from all images without any
                // further operations
                } else {
                    Glide.with(holder.image).load(image.uri)
                        .thumbnail(0.3f).into(holder.image)
                }
            }

            holder.itemView.setOnClickListener {
                onThumbnailClicked(position, holder)
            }

            // launch media preview on long click.
            holder.itemView.setOnLongClickListener {
                imageSelectListener.onLongPress(images.indexOf(image), images, selectedImages)
                true
            }
        }
    }

    /**
     * Process thumbnail for actioned image
     */
    suspend fun processThumbnailForActionedImage(
        holder: ImageViewHolder,
        position: Int
    ) {
        val next = imageLoader.nextActionableImage(
            allImages, ioDispatcher, defaultDispatcher,
            nextImagePosition
        )

        // If next actionable image is found, saves it, as the the search for
        // finding next actionable image will start from this position
        if (next > -1) {
            nextImagePosition = next + 1

            // If map doesn't contains the next actionable image, that means it's a
            // new actionable image, it will put it to the map as actionable images
            // and it will load the new image in the view holder
            if (!actionableImagesMap.containsKey(next)) {
                actionableImagesMap[next] = allImages[next]
                alreadyAddedPositions.add(imagePositionAsPerIncreasingOrder)
                imagePositionAsPerIncreasingOrder++
                Glide.with(holder.image).load(allImages[next].uri)
                    .thumbnail(0.3f).into(holder.image)
                notifyItemInserted(position)
                notifyItemRangeChanged(position, itemCount + 1)
            }

            // If next actionable image is not found, that means searching is
            // complete till end, and it will stop searching.
        } else {
            reachedEndOfFolder = true
            notifyItemRemoved(position)
        }
    }

    /**
     * Handles click on thumbnail
     */
    private fun onThumbnailClicked(
        position: Int,
        holder: ImageViewHolder
    ) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, 0)
        val switchState =
            sharedPreferences.getBoolean(SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY, true)

        // While switch is turned off, lets user click on image only if the position is
        // added inside map
        if (!switchState) {
            if (actionableImagesMap.size > position) {
                selectOrRemoveImage(holder, position)
            }
        } else {
            selectOrRemoveImage(holder, position)
        }
    }

    /**
     * Handle click event on an image, update counter on images.
     */
    private fun selectOrRemoveImage(holder: ImageViewHolder, position: Int){
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, 0)
        val showAlreadyActionedImages =
            sharedPreferences.getBoolean(SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY, true)

        // Getting clicked index from all images index when show_already_actioned_images
        // switch is on
        val clickedIndex: Int = if(showAlreadyActionedImages) {
            ImageHelper.getIndex(selectedImages, images[position])

        // Getting clicked index from actionable images when show_already_actioned_images
        // switch is off
        } else {
            ImageHelper.getIndex(selectedImages, ArrayList(actionableImagesMap.values)[position])
        }

        if (clickedIndex != -1) {
            selectedImages.removeAt(clickedIndex)
            if (holder.isItemNotForUpload()) {
                numberOfSelectedImagesMarkedAsNotForUpload--
            }
            notifyItemChanged(position, ImageUnselected())

            // Getting index from all images index when switch is on
            val indexes = if (showAlreadyActionedImages) {
                ImageHelper.getIndexList(selectedImages, images)

            // Getting index from actionable images when switch is off
            } else {
                ImageHelper.getIndexList(selectedImages, ArrayList(actionableImagesMap.values))
            }
            for (index in indexes) {
                notifyItemChanged(index, ImageSelectedOrUpdated())
            }
        } else {
            if (holder.isItemUploaded()) {
                Toast.makeText(context, R.string.custom_selector_already_uploaded_image_text, Toast.LENGTH_SHORT).show()
            } else {
                if (holder.isItemNotForUpload()) {
                    numberOfSelectedImagesMarkedAsNotForUpload++
                }

                // Getting index from all images index when switch is on
                val indexes: ArrayList<Int> = if (showAlreadyActionedImages) {
                    selectedImages.add(images[position])
                    ImageHelper.getIndexList(selectedImages, images)

                // Getting index from actionable images when switch is off
                } else {
                    selectedImages.add(ArrayList(actionableImagesMap.values)[position])
                    ImageHelper.getIndexList(selectedImages, ArrayList(actionableImagesMap.values))
                }

                for (index in indexes) {
                    notifyItemChanged(index, ImageSelectedOrUpdated())
                }
            }
        }
        imageSelectListener.onSelectedImagesChanged(selectedImages, numberOfSelectedImagesMarkedAsNotForUpload)
    }

    /**
     * Initialize the data set.
     */
    fun init(newImages: List<Image>, fixedImages: List<Image>, emptyMap: TreeMap<Int, Image>) {
        allImages = fixedImages
        val oldImageList:ArrayList<Image> = images
        val newImageList:ArrayList<Image> = ArrayList(newImages)
        actionableImagesMap = emptyMap
        alreadyAddedPositions = ArrayList()
        nextImagePosition = 0
        reachedEndOfFolder = false
        selectedImages = ArrayList()
        imagePositionAsPerIncreasingOrder = 0
        val diffResult = DiffUtil.calculateDiff(
            ImagesDiffCallback(oldImageList, newImageList)
        )
        images = newImageList
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * Set new selected images
     */
    fun setSelectedImages(newSelectedImages: ArrayList<Image>){
        selectedImages = ArrayList(newSelectedImages)
        imageSelectListener.onSelectedImagesChanged(selectedImages, 0)
    }
    /**
     * Refresh the data in the adapter
     */
    fun refresh(newImages: List<Image>, fixedImages: List<Image>) {
        numberOfSelectedImagesMarkedAsNotForUpload = 0
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
        val showAlreadyActionedImages =
            sharedPreferences.getBoolean(SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY, true)

        // While switch is on initializes the holder with all images size
        return if(showAlreadyActionedImages) {
            allImages.size

        // While switch is off and searching for next actionable has ended, initializes the holder
        // with size of all actionable images
        } else if (actionableImagesMap.size == allImages.size || reachedEndOfFolder) {
            actionableImagesMap.size

        // While switch is off, initializes the holder with and extra view holder so that finding
        // and addition of the next actionable image in the adapter can be continued
        } else {
            actionableImagesMap.size + 1
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
        private val uploadedGroup: Group = itemView.findViewById(R.id.uploaded_group)
        private val notForUploadGroup: Group = itemView.findViewById(R.id.not_for_upload_group)
        private val selectedGroup: Group = itemView.findViewById(R.id.selected_group)

        /**
         * Item selected view.
         */
        fun itemSelected() {
            selectedGroup.visibility = View.VISIBLE
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

    /**
     * Returns the text for showing inside the bubble during bubble scroll.
     */
    override fun getSectionName(position: Int): String {
        return images[position].date
    }

}