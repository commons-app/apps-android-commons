package fr.free.nrw.commons.customselector.ui.selector

import android.app.Activity
import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Switch
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.database.NotForUploadStatusDao
import fr.free.nrw.commons.customselector.database.UploadedStatusDao
import fr.free.nrw.commons.customselector.helper.ImageHelper
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
import fr.free.nrw.commons.utils.CustomSelectorUtils
import kotlinx.android.synthetic.main.fragment_custom_selector.*
import kotlinx.android.synthetic.main.fragment_custom_selector.view.*
import kotlinx.coroutines.*
import javax.inject.Inject

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
     * Hashmap to store removed images
     */
    private var removedImages: LinkedHashMap<Int,Image> = LinkedHashMap()

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
     * For showing progress dialog
     */
    private var progressDialog: ProgressDialog? = null

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

        return root
    }

    private fun onChangeSwitchState(checked: Boolean) {
        if (checked) {
            switch?.text = getString(R.string.hide_already_actioned_pictures)
            if(removedImages.isNotEmpty()) {
                removedImages.forEach { (key, value) ->
                    filteredImages.add(key, value)
                }
                removedImages.clear()
                imageAdapter.init(filteredImages)
                imageAdapter.notifyDataSetChanged()
            }
        } else {
            switch?.text = getString(R.string.show_already_actioned_pictures)
            val currentRemovedImages: ArrayList<Image> = ArrayList()
            scope.launch {
                showProgressBar()
                filteredImages.forEachIndexed{ index, it ->
                    val imageSHA1 = CustomSelectorUtils.getImageSHA1(
                        it.uri,
                        ioDispatcher,
                        fileUtilsWrapper,
                        activity!!.contentResolver
                    )
                    val modifiedSHA1 = CustomSelectorUtils
                        .generateModifiedSHA1(it,
                            defaultDispatcher,
                            requireContext(),
                            fileProcessor,
                            fileUtilsWrapper
                        )

                    var result = uploadedStatusDao.findByImageSHA1(imageSHA1, true)
                    if(result < 1) {
                        result = uploadedStatusDao.findByModifiedImageSHA1(modifiedSHA1, true)
                    }
                    val exists = notForUploadStatusDao.find(imageSHA1)

                    when {
                        exists >= 1 || result >= 1 -> {
                            removedImages[index] = it
                            currentRemovedImages.add(it)
                        }
                    }
                }
                when {
                    removedImages.isNotEmpty() -> {
                        filteredImages.removeAll(currentRemovedImages)
                        imageAdapter.init(filteredImages)
                        imageAdapter.notifyDataSetChanged()
                    }
                }
                progressDialog!!.dismiss()
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
                imageAdapter.init(filteredImages)
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
        imageLoader?.cleanUP()

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

    /**
     * Show progress bar
     */
    private fun showProgressBar() {
        progressDialog = ProgressDialog(requireContext())
        progressDialog!!.isIndeterminate = true
        progressDialog!!.setTitle(getString(R.string.hiding_already_actioned_pictures))
        progressDialog!!.setMessage(getString(R.string.please_wait))
        progressDialog!!.setCanceledOnTouchOutside(false)
        progressDialog!!.show()
    }

    override fun refresh() {
        imageAdapter.refresh(filteredImages)
    }
}