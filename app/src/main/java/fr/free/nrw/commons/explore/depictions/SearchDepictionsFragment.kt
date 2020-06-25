package fr.free.nrw.commons.explore.depictions

import fr.free.nrw.commons.R
import fr.free.nrw.commons.depictions.WikidataItemDetailsActivity
import fr.free.nrw.commons.explore.BasePagingFragment
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import javax.inject.Inject

/**
 * Display depictions in search fragment
 */
class SearchDepictionsFragment : BasePagingFragment<DepictedItem>(),
    SearchDepictionsFragmentContract.View {
    @Inject
    lateinit var presenter: SearchDepictionsFragmentContract.Presenter

    override val errorTextId: Int = R.string.error_loading_depictions

    override val injectedPresenter
        get() = presenter

    override val pagedListAdapter by lazy {
        DepictionAdapter { WikidataItemDetailsActivity.startYourself(context, it) }
    }

    override fun getEmptyText(query: String) = getString(R.string.depictions_not_found, query)
}
