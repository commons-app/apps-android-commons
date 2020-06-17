package fr.free.nrw.commons.explore.media

import fr.free.nrw.commons.category.CategoryImagesCallback
import fr.free.nrw.commons.explore.SearchActivity
import javax.inject.Inject

/**
 * Displays the image search screen.
 */
class SearchMediaFragment : PageableMediaFragment(), SearchMediaFragmentContract.View {
    @Inject
    lateinit var presenter: SearchMediaFragmentContract.Presenter

    override val injectedPresenter: SearchMediaFragmentContract.Presenter
        get() = presenter

    override fun onItemClicked(position: Int) {
        (context as SearchActivity?)!!.onSearchImageClicked(position)
    }

    override fun notifyViewPager() {
        (activity as CategoryImagesCallback).viewPagerNotifyDataSetChanged()
    }

}

