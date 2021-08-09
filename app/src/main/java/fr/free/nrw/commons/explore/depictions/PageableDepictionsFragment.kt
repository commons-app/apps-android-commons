package fr.free.nrw.commons.explore.depictions

import android.util.Log
import fr.free.nrw.commons.R
import fr.free.nrw.commons.explore.paging.BasePagingFragment
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem


abstract class PageableDepictionsFragment : BasePagingFragment<DepictedItem>() {
    override val errorTextId: Int = R.string.error_loading_depictions
    override val pagedListAdapter by lazy {
        Log.d("abcde", "here")
        DepictionAdapter { WikidataItemDetailsActivity.startYourself(context, it) }
    }
}
