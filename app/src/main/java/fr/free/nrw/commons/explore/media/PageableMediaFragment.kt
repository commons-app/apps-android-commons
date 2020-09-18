package fr.free.nrw.commons.explore.media

import android.content.Context
import android.os.Bundle
import android.view.View
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryImagesCallback
import fr.free.nrw.commons.explore.paging.BasePagingFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment.MediaDetailProvider
import kotlinx.android.synthetic.main.fragment_search_paginated.*


abstract class PageableMediaFragment : BasePagingFragment<Media>(), MediaDetailProvider {

    override val pagedListAdapter by lazy {
        PagedMediaAdapter(categoryImagesCallback::onMediaClicked)
    }

    override val errorTextId: Int = R.string.error_loading_images

    override fun getEmptyText(query: String) = getString(R.string.no_images_found)

    lateinit var categoryImagesCallback: CategoryImagesCallback

    override fun onAttach(context: Context) {
        super.onAttach(context)
        categoryImagesCallback = (parentFragment as CategoryImagesCallback)
    }

    private val simpleDataObserver =
        SimpleDataObserver { categoryImagesCallback.viewPagerNotifyDataSetChanged() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pagedListAdapter.registerAdapterDataObserver(simpleDataObserver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pagedListAdapter.unregisterAdapterDataObserver(simpleDataObserver)
    }

    override fun getMediaAtPosition(position: Int): Media? =
        pagedListAdapter.currentList?.get(position)?.takeIf { it.filename != null }
            .also {
                pagedListAdapter.currentList?.loadAround(position)
                paginatedSearchResultsList.scrollToPosition(position)
            }

    override fun getTotalMediaCount(): Int = pagedListAdapter.itemCount

    override fun getContributionStateAt(position: Int) = null
}
