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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class BookmarkPicturesFragment : DaggerFragment() {
    private var gridAdapter: GridViewAdapter? = null
    private val compositeDisposable = CompositeDisposable()

    private var binding: FragmentBookmarksPicturesBinding? = null
    private var statusView: BookmarkPicturesStatusView? = null

    @JvmField
    @Inject
    var controller: BookmarkPicturesController? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBookmarksPicturesBinding.inflate(inflater, container, false)
        statusView = BookmarkPicturesStatusView(binding!!)
        return binding!!.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
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
        statusView = null
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
            statusView!!.showNoInternet(gridAdapter?.isEmpty != false)
            return
        }

        binding!!.loadingImagesProgressBar.visibility = View.VISIBLE
        binding!!.statusMessage.visibility = View.GONE

        compositeDisposable.add(
            controller!!
                .loadBookmarkedPictures()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    ::handleSuccess,
                    { statusView?.showError(it, gridAdapter?.isEmpty != false) },
                ),
        )
    }

    /**
     * Handles the success scenario
     * On first load, it initializes the grid view. On subsequent loads, it adds items to the adapter
     * @param collection List of new Media to be displayed
     */
    private fun handleSuccess(collection: List<Media>?) {
        if (collection == null) {
            statusView!!.showErrorView(gridAdapter?.isEmpty != false)
            return
        }
        if (collection.isEmpty()) {
            statusView!!.showEmptyBookmarks(gridAdapter?.isEmpty != false)
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
        gridAdapter =
            GridViewAdapter(
                requireContext(),
                R.layout.layout_category_images,
                mediaList.toMutableList(),
            )
        binding?.let { it.bookmarkedPicturesList.adapter = gridAdapter }
    }

    /**
     * It return an instance of gridView adapter which helps in extracting media details
     * used by the gridView
     * @return GridView Adapter
     */
    fun getAdapter(): ListAdapter? = binding?.bookmarkedPicturesList?.adapter
}
