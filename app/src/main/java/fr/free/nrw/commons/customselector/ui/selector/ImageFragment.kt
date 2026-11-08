package fr.free.nrw.commons.customselector.ui.selector

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.customselector.database.NotForUploadStatusDao
import fr.free.nrw.commons.customselector.database.UploadedStatusDao
import fr.free.nrw.commons.customselector.helper.ImageFolderManager
import fr.free.nrw.commons.customselector.helper.ImageHelper
import fr.free.nrw.commons.customselector.helper.ImageHelper.CUSTOM_SELECTOR_PREFERENCE_KEY
import fr.free.nrw.commons.customselector.helper.ImageHelper.SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.listeners.RefreshUIListener
import fr.free.nrw.commons.customselector.model.CallbackStatus
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.adapter.ImageAdapter
import fr.free.nrw.commons.databinding.FragmentCustomSelectorBinding
import fr.free.nrw.commons.databinding.ProgressDialogBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.upload.FileProcessor
import fr.free.nrw.commons.upload.FileUtilsWrapper
import fr.free.nrw.commons.utils.applyEdgeToEdgeBottomPaddingInsets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.collections.ArrayList

/**
 * Custom Selector Image Fragment.
 */
class ImageFragment :
    CommonsDaggerSupportFragment(),
    RefreshUIListener {
    private var _binding: FragmentCustomSelectorBinding? = null
    val binding get() = _binding

    /**
     * Current bucketId.
     */
    private var bucketId: Long? = null

    /**
     * Last ImageItem Id.
     */
    private var lastItemId: Long? = null

    /**
     * View model for images.
     */
    private var viewModel: CustomSelectorViewModel? = null

    /**
     * View Elements.
     */
    private var selectorRV: RecyclerView? = null
    private var loader: ProgressBar? = null
    private var switch: SwitchMaterial? = null
    private var filteredImages: ArrayList<Image> = arrayListOf()

    /**
     * Stores all images
     */
    var allImages: ArrayList<Image> = ArrayList()

    /**
     * Keeps track of switch state
     */
    private val _switchState = MutableStateFlow(false)
    val switchState = _switchState.asStateFlow()

    /**
     * View model Factory.
     */
    lateinit var customSelectorViewModelFactory: CustomSelectorViewModelFactory
        @Inject set

    /**
     * Image loader for adapter.
     */
    var imageLoader: ImageLoader? = null
        @Inject set

    /**
     * Image Adapter for recycle view.
     */
    private lateinit var imageAdapter: ImageAdapter

    /**
     * GridLayoutManager for recycler view.
     */
    private lateinit var gridLayoutManager: GridLayoutManager

    /**
     * For showing progress
     */
    private var progressLayout: ConstraintLayout? = null

    private lateinit var progressDialog: AlertDialog
    private lateinit var progressDialogLayout: ProgressDialogBinding

    /**
     * NotForUploadStatus Dao class for database operations
     */
    @Inject
    lateinit var notForUploadStatusDao: NotForUploadStatusDao

    /**
     * UploadedStatus Dao class for database operations
     */
    @Inject
    lateinit var uploadedStatusDao: UploadedStatusDao

    /**
     * FileUtilsWrapper class to get imageSHA1 from uri
     */
    @Inject
    lateinit var fileUtilsWrapper: FileUtilsWrapper

    /**
     * FileProcessor to pre-process the file.
     */
    @Inject
    lateinit var fileProcessor: FileProcessor

    /**
     * MediaClient for SHA1 query.
     */
    @Inject
    lateinit var mediaClient: MediaClient

    @Inject
    lateinit var contributionDao: ContributionDao

    /**
     * Folder manager for custom selector folder operations.
     */
    @Inject
    lateinit var folderManager: ImageFolderManager

    companion object {
        // todo change span count depending on the device orientation and other factos.
        private const val SPAN_COUNT = 3

        /**
         * Switch state
         */
        var showAlreadyActionedImages: Boolean = true

        /**
         * BucketId args name
         */
        const val BUCKET_ID = "BucketId"
        const val LAST_ITEM_ID = "LastItemId"

        /**
         * newInstance from bucketId.
         */
        fun newInstance(
            bucketId: Long,
            lastItemId: Long,
        ): ImageFragment {
            val fragment = ImageFragment()
            val args = Bundle()
            args.putLong(BUCKET_ID, bucketId)
            args.putLong(LAST_ITEM_ID, lastItemId)
            fragment.arguments = args
            return fragment
        }
    }

    /**
     * OnCreate
     * Get BucketId, view Model.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bucketId = arguments?.getLong(BUCKET_ID)
        lastItemId = arguments?.getLong(LAST_ITEM_ID, 0)
        viewModel =
            ViewModelProvider(requireActivity(), customSelectorViewModelFactory).get(
                CustomSelectorViewModel::class.java,
            )
        try {
            (activity as? CustomSelectorActivity)?.setOnDataListener(this)
        } catch (ex: Exception) {
            Timber.e(ex, "Could not set data listener on CustomSelectorActivity")
        }
        val builder = AlertDialog.Builder(requireActivity()).setCancelable(false)
        progressDialogLayout = ProgressDialogBinding.inflate(layoutInflater)
        progressDialog = builder.setView(progressDialogLayout.root).create()
        val prefs = requireContext().getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, MODE_PRIVATE)
        showAlreadyActionedImages = prefs.getBoolean(SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY, true)
    }

    /**
     * OnCreateView
     * Init imageAdapter, gridLayoutManger.
     * SetUp recycler view.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentCustomSelectorBinding.inflate(inflater, container, false)

        // ensures imageAdapter is initialized
        if (!::imageAdapter.isInitialized) {
            imageAdapter = ImageAdapter(requireActivity(), activity as ImageSelectListener, imageLoader!!)
            Timber.d("Initialized imageAdapter in onCreateView")
        }
        // Set single selection mode if needed
        imageAdapter.selectionManager.singleSelection =
            (activity as? CustomSelectorActivity)
                ?.intent
                ?.getBooleanExtra(CustomSelectorActivity.EXTRA_SINGLE_SELECTION, false) == true
        gridLayoutManager = GridLayoutManager(context, SPAN_COUNT)
        selectorRV =
            binding?.selectorRv?.apply {
                applyEdgeToEdgeBottomPaddingInsets()
                layoutManager = gridLayoutManager
                setHasFixedSize(true)
                adapter = imageAdapter
            }
        loader = binding?.loader
        progressLayout = binding?.progressLayout
        switch =
            binding?.switchWidget?.apply {
                visibility = View.VISIBLE
                _switchState.value = isChecked
                setOnCheckedChangeListener { _, isChecked ->
                    folderManager.onSwitchChanged(isChecked, allImages, imageAdapter, requireContext())
                    _switchState.value = isChecked
                }
                this.isChecked = showAlreadyActionedImages
            }
        return binding?.root
    }

    /**
     * onViewCreated
     * Updates empty text view visibility based on image count, switch state, and loading status.
     */
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel?.result?.observe(
            viewLifecycleOwner,
            Observer { result ->
                loader?.visibility = if (result.status is CallbackStatus.FETCHING) View.VISIBLE else View.GONE
                if (result.status is CallbackStatus.SUCCESS) {
                    if (result.images.isNotEmpty()) {
                        allImages =
                            folderManager.processResultImages(
                                result.images,
                                bucketId,
                                imageAdapter,
                                viewModel?.selectedImages?.value,
                            )
                        filteredImages = allImages
                        selectorRV?.visibility = View.VISIBLE
                        if (switch?.isChecked == false && lastItemId != null) {
                            val pos = ImageHelper.getIndexFromId(allImages, lastItemId!!)
                            (selectorRV?.layoutManager as? GridLayoutManager)?.scrollToPosition(pos)
                        }
                    } else {
                        filteredImages = ArrayList()
                        allImages = filteredImages
                        binding?.emptyText?.visibility = View.VISIBLE
                        selectorRV?.visibility = View.GONE
                    }
                }
            },
        )
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    imageAdapter.currentImagesCount,
                    switchState,
                    imageAdapter.isLoadingImages,
                ) { imageCount, isChecked, isLoadingImages ->
                    Triple(imageCount, isChecked, isLoadingImages)
                }.collect { (imageCount, isChecked, isLoadingImages) ->
                    binding?.allImagesUploadedOrMarked?.isVisible =
                        !isLoadingImages &&
                        !isChecked &&
                        imageCount == 0 &&
                        (switch?.isVisible == true)
                }
            }
        }
    }

    /**
     * onResume
     * notifyDataSetChanged, rebuild the holder views to account for deleted images.
     */
    override fun onResume() {
        if (::imageAdapter.isInitialized) {
            imageAdapter.notifyDataSetChanged()
            Timber.d("Notified imageAdapter in onResume")
        } else {
            Timber.w("imageAdapter not initialized in onResume")
        }
        super.onResume()
    }

    override fun onDestroyView() {
        val position =
            (selectorRV?.layoutManager as? GridLayoutManager)
                ?.findFirstVisibleItemPosition() ?: -1
        val adapter = if (::imageAdapter.isInitialized) imageAdapter else null
        folderManager.cleanup(adapter)
        folderManager.saveLastVisibleItemId(position, filteredImages, adapter, context)
        _binding = null
        selectorRV = null
        loader = null
        switch = null
        progressLayout = null
        super.onDestroyView()
    }

    override fun refresh() {
        if (::imageAdapter.isInitialized) {
            val uploadingContributions = folderManager.getUploadingContributions()
            imageAdapter.refresh(filteredImages, allImages, uploadingContributions)
            Timber.d("Refreshed imageAdapter")
        } else {
            Timber.w("imageAdapter not initialized in refresh")
        }
    }

    /**
     * Removes the image from the actionable image map
     */
    fun removeImage(image: Image) {
        if (::imageAdapter.isInitialized) {
            imageAdapter.removeImageFromActionableImageMap(image)
            Timber.d("Removed image from actionable image map")
        } else {
            Timber.w("imageAdapter not initialized in removeImage")
        }
    }

    /**
     * Clears the selected images
     */
    fun clearSelectedImages() {
        if (::imageAdapter.isInitialized) {
            imageAdapter.selectionManager.clearSelectedImages()
            Timber.d("Cleared selected images")
        } else {
            Timber.w("imageAdapter not initialized in clearSelectedImages")
        }
    }

    /**
     * Shows mark/unmark progress dialog
     */
    fun showMarkUnmarkProgressDialog(text: String) {
        if (!progressDialog.isShowing) {
            progressDialogLayout.progressDialogText.text = text
            progressDialog.show()
            Timber.d("Showing mark/unmark progress dialog: %s", text)
        }
    }

    /**
     * Dismisses mark/unmark progress dialog
     */
    fun dismissMarkUnmarkProgressDialog() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
            Timber.d("Dismissed mark/unmark progress dialog")
        }
    }
}
