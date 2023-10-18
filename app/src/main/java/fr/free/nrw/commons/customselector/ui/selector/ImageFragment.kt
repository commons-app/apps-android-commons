package fr.free.nrw.commons.customselector.ui.selector

import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Switch
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.customselector.database.NotForUploadStatusDao
import fr.free.nrw.commons.customselector.database.UploadedStatusDao
import fr.free.nrw.commons.customselector.helper.ImageHelper
import fr.free.nrw.commons.customselector.helper.ImageHelper.CUSTOM_SELECTOR_PREFERENCE_KEY
import fr.free.nrw.commons.customselector.helper.ImageHelper.SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.listeners.PassDataListener
import fr.free.nrw.commons.customselector.listeners.RefreshUIListener
import fr.free.nrw.commons.customselector.model.CallbackStatus
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.model.Result
import fr.free.nrw.commons.customselector.ui.adapter.ImageAdapter
import fr.free.nrw.commons.databinding.FragmentCustomSelectorBinding
import fr.free.nrw.commons.databinding.ProgressDialogBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.upload.FileProcessor
import fr.free.nrw.commons.upload.FileUtilsWrapper
import java.util.*
import javax.inject.Inject

/**
 * Custom Selector Image Fragment.
 */
class ImageFragment : CommonsDaggerSupportFragment(), RefreshUIListener, PassDataListener {

    private var _binding: FragmentCustomSelectorBinding? = null
    private val binding get() = _binding

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
    private var switch: Switch? = null
    lateinit var filteredImages: ArrayList<Image>

    /**
     * Stores all images
     */
    var allImages: ArrayList<Image> = ArrayList()

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

    companion object {

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
        fun newInstance(bucketId: Long, lastItemId: Long): ImageFragment {
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
        viewModel = ViewModelProvider(requireActivity(), customSelectorViewModelFactory).get(
            CustomSelectorViewModel::class.java
        )
    }

    /**
     * OnCreateView
     * Init imageAdapter, gridLayoutManger.
     * SetUp recycler view.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCustomSelectorBinding.inflate(inflater, container, false)
        imageAdapter =
            ImageAdapter(requireActivity(), activity as ImageSelectListener, imageLoader!!)
        gridLayoutManager = GridLayoutManager(context, getSpanCount())
        with(binding?.selectorRv) {
            this?.layoutManager = gridLayoutManager
            this?.setHasFixedSize(true)
            this?.adapter = imageAdapter
        }

        viewModel?.result?.observe(viewLifecycleOwner, Observer {
            handleResult(it)
        })

        switch = binding?.switchWidget
        switch?.visibility = View.VISIBLE
        switch?.setOnCheckedChangeListener { _, isChecked -> onChangeSwitchState(isChecked) }
        selectorRV = binding?.selectorRv
        loader = binding?.loader
        progressLayout = binding?.progressLayout

        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, MODE_PRIVATE)
        showAlreadyActionedImages =
            sharedPreferences.getBoolean(SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY, true)
        switch?.isChecked = showAlreadyActionedImages

        val builder = AlertDialog.Builder(requireActivity())
        builder.setCancelable(false)
        progressDialogLayout = ProgressDialogBinding.inflate(layoutInflater, container, false)
        builder.setView(progressDialogLayout.root)
        progressDialog = builder.create()

        return binding?.root
    }

    private fun onChangeSwitchState(checked: Boolean) {
        if (checked) {
            showAlreadyActionedImages = true
            val sharedPreferences: SharedPreferences =
                requireContext().getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean(SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY, true)
            editor.apply()
        } else {
            showAlreadyActionedImages = false
            val sharedPreferences: SharedPreferences =
                requireContext().getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean(SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY, false)
            editor.apply()
        }

        imageAdapter.init(allImages, allImages, TreeMap())
        imageAdapter.notifyDataSetChanged()
    }

    /**
     * Attaching data listener
     */
    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        try {
            (getActivity() as CustomSelectorActivity).setOnDataListener(this)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /**
     * Handle view model result.
     */
    private fun handleResult(result: Result) {
        if (result.status is CallbackStatus.SUCCESS) {
            val images = result.images
            if (images.isNotEmpty()) {
                filteredImages = ImageHelper.filterImages(images, bucketId)
                allImages = ArrayList(filteredImages)
                imageAdapter.init(filteredImages, allImages, TreeMap())
                selectorRV?.let {
                    it.visibility = View.VISIBLE
                    lastItemId?.let { pos ->
                        (it.layoutManager as GridLayoutManager)
                            .scrollToPosition(ImageHelper.getIndexFromId(filteredImages, pos))
                    }
                }
            } else {
                binding?.emptyText?.let {
                    it.visibility = View.VISIBLE
                }
                selectorRV?.let {
                    it.visibility = View.GONE
                }
            }
        }
        loader?.let {
            it.visibility =
                if (result.status is CallbackStatus.FETCHING) View.VISIBLE else View.GONE
        }
    }

    /**
     * getSpanCount for GridViewManager.
     *
     * @return spanCount.
     */
    private fun getSpanCount(): Int {
        return 3
        // todo change span count depending on the device orientation and other factos.
    }

    /**
     * onResume
     * notifyDataSetChanged, rebuild the holder views to account for deleted images.
     */
    override fun onResume() {
        imageAdapter.notifyDataSetChanged()
        super.onResume()
    }

    /**
     * OnDestroy
     * Cleanup the imageLoader coroutine.
     * Save the Image Fragment state.
     */
    override fun onDestroy() {
        imageAdapter.cleanUp()

        val position = (selectorRV?.layoutManager as GridLayoutManager)
            .findFirstVisibleItemPosition()

        // Check for empty RecyclerView.
        if (position != -1) {
            context?.let { context ->
                context.getSharedPreferences(
                    "CustomSelector",
                    BaseActivity.MODE_PRIVATE
                )?.let { prefs ->
                    prefs.edit()?.let { editor ->
                        editor.putLong("ItemId", imageAdapter.getImageIdAt(position))?.apply()
                    }
                }
            }
        }
        super.onDestroy()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun refresh() {
        imageAdapter.refresh(filteredImages, allImages)
    }

    /**
     * Passes selected images and other information from Activity to Fragment and connects it with
     * the adapter
     */
    override fun passSelectedImages(selectedImages: ArrayList<Image>, shouldRefresh: Boolean) {
        imageAdapter.setSelectedImages(selectedImages)

        if (!showAlreadyActionedImages && shouldRefresh) {
            imageAdapter.init(filteredImages, allImages, TreeMap())
            imageAdapter.setSelectedImages(selectedImages)
        }
    }

    /**
     * Shows mark/unmark progress dialog
     */
    fun showMarkUnmarkProgressDialog(text: String) {
        if (!progressDialog.isShowing) {
            progressDialogLayout.progressDialogText.text = text
            progressDialog.show()
        }
    }

    /**
     * Dismisses mark/unmark progress dialog
     */
    fun dismissMarkUnmarkProgressDialog() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

}