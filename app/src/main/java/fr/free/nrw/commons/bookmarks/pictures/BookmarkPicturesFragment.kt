package fr.free.nrw.commons.bookmarks.pictures

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListAdapter
import dagger.android.support.DaggerFragment
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.bookmarks.BookmarkListRootFragment
import fr.free.nrw.commons.category.GridViewAdapter
import fr.free.nrw.commons.databinding.FragmentBookmarksPicturesBinding
import fr.free.nrw.commons.utils.NetworkUtils
import fr.free.nrw.commons.utils.ViewUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject


class BookmarkPicturesFragment : DaggerFragment() {

    private var gridAdapter: GridViewAdapter? = null
    private val compositeDisposable = CompositeDisposable()

    private var binding: FragmentBookmarksPicturesBinding? = null

    @Inject
    lateinit var controller: BookmarkPicturesController

    /**
     * Create an instance of the fragment with the right bundle parameters
     * @return an instance of the fragment
     */
    companion object {
        fun newInstance(): BookmarkPicturesFragment {
            return BookmarkPicturesFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBookmarksPicturesBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.bookmarkedPicturesList?.onItemClickListener = parentFragment as? AdapterView.OnItemClickListener
        initList()
    }

    override fun onStop() {
        super.onStop()
        controller.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
        binding = null
    }

    override fun onResume() {
        super.onResume()
        if (controller.needRefreshBookmarkedPictures()) {
            binding?.bookmarkedPicturesList?.visibility = View.GONE
            gridAdapter?.let {
                it.clear()
                (parentFragment as? BookmarkListRootFragment)?.viewPagerNotifyDataSetChanged()
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
        if (!NetworkUtils.isInternetConnectionEstablished(requireContext())) {
            handleNoInternet()
            return
        }

        binding?.apply {
            loadingImagesProgressBar.visibility = View.VISIBLE
            statusMessage.visibility = View.GONE
        }

        compositeDisposable.add(
            controller.loadBookmarkedPictures()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::handleSuccess, ::handleError)
        )
    }

    /**
     * Handles the UI updates for no internet scenario
     */
    private fun handleNoInternet() {
        binding?.apply {
            loadingImagesProgressBar.visibility = View.GONE
            if (gridAdapter == null || gridAdapter?.isEmpty == true) {
                statusMessage.visibility = View.VISIBLE
                statusMessage.text = getString(R.string.no_internet)
            } else {
                ViewUtil.showShortSnackbar(parentLayout, R.string.no_internet)
            }
        }
    }

    /**
     * Logs and handles API error scenario
     * @param throwable
     */
    private fun handleError(throwable: Throwable) {
        Timber.e(throwable, "Error occurred while loading images inside a category")
        try {
            ViewUtil.showShortSnackbar(binding?.root ?: return, R.string.error_loading_images)
            initErrorView()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Handles the UI updates for an error scenario
     */
    private fun initErrorView() {
        binding?.apply {
            loadingImagesProgressBar.visibility = View.GONE
            if (gridAdapter == null || gridAdapter?.isEmpty == true) {
                statusMessage.visibility = View.VISIBLE
                statusMessage.text = getString(R.string.no_images_found)
            } else {
                statusMessage.visibility = View.GONE
            }
        }
    }

    /**
     * Handles the UI updates when there are no bookmarks
     */
    private fun initEmptyBookmarkListView() {
        binding?.apply {
            loadingImagesProgressBar.visibility = View.GONE
            if (gridAdapter == null || gridAdapter?.isEmpty == true) {
                statusMessage.visibility = View.VISIBLE
                statusMessage.text = getString(R.string.bookmark_empty)
            } else {
                statusMessage.visibility = View.GONE
            }
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
            if (gridAdapter?.containsAll(collection) == true) {
                binding?.apply {
                    loadingImagesProgressBar.visibility = View.GONE
                    statusMessage.visibility = View.GONE
                    bookmarkedPicturesList.visibility = View.VISIBLE
                    bookmarkedPicturesList.adapter = gridAdapter
                }
                return
            }
            gridAdapter?.addItems(collection)
            (parentFragment as? BookmarkListRootFragment)?.viewPagerNotifyDataSetChanged()
        }
        binding?.apply {
            loadingImagesProgressBar.visibility = View.GONE
            statusMessage.visibility = View.GONE
            bookmarkedPicturesList.visibility = View.VISIBLE
        }
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
        binding?.bookmarkedPicturesList?.adapter = gridAdapter
    }

    /**
     * It returns an instance of gridView adapter which helps in extracting media details
     * used by the gridView
     * @return GridView Adapter
     */
    fun getAdapter(): ListAdapter? {
        return binding?.bookmarkedPicturesList?.adapter
    }
}
