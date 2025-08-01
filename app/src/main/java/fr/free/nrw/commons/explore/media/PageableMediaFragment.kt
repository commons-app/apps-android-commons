package fr.free.nrw.commons.explore.media

import android.content.Context
import android.os.Bundle
import android.view.View
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.MediaDataExtractor
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryImagesCallback
import fr.free.nrw.commons.explore.paging.BasePagingFragment
import fr.free.nrw.commons.media.MediaDetailProvider
import javax.inject.Inject

abstract class PageableMediaFragment :
    BasePagingFragment<Media>(),
    MediaDetailProvider {
    override val pagedListAdapter by lazy {
        PagedMediaAdapter(categoryImagesCallback::onMediaClicked, mediaDataExtractor)
    }

    override val errorTextId: Int = R.string.error_loading_images

    override fun getEmptyText(query: String) = getString(R.string.no_images_found)

    lateinit var categoryImagesCallback: CategoryImagesCallback

    @Inject
    lateinit var mediaDataExtractor: MediaDataExtractor

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment != null) {
            categoryImagesCallback = (parentFragment as CategoryImagesCallback)
        } else {
            categoryImagesCallback = (activity as CategoryImagesCallback)
        }
    }

    private val simpleDataObserver =
        SimpleDataObserver { categoryImagesCallback.viewPagerNotifyDataSetChanged() }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        pagedListAdapter.registerAdapterDataObserver(simpleDataObserver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pagedListAdapter.unregisterAdapterDataObserver(simpleDataObserver)
    }

    override fun getMediaAtPosition(position: Int): Media? =
        pagedListAdapter.currentList
            ?.get(position)
            ?.takeIf { it.filename != null }
            .also {
                pagedListAdapter.currentList?.loadAround(position)
                binding.paginatedSearchResultsList.scrollToPosition(position)
            }

    override fun getTotalMediaCount(): Int = pagedListAdapter.itemCount

    override fun getContributionStateAt(position: Int) = null

    /**
     * Reload media detail fragment once media is nominated
     *
     * @param index item position that has been nominated
     */
    override fun refreshNominatedMedia(index: Int) {
        activity?.onBackPressed()
        categoryImagesCallback.onMediaClicked(index)
    }
}
