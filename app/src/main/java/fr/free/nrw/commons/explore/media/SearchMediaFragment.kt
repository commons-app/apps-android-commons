package fr.free.nrw.commons.explore.media

import android.os.Bundle
import android.view.View
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.explore.BaseSearchFragment
import fr.free.nrw.commons.explore.SearchActivity
import javax.inject.Inject

/**
 * Displays the image search screen.
 */
class SearchMediaFragment : BaseSearchFragment<Media>(), SearchMediaFragmentContract.View {
    @Inject
    lateinit var presenter: SearchMediaFragmentContract.Presenter

    override val emptyTemplateTextId: Int = R.string.depictions_not_found

    override val errorTextId: Int = R.string.error_loading_images

    override val injectedPresenter: SearchMediaFragmentContract.Presenter
        get() = presenter

    override val pagedListAdapter by lazy {
        SearchImagesAdapter {
            (context as SearchActivity?)!!.onSearchImageClicked(it)
        }
    }

    private val simpleDataObserver = SimpleDataObserver { notifyViewPager() }

    fun requestMoreImages() {
        // This functionality is replaced by a dataSetObserver and by using loadAround
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pagedListAdapter.registerAdapterDataObserver(simpleDataObserver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pagedListAdapter.unregisterAdapterDataObserver(simpleDataObserver)
    }

    private fun notifyViewPager() {
        (activity as SearchActivity).viewPagerNotifyDataSetChanged()
    }

    fun getImageAtPosition(position: Int): Media? =
        pagedListAdapter.currentList?.get(position)?.takeIf { it.filename != null }
            .also { pagedListAdapter.currentList?.loadAround(position) }

    fun getTotalImagesCount(): Int = pagedListAdapter.itemCount
}

