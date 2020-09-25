package fr.free.nrw.commons.explore.media

import javax.inject.Inject

/**
 * Displays the image search screen.
 */
class SearchMediaFragment : PageableMediaFragment() {
    @Inject
    lateinit var presenter: SearchMediaFragmentPresenter

    override val injectedPresenter
        get() = presenter
}

