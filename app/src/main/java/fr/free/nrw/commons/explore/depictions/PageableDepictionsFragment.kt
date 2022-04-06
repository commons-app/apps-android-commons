package fr.free.nrw.commons.explore.depictions

import fr.free.nrw.commons.R
import fr.free.nrw.commons.upload.models.depictions.DepictedItem
import fr.free.nrw.commons.explore.paging.BasePagingFragment


abstract class PageableDepictionsFragment : BasePagingFragment<DepictedItem>() {
    override val errorTextId: Int = R.string.error_loading_depictions
    override val pagedListAdapter by lazy {
        DepictionAdapter { WikidataItemDetailsActivity.startYourself(context, it) }
    }
}
