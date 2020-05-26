package fr.free.nrw.commons.explore.media

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

    fun requestMoreImages() {
        // This paradigm is not well suited to pagination
    }

    fun getImageAtPosition(position: Int): Media? =
        pagedListAdapter.currentList?.get(position)?.takeIf { it.filename != null }

    fun getTotalImagesCount(): Int = pagedListAdapter.itemCount
}
