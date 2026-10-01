package fr.free.nrw.commons.customselector.ui.adapter

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.customselector.helper.CustomSelectorConstants.MAX_IMAGE_COUNT
import fr.free.nrw.commons.customselector.helper.ImageHelper
import fr.free.nrw.commons.customselector.helper.ImageHelper.CUSTOM_SELECTOR_PREFERENCE_KEY
import fr.free.nrw.commons.customselector.helper.ImageHelper.SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.selector.ImageLoader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.collections.ArrayList

/**
 * Custom selector ImageAdapter.
 */
class ImageAdapter(
    /**
     * Application Context.
     */
    private val context: Context,
    /**
     * Image select listener for click events on image.
     */
    private var imageSelectListener: ImageSelectListener,
    /**
     * ImageLoader queries images.
     */
    private var imageLoader: ImageLoader,
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>(),
    FastScrollRecyclerView.SectionedAdapter {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

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
     * Actionable images in display order when "Show already actioned pictures" is off.
     * Adapter position always maps to the same [Image] for bind and selection.
     */
    private val actionableImages = ArrayList<Image>()

    private var uploadingContributionList: List<Contribution> = ArrayList()

    /**
     * Next starting index to initiate query to find next actionable image
     */
    private var nextImagePosition = 0

    /**
     * Stores the number of images currently visible on the screen
     */
    private val _currentImagesCount = MutableStateFlow(0)
    val currentImagesCount = _currentImagesCount

    /**
     * Stores whether images are being loaded or not
     */
    private val _isLoadingImages = MutableStateFlow(false)
    val isLoadingImages = _isLoadingImages

    /**
     * Coroutine Dispatchers and Scope.
     */
    private var defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    private var ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val scope: CoroutineScope = MainScope()
    private val loadMutex = Mutex()
    private var isLoadingActionables = false
    private var loadGeneration = 0

    //maximum number of images that can be selected.
    private var maxUploadLimit: Int = MAX_IMAGE_COUNT


    //set maximum number of images allowed for upload.
    fun setMaxUploadLimit(limit: Int) {
        maxUploadLimit = limit
    }

    private fun showAlreadyActionedImages(): Boolean {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, 0)
        return sharedPreferences.getBoolean(SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY, true)
    }

    private fun resolveImageAt(position: Int, showAlreadyActionedImages: Boolean): Image? =
        if (showAlreadyActionedImages) {
            images.getOrNull(position)
        } else {
            actionableImages.getOrNull(position)
        }

    /**
     * Create View holder.
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ImageViewHolder {
        val itemView = inflater.inflate(R.layout.item_custom_selector_image, parent, false)
        return ImageViewHolder(itemView)
    }

    /**
     * Bind View holder, load image, selected view, click listeners.
     */
    override fun onBindViewHolder(
        holder: ImageViewHolder,
        position: Int,
    ) {
        val showAlreadyActionedImages = showAlreadyActionedImages()
        if (showAlreadyActionedImages && images.isEmpty()) {
            return
        }

        val image = resolveImageAt(position, showAlreadyActionedImages)
        if (image == null) {
            holder.image.setImageDrawable(null)
            holder.itemUnselected()
            holder.itemView.setOnClickListener(null)
            holder.itemView.setOnLongClickListener(null)
            return
        }

        holder.image.setImageDrawable(null)
        if (context.contentResolver.getType(image.uri) == null) {
            // Image does not exist anymore, update adapter.
            holder.itemView.post {
                if (showAlreadyActionedImages) {
                    val updatedPosition = images.indexOf(image)
                    images.remove(image)
                    notifyItemRemoved(updatedPosition)
                    notifyItemRangeChanged(updatedPosition, images.size)
                } else {
                    val updatedPosition = actionableImages.indexOf(image)
                    if (updatedPosition != -1) {
                        actionableImages.removeAt(updatedPosition)
                        _currentImagesCount.value = actionableImages.size
                        notifyItemRemoved(updatedPosition)
                        notifyItemRangeChanged(updatedPosition, itemCount)
                    }
                }
            }
        } else {
            val selectedIndex: Int =
                if (showAlreadyActionedImages) {
                    ImageHelper.getIndex(selectedImages, image)
                } else {
                    ImageHelper.getIndex(selectedImages, actionableImages[position])
                }

            val isSelected = selectedIndex != -1
            if (isSelected) {
                holder.itemSelected()
            } else {
                holder.itemUnselected()
            }
            imageLoader.queryAndSetView(
                holder,
                image,
                ioDispatcher,
                defaultDispatcher,
                uploadingContributionList,
            )
            Glide
                .with(holder.image)
                .load(image.uri)
                .thumbnail(0.3f)
                .into(holder.image)

            holder.itemView.setOnClickListener {
                onThumbnailClicked(position, holder)
            }

            // launch media preview on long click.
            holder.itemView.setOnLongClickListener {
                imageSelectListener.onLongPress(
                    allImages.indexOf(image),
                    ArrayList(allImages),
                    selectedImages,
                )
                true
            }
        }
    }

    private fun startLoadingActionables(generation: Int) {
        if (isLoadingActionables) {
            return
        }
        isLoadingActionables = true
        _isLoadingImages.value = true

        scope.launch {
            loadMutex.withLock {
                while (generation == loadGeneration) {
                    val next =
                        imageLoader.nextActionableImage(
                            allImages,
                            ioDispatcher,
                            defaultDispatcher,
                            nextImagePosition,
                            uploadingContributionList,
                        )
                    if (next == -1) {
                        reachedEndOfFolder = true
                        break
                    }
                    if (generation != loadGeneration) {
                        return@launch
                    }
                    nextImagePosition = next + 1
                    val loadedImage = allImages[next]
                    val insertIndex = actionableImages.size
                    actionableImages.add(loadedImage)
                    _currentImagesCount.value = actionableImages.size
                    withContext(Dispatchers.Main) {
                        if (generation == loadGeneration) {
                            notifyItemInserted(insertIndex)
                        }
                    }
                }
            }
            if (generation == loadGeneration) {
                withContext(Dispatchers.Main) {
                    if (reachedEndOfFolder && !showAlreadyActionedImages()) {
                        notifyItemRemoved(actionableImages.size)
                    }
                }
                _isLoadingImages.value = false
                isLoadingActionables = false
            }
        }
    }

    /**
     * Handles click on thumbnail
     */
    private fun onThumbnailClicked(
        position: Int,
        holder: ImageViewHolder,
    ) {
        val switchState = showAlreadyActionedImages()

        if (!switchState) {
            if (position < actionableImages.size) {
                selectOrRemoveImage(holder, position)
            }
        } else {
            selectOrRemoveImage(holder, position)
        }
    }

    /**
     * Handle click event on an image, update counter on images.
     */
    private fun selectOrRemoveImage(
        holder: ImageViewHolder,
        position: Int,
    ) {
        val showAlreadyActionedImages = showAlreadyActionedImages()

        if (singleSelection) {
            // If single selection mode, clear previous selection and select only the new one
            if (selectedImages.isNotEmpty()) {
                val currentImage =
                    if (showAlreadyActionedImages) {
                        images.getOrNull(position)
                    } else {
                        actionableImages.getOrNull(position)
                    }
                if (currentImage != null && selectedImages[0] != currentImage) {
                    val prevIndex =
                        if (showAlreadyActionedImages) {
                            images.indexOf(selectedImages[0])
                        } else {
                            actionableImages.indexOf(selectedImages[0])
                        }
                    selectedImages.clear()
                    if (prevIndex != -1) {
                        notifyItemChanged(prevIndex, ImageUnselected())
                    }
                }
            }
        }

        if (!showAlreadyActionedImages && position >= actionableImages.size) {
            return
        }

        val clickedIndex: Int =
            if (showAlreadyActionedImages) {
                ImageHelper.getIndex(selectedImages, images[position])
            } else {
                ImageHelper.getIndex(selectedImages, actionableImages[position])
            }

        if (clickedIndex != -1) {
            selectedImages.removeAt(clickedIndex)
            if (holder.isItemNotForUpload()) {
                numberOfSelectedImagesMarkedAsNotForUpload--
            }
            notifyItemChanged(position, ImageUnselected())
            // Notify listener of deselection to update UI
            imageSelectListener.onSelectedImagesChanged(selectedImages, numberOfSelectedImagesMarkedAsNotForUpload)
        } else {
            //check the maximum limit before allowing the selection
            if (!singleSelection && selectedImages.size >= maxUploadLimit) {
                // limit reached, show a toast and prevent selection
                Toast.makeText(
                    context,
                    context.getString(
                        R.string.custom_selector_max_image_limit_reached,
                        maxUploadLimit
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                return //exit the function, preventing selection
            }

            // Prevent adding the same image multiple times
            val image =
                if (showAlreadyActionedImages) {
                    images[position]
                } else {
                    actionableImages[position]
                }
            if (selectedImages.contains(image)) {
                return // Image already selected, ignore additional clicks
            }
            scope.launch(ioDispatcher) {
                val imageSHA1 = imageLoader.getSHA1(image, defaultDispatcher)
                withContext(Dispatchers.Main) {
                    if (holder.isItemUploaded()) {
                        Toast.makeText(context, R.string.custom_selector_already_uploaded_image_text, Toast.LENGTH_SHORT).show()
                        return@withContext
                    }

                    if (imageSHA1.isNotEmpty() && imageLoader.getFromUploaded(imageSHA1) != null) {
                        holder.itemUploaded()
                        Toast.makeText(context, R.string.custom_selector_already_uploaded_image_text, Toast.LENGTH_SHORT).show()
                        return@withContext
                    }

                    if (!holder.isItemUploaded() && imageSHA1.isNotEmpty() && imageLoader.getFromUploaded(imageSHA1) != null) {
                        Toast.makeText(context, R.string.custom_selector_already_uploaded_image_text, Toast.LENGTH_SHORT).show()
                    }

                    if (holder.isItemNotForUpload()) {
                        numberOfSelectedImagesMarkedAsNotForUpload++
                    }
                    selectedImages.add(image)
                    notifyItemChanged(position, ImageSelectedOrUpdated())
                    imageSelectListener.onSelectedImagesChanged(selectedImages, numberOfSelectedImagesMarkedAsNotForUpload)
                }
            }
        }
    }

    /**
     * Initialize the data set.
     */
    fun init(
        newImages: List<Image>,
        fixedImages: List<Image>,
        uploadedImages: List<Contribution> = ArrayList(),
    ) {
        _isLoadingImages.value = true
        allImages = fixedImages
        val oldImageList: ArrayList<Image> = images
        val newImageList: ArrayList<Image> = ArrayList(newImages)
        loadGeneration++
        val currentGeneration = loadGeneration
        actionableImages.clear()
        uploadingContributionList = uploadedImages
        nextImagePosition = 0
        reachedEndOfFolder = false
        isLoadingActionables = false
        selectedImages = ArrayList()
        _currentImagesCount.value = 0
        val diffResult =
            DiffUtil.calculateDiff(
                ImagesDiffCallback(oldImageList, newImageList),
            )
        images = newImageList
        diffResult.dispatchUpdatesTo(this)

        if (!showAlreadyActionedImages()) {
            startLoadingActionables(currentGeneration)
        } else {
            _isLoadingImages.value = false
        }
    }

    /**
     * Set new selected images
     */
    fun setSelectedImages(newSelectedImages: ArrayList<Image>) {
        selectedImages = ArrayList(newSelectedImages)
        imageSelectListener.onSelectedImagesChanged(selectedImages, 0)
    }

    /**
     * Refresh the data in the adapter
     */
    fun refresh(
        newImages: List<Image>,
        fixedImages: List<Image>,
        uploadingImages: List<Contribution> = ArrayList(),
    ) {
        numberOfSelectedImagesMarkedAsNotForUpload = 0
        images.clear()
        selectedImages = arrayListOf()
        init(newImages, fixedImages, uploadingImages)
        notifyDataSetChanged()
    }

    /**
     * Clear selected images and empty the list.
     */
    fun clearSelectedImages() {
        numberOfSelectedImagesMarkedAsNotForUpload = 0
        selectedImages.clear()
        selectedImages = arrayListOf()
    }

    /**
     * Remove image from actionable images list.
     */
    fun removeImageFromActionableImageMap(image: Image) {
        val showAlreadyActionedImages = showAlreadyActionedImages()

        if (showAlreadyActionedImages) {
            refresh(allImages, allImages, uploadingContributionList)
        } else {
            val index = actionableImages.indexOf(image)
            if (index != -1) {
                actionableImages.removeAt(index)
                _currentImagesCount.value = actionableImages.size
                notifyItemRemoved(index)
                notifyItemRangeChanged(index, itemCount)
            }
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int {
        val showAlreadyActionedImages = showAlreadyActionedImages()

        return if (showAlreadyActionedImages) {
            allImages.size
        } else if (reachedEndOfFolder) {
            actionableImages.size
        } else {
            actionableImages.size + 1
        }
    }

    fun getImageIdAt(position: Int): Long {
        val showAlreadyActionedImages = showAlreadyActionedImages()
        return resolveImageAt(position, showAlreadyActionedImages)?.id ?: 0L
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
    class ImageViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.image_thumbnail)
        private val uploadedGroup: Group = itemView.findViewById(R.id.uploaded_group)
        private val uploadingGroup: Group = itemView.findViewById(R.id.uploading_group)
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
         * Item is uploading
         */
        fun itemUploading() {
            uploadingGroup.visibility = View.VISIBLE
        }

        /**
         * Item is not for upload view
         */
        fun itemNotForUpload() {
            notForUploadGroup.visibility = View.VISIBLE
        }

        fun isItemUploaded(): Boolean = uploadedGroup.visibility == View.VISIBLE

        /**
         * Item is not for upload
         */
        fun isItemNotForUpload(): Boolean = notForUploadGroup.visibility == View.VISIBLE

        /**
         * Item is not uploading
         */
        fun itemNotUploading() {
            uploadingGroup.visibility = View.GONE
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
        var newImageList: ArrayList<Image>,
    ) : DiffUtil.Callback() {
        /**
         * Returns the size of the old list.
         */
        override fun getOldListSize(): Int = oldImageList.size

        /**
         * Returns the size of the new list.
         */
        override fun getNewListSize(): Int = newImageList.size

        /**
         * Called by the DiffUtil to decide whether two object represent the same Item.
         */
        override fun areItemsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int,
        ): Boolean = newImageList[newItemPosition].id == oldImageList[oldItemPosition].id

        /**
         * Called by the DiffUtil when it wants to check whether two items have the same data.
         * DiffUtil uses this information to detect if the contents of an item has changed.
         */
        override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int,
        ): Boolean = oldImageList[oldItemPosition].equals(newImageList[newItemPosition])
    }

    /**
     * Returns the text for showing inside the bubble during bubble scroll.
     */
    override fun getSectionName(position: Int): String {
        val showAlreadyActionedImages = showAlreadyActionedImages()
        return resolveImageAt(position, showAlreadyActionedImages)?.date ?: ""
    }

    private var singleSelection: Boolean = false

    /**
     * Set single selection mode
     */
    fun setSingleSelection(single: Boolean) {
        singleSelection = single
    }
}
