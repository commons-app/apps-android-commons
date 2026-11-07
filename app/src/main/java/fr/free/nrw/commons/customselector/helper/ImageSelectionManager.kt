package fr.free.nrw.commons.customselector.helper

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.helper.CustomSelectorConstants.MAX_IMAGE_COUNT
import fr.free.nrw.commons.customselector.helper.ImageHelper.CUSTOM_SELECTOR_PREFERENCE_KEY
import fr.free.nrw.commons.customselector.helper.ImageHelper.SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.adapter.ImageAdapter
import fr.free.nrw.commons.customselector.ui.selector.ImageLoader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TreeMap

/**
 * For managing selection state, selection limits and click toggle logic for custom selector images.
 */
class ImageSelectionManager(
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
    /**
     * Default coroutine dispatcher.
     */
    private var defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    /**
     * IO coroutine dispatcher.
     */
    private var ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    /**
     * Coroutine scope.
     */
    private val scope: CoroutineScope = MainScope(),
) {
    /**
     * Currently selected images.
     */
    var selectedImages = arrayListOf<Image>()
        private set

    /**
     * Number of selected images that are marked as not for upload
     */
    var numberOfSelectedImagesMarkedAsNotForUpload = 0

    // maximum number of images that can be selected.
    var maxUploadLimit: Int = MAX_IMAGE_COUNT

    var singleSelection: Boolean = false

    /**
     * Checks whether an image at the given position is currently selected.
     */
    fun isSelected(
        position: Int,
        image: Image,
        showAlreadyActionedImages: Boolean,
        actionableImages: List<Image>,
    ): Boolean {
        // Getting selected index when switch is on
        val selectedIndex: Int =
            if (showAlreadyActionedImages) {
                ImageHelper.getIndex(selectedImages, image)

                // Getting selected index when switch is off
            } else if (actionableImages.size > position) {
                ImageHelper.getIndex(selectedImages, actionableImages[position])

                // For any other case return -1
            } else {
                -1
            }
        return selectedIndex != -1
    }

    /**
     * Handles click on thumbnail
     */
    fun onThumbnailClicked(
        position: Int,
        holder: ImageAdapter.ImageViewHolder,
        images: List<Image>,
        actionableImagesMap: TreeMap<Int, Image>,
        notifyItemChanged: (Int, Any?) -> Unit,
    ) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, 0)
        val switchState =
            sharedPreferences.getBoolean(SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY, true)

        // While switch is turned off, lets user click on image only if the position is
        // added inside map
        if (!switchState) {
            if (actionableImagesMap.size > position) {
                selectOrRemoveImage(holder, position, images, actionableImagesMap, notifyItemChanged)
            }
        } else {
            selectOrRemoveImage(holder, position, images, actionableImagesMap, notifyItemChanged)
        }
    }

    /**
     * Handle click event on an image, update counter on images.
     */
    private fun selectOrRemoveImage(
        holder: ImageAdapter.ImageViewHolder,
        position: Int,
        images: List<Image>,
        actionableImagesMap: TreeMap<Int, Image>,
        notifyItemChanged: (Int, Any?) -> Unit,
    ) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, 0)
        val showAlreadyActionedImages =
            sharedPreferences.getBoolean(SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY, true)

        val image =
            if (showAlreadyActionedImages) {
                images[position]
            } else {
                ArrayList(actionableImagesMap.values)[position]
            }

        if (singleSelection) {
            // If single selection mode, clear previous selection and select only the new one
            if (selectedImages.isNotEmpty() && (selectedImages[0] != image)) {
                val prevIndex = images.indexOf(selectedImages[0])
                selectedImages.clear()
                notifyItemChanged(prevIndex, ImageAdapter.ImageUnselected())
            }
        }

        // Getting clicked index from all images index when show_already_actioned_images
        // switch is on
        val clickedIndex: Int = ImageHelper.getIndex(selectedImages, image)

        if (clickedIndex != -1) {
            deselectImage(holder, position, clickedIndex, notifyItemChanged)
        } else {
            selectImage(holder, position, image, notifyItemChanged)
        }
    }

    /**
     * Deselects an image that was previously selected.
     */
    private fun deselectImage(
        holder: ImageAdapter.ImageViewHolder,
        position: Int,
        clickedIndex: Int,
        notifyItemChanged: (Int, Any?) -> Unit,
    ) {
        selectedImages.removeAt(clickedIndex)
        if (holder.isItemNotForUpload()) {
            numberOfSelectedImagesMarkedAsNotForUpload--
        }
        notifyItemChanged(position, ImageAdapter.ImageUnselected())
        // Notify listener of deselection to update UI
        imageSelectListener.onSelectedImagesChanged(
            selectedImages,
            numberOfSelectedImagesMarkedAsNotForUpload,
        )
    }

    /**
     * Validates limits and selects an image.
     */
    private fun selectImage(
        holder: ImageAdapter.ImageViewHolder,
        position: Int,
        image: Image,
        notifyItemChanged: (Int, Any?) -> Unit,
    ) {
        // check the maximum limit before allowing the selection
        if (!singleSelection && selectedImages.size >= maxUploadLimit) {
            // limit reached, show a toast and prevent selection
            Toast
                .makeText(
                    context,
                    context.getString(
                        R.string.custom_selector_max_image_limit_reached,
                        maxUploadLimit,
                    ),
                    Toast.LENGTH_SHORT,
                ).show()
            return // exit the function, preventing selection
        }

        // Prevent adding the same image multiple times
        if (selectedImages.contains(image)) {
            return // Image already selected, ignore additional clicks
        }
        scope.launch(ioDispatcher) {
            val imageSHA1 = imageLoader.getSHA1(image, defaultDispatcher)
            withContext(Dispatchers.Main) {
                handleUploadedCheckAndSelection(
                    holder,
                    position,
                    image,
                    imageSHA1,
                    notifyItemChanged,
                )
            }
        }
    }

    /**
     * Handles upload verification and adds the image to selection.
     */
    private suspend fun handleUploadedCheckAndSelection(
        holder: ImageAdapter.ImageViewHolder,
        position: Int,
        image: Image,
        imageSHA1: String,
        notifyItemChanged: (Int, Any?) -> Unit,
    ) {
        if (holder.isItemUploaded()) {
            Toast
                .makeText(
                    context,
                    R.string.custom_selector_already_uploaded_image_text,
                    Toast.LENGTH_SHORT,
                ).show()
            return
        }

        if (imageSHA1.isNotEmpty() && imageLoader.getFromUploaded(imageSHA1) != null) {
            holder.itemUploaded()
            Toast
                .makeText(
                    context,
                    R.string.custom_selector_already_uploaded_image_text,
                    Toast.LENGTH_SHORT,
                ).show()
            return
        }

        if (!holder.isItemUploaded() &&
            imageSHA1.isNotEmpty() &&
            imageLoader.getFromUploaded(imageSHA1) != null
        ) {
            Toast
                .makeText(
                    context,
                    R.string.custom_selector_already_uploaded_image_text,
                    Toast.LENGTH_SHORT,
                ).show()
        }

        if (holder.isItemNotForUpload()) {
            numberOfSelectedImagesMarkedAsNotForUpload++
        }
        selectedImages.add(image)
        notifyItemChanged(position, ImageAdapter.ImageSelectedOrUpdated())
        imageSelectListener.onSelectedImagesChanged(
            selectedImages,
            numberOfSelectedImagesMarkedAsNotForUpload,
        )
    }

    /**
     * Set new selected images
     */
    fun setSelectedImages(newSelectedImages: ArrayList<Image>) {
        selectedImages = ArrayList(newSelectedImages)
        imageSelectListener.onSelectedImagesChanged(selectedImages, 0)
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
     * Resets the selection state for a fresh data set.
     */
    fun reset() {
        numberOfSelectedImagesMarkedAsNotForUpload = 0
        selectedImages = ArrayList()
    }
}
