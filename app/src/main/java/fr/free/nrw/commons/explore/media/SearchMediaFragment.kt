package fr.free.nrw.commons.explore.media

import fr.free.nrw.commons.category.CategoryImagesCallback
import fr.free.nrw.commons.explore.SearchActivity
import javax.inject.Inject

/**
 * Displays the image search screen.
 */
class SearchMediaFragment : PageableMediaFragment(){
    @Inject
    lateinit var presenter: SearchMediaFragmentPresenter

    override val injectedPresenter
        get() = presenter

    override fun onItemClicked(position: Int) {
        (context as SearchActivity).onSearchImageClicked(position)
    }

    override fun notifyViewPager() {
        (activity as CategoryImagesCallback).viewPagerNotifyDataSetChanged()
    }

}

