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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.database.NotForUploadStatusDao
import fr.free.nrw.commons.customselector.database.UploadedStatusDao
import fr.free.nrw.commons.customselector.helper.ImageHelper
import fr.free.nrw.commons.customselector.helper.ImageHelper.CUSTOM_SELECTOR_PREFERENCE_KEY
import fr.free.nrw.commons.customselector.helper.ImageHelper.SWITCH_STATE_PREFERENCE_KEY
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.listeners.RefreshUIListener
import fr.free.nrw.commons.customselector.model.CallbackStatus
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.model.Result
import fr.free.nrw.commons.customselector.ui.adapter.ImageAdapter
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.upload.FileProcessor
import fr.free.nrw.commons.upload.FileUtilsWrapper
import kotlinx.android.synthetic.main.fragment_custom_selector.*
import kotlinx.android.synthetic.main.fragment_custom_selector.view.*
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

/**
 * Custom Selector Image Fragment.
 */
class ImageFragment: CommonsDaggerSupportFragment(), RefreshUIListener {

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
    private var  viewModel: CustomSelectorViewModel? = null

    /**
     * View Elements.
     */
    private var selectorRV: RecyclerView? = null
    private var loader: ProgressBar? = null
    private var switch: Switch? = null
    lateinit var filteredImages: ArrayList<Image>;

    /**
     * Stores all images
     */
    var allImages: ArrayList<Image> = ArrayList()

    /**
     * Hashmap to store actioned images
     */
    private var actionedImages: TreeMap<Int,Image> = TreeMap()

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

    /**
     * Coroutine Dispatchers and Scope.
     */
    private val scope : CoroutineScope = MainScope()
    private var ioDispatcher : CoroutineDispatcher = Dispatchers.IO
    private var defaultDispatcher : CoroutineDispatcher = Dispatchers.Default

    companion object {

        /**
         * Switch state
         */
        var switchState: Boolean = true

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
        viewModel = ViewModelProvider(requireActivity(),customSelectorViewModelFactory).get(CustomSelectorViewModel::class.java)
    }

    /**
     * OnCreateView
     * Init imageAdapter, gridLayoutManger.
     * SetUp recycler view.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val root = inflater.inflate(R.layout.fragment_custom_selector, container, false)
        imageAdapter = ImageAdapter(requireActivity(), activity as ImageSelectListener, imageLoader!!)
        gridLayoutManager = GridLayoutManager(context,getSpanCount())
        with(root.selector_rv){
            this.layoutManager = gridLayoutManager
            setHasFixedSize(true)
            this.adapter = imageAdapter
        }

        viewModel?.result?.observe(viewLifecycleOwner, Observer{
            handleResult(it)
        })

        switch = root.switchWidget
        switch?.visibility = View.VISIBLE
        switch?.setOnCheckedChangeListener { _, isChecked -> onChangeSwitchState(isChecked) }
        selectorRV = root.selector_rv
        loader = root.loader
        progressLayout = root.progressLayout

        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, MODE_PRIVATE)
        switchState = sharedPreferences.getBoolean(SWITCH_STATE_PREFERENCE_KEY, true)
        switch?.isChecked = switchState
        switch?.text =
            if (switchState) getString(R.string.hide_already_actioned_pictures)
            else getString(R.string.show_already_actioned_pictures)

        return root
    }

    private fun onChangeSwitchState(checked: Boolean) {
        if (checked) {
            switchState = true
            val sharedPreferences: SharedPreferences =
                requireContext().getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean(SWITCH_STATE_PREFERENCE_KEY, true)
            editor.apply()
            switch?.text = getString(R.string.hide_already_actioned_pictures)

            actionedImages.clear()
            imageAdapter.init(allImages, allImages)
            imageAdapter.notifyDataSetChanged()
        } else {
            switchState = false
            val sharedPreferences: SharedPreferences =
                requireContext().getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean(SWITCH_STATE_PREFERENCE_KEY, false)
            editor.apply()
            switch?.text = getString(R.string.show_already_actioned_pictures)

            filteredImages = imageAdapter.getFilteredImages()
            scope.launch {
                actionedImages = imageLoader!!.getActionedImages()
                when {
                    actionedImages.isNotEmpty() -> {
                        filteredImages.removeAll(actionedImages.values)
                        imageAdapter.init(filteredImages, allImages)
                        imageAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
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
    private fun handleResult(result:Result){
        if(result.status is CallbackStatus.SUCCESS){
            val images = result.images
            if(images.isNotEmpty()) {
                filteredImages = ImageHelper.filterImages(images, bucketId)
                allImages = ArrayList(filteredImages)
                imageAdapter.init(filteredImages, allImages)
                selectorRV?.let {
                    it.visibility = View.VISIBLE
                    lastItemId?.let { pos ->
                        (it.layoutManager as GridLayoutManager)
                            .scrollToPosition(ImageHelper.getIndexFromId(filteredImages, pos))
                    }
                }
            }
            else{
                empty_text?.let {
                    it.visibility = View.VISIBLE
                }
                selectorRV?.let{
                    it.visibility = View.GONE
                }
            }
        }
        loader?.let {
            it.visibility = if (result.status is CallbackStatus.FETCHING) View.VISIBLE else View.GONE
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
        imageAdapter.cleanUP()

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

    override fun refresh() {
        imageAdapter.refresh(filteredImages, allImages)
    }
}