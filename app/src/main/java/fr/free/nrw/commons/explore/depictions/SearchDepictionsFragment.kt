package fr.free.nrw.commons.explore.depictions

import fr.free.nrw.commons.R
import fr.free.nrw.commons.depictions.WikidataItemDetailsActivity
import fr.free.nrw.commons.explore.BaseSearchFragment
import fr.free.nrw.commons.explore.SearchFragmentContract
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import javax.inject.Inject

/**
 * Display depictions in search fragment
 */
class SearchDepictionsFragment : BaseSearchFragment<DepictedItem>(),
    SearchDepictionsFragmentContract.View {
    @Inject
    lateinit var presenter: SearchDepictionsFragmentContract.Presenter

    override val emptyTemplateTextId: Int = R.string.depictions_not_found

    override val injectedPresenter: SearchFragmentContract.Presenter<DepictedItem>
        get() = presenter

    override val pagedListAdapter by lazy {
        DepictionAdapter { WikidataItemDetailsActivity.startYourself(context, it) }
    }
}
