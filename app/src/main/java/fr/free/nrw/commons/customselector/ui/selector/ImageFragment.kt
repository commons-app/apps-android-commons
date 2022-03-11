package fr.free.nrw.commons.customselector.ui.selector

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.helper.ImageHelper
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.CallbackStatus
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.model.Result
import fr.free.nrw.commons.customselector.ui.adapter.ImageAdapter
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.theme.BaseActivity
import kotlinx.android.synthetic.main.fragment_custom_selector.*
import kotlinx.android.synthetic.main.fragment_custom_selector.view.*
import java.io.File
import java.io.FileInputStream
import java.net.URI
import javax.inject.Inject

/**
 * Custom Selector Image Fragment.
 */
class ImageFragment: CommonsDaggerSupportFragment() {

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
    lateinit var filteredImages: ArrayList<Image>;

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

        selectorRV = root.selector_rv
        loader = root.loader

        return root
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
}