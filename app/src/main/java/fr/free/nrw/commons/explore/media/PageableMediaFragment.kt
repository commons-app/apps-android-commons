package fr.free.nrw.commons.explore.media

import android.os.Bundle
import android.view.View
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.explore.BasePagingFragment
import kotlinx.android.synthetic.main.fragment_search_paginated.*


abstract class PageableMediaFragment : BasePagingFragment<Media>() {
    override val pagedListAdapter by lazy { PagedMediaAdapter(::onItemClicked) }

    override val errorTextId: Int = R.string.error_loading_images

    override fun getEmptyText(query: String) = getString(R.string.no_images_found)

    protected abstract fun onItemClicked(position: Int)

    protected abstract fun notifyViewPager()

    private val simpleDataObserver = SimpleDataObserver { notifyViewPager() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pagedListAdapter.registerAdapterDataObserver(simpleDataObserver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pagedListAdapter.unregisterAdapterDataObserver(simpleDataObserver)
    }

    fun getImageAtPosition(position: Int): Media? =
        pagedListAdapter.currentList?.get(position)?.takeIf { it.filename != null }
            .also {
                pagedListAdapter.currentList?.loadAround(position)
                paginatedSearchResultsList.scrollToPosition(position)
            }

    fun getTotalImagesCount(): Int = pagedListAdapter.itemCount
}
