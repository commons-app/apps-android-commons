package fr.free.nrw.commons.explore.depictions

import fr.free.nrw.commons.R
import fr.free.nrw.commons.depictions.WikidataItemDetailsActivity
import fr.free.nrw.commons.explore.BaseSearchFragment
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

    override val errorTextId: Int = R.string.error_loading_depictions

    override val injectedPresenter: SearchDepictionsFragmentContract.Presenter
        get() = presenter

    override val pagedListAdapter by lazy {
        DepictionAdapter { WikidataItemDetailsActivity.startYourself(context, it) }
    }
}
