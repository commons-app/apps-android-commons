package fr.free.nrw.commons.bookmarks.pictures

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListAdapter
import dagger.android.support.DaggerFragment
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.bookmarks.BookmarkListRootFragment
import fr.free.nrw.commons.category.GridViewAdapter
import fr.free.nrw.commons.databinding.FragmentBookmarksPicturesBinding
import fr.free.nrw.commons.utils.NetworkUtils.isInternetConnectionEstablished
import fr.free.nrw.commons.utils.ViewUtil.showShortSnackbar
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class BookmarkPicturesFragment : DaggerFragment() {
    private var gridAdapter: GridViewAdapter? = null
    private val compositeDisposable = CompositeDisposable()

    private var binding: FragmentBookmarksPicturesBinding? = null

    @JvmField
    @Inject
    var controller: BookmarkPicturesController? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBookmarksPicturesBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.bookmarkedPicturesList.onItemClickListener =
            parentFragment as OnItemClickListener?
        initList()
    }

    override fun onStop() {
        super.onStop()
        controller!!.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
        binding = null
    }

    override fun onResume() {
        super.onResume()
        if (controller!!.needRefreshBookmarkedPictures()) {
            binding!!.bookmarkedPicturesList.visibility = View.GONE
            gridAdapter?.let {
                it.clear()
                (parentFragment as BookmarkListRootFragment).viewPagerNotifyDataSetChanged()
            }
            initList()
        }
    }

    /**
     * Checks for internet connection and then initializes
     * the recycler view with bookmarked pictures
     */
    @SuppressLint("CheckResult")
    private fun initList() {
        if (!isInternetConnectionEstablished(context)) {
            handleNoInternet()
            return
        }

        binding!!.loadingImagesProgressBar.visibility = View.VISIBLE
        binding!!.statusMessage.visibility = View.GONE

        compositeDisposable.add(
            controller!!.loadBookmarkedPictures()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::handleSuccess, ::handleError)
        )
    }

    /**
     * Handles the UI updates for no internet scenario
     */
    private fun handleNoInternet() {
        binding!!.loadingImagesProgressBar.visibility = View.GONE
        if (gridAdapter == null || gridAdapter!!.isEmpty) {
            binding!!.statusMessage.visibility = View.VISIBLE
            binding!!.statusMessage.text = getString(R.string.no_internet)
        } else {
            showShortSnackbar(binding!!.parentLayout, R.string.no_internet)
        }
    }

    /**
     * Logs and handles API error scenario
     * @param throwable
     */
    private fun handleError(throwable: Throwable) {
        Timber.e(throwable, "Error occurred while loading images inside a category")
        try {
            showShortSnackbar(binding!!.root, R.string.error_loading_images)
            initErrorView()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    /**
     * Handles the UI updates for a error scenario
     */
    private fun initErrorView() {
        binding!!.loadingImagesProgressBar.visibility = View.GONE
        if (gridAdapter == null || gridAdapter!!.isEmpty) {
            binding!!.statusMessage.visibility = View.VISIBLE
            binding!!.statusMessage.text = getString(R.string.no_images_found)
        } else {
            binding!!.statusMessage.visibility = View.GONE
        }
    }

    /**
     * Handles the UI updates when there is no bookmarks
     */
    private fun initEmptyBookmarkListView() {
        binding!!.loadingImagesProgressBar.visibility = View.GONE
        if (gridAdapter == null || gridAdapter!!.isEmpty) {
            binding!!.statusMessage.visibility = View.VISIBLE
            binding!!.statusMessage.text = getString(R.string.bookmark_empty)
        } else {
            binding!!.statusMessage.visibility = View.GONE
        }
    }

    /**
     * Handles the success scenario
     * On first load, it initializes the grid view. On subsequent loads, it adds items to the adapter
     * @param collection List of new Media to be displayed
     */
    private fun handleSuccess(collection: List<Media>?) {
        if (collection == null) {
            initErrorView()
            return
        }
        if (collection.isEmpty()) {
            initEmptyBookmarkListView()
            return
        }

        if (gridAdapter == null) {
            setAdapter(collection)
        } else {
            if (gridAdapter!!.containsAll(collection)) {
                binding!!.loadingImagesProgressBar.visibility = View.GONE
                binding!!.statusMessage.visibility = View.GONE
                binding!!.bookmarkedPicturesList.visibility = View.VISIBLE
                binding!!.bookmarkedPicturesList.adapter = gridAdapter
                return
            }
            gridAdapter!!.addItems(collection)
            (parentFragment as BookmarkListRootFragment).viewPagerNotifyDataSetChanged()
        }
        binding!!.loadingImagesProgressBar.visibility = View.GONE
        binding!!.statusMessage.visibility = View.GONE
        binding!!.bookmarkedPicturesList.visibility = View.VISIBLE
    }

    /**
     * Initializes the adapter with a list of Media objects
     * @param mediaList List of new Media to be displayed
     */
    private fun setAdapter(mediaList: List<Media>) {
        gridAdapter = GridViewAdapter(
            requireContext(),
            R.layout.layout_category_images,
            mediaList.toMutableList()
        )
        binding?.let {  it.bookmarkedPicturesList.adapter = gridAdapter }
    }

    /**
     * It return an instance of gridView adapter which helps in extracting media details
     * used by the gridView
     * @return  GridView Adapter
     */
    fun getAdapter(): ListAdapter? = binding?.bookmarkedPicturesList?.adapter
}
