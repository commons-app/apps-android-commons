package fr.free.nrw.commons.explore.depictions.search

import fr.free.nrw.commons.data.models.upload.depictions.DepictedItem
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.explore.paging.BasePagingPresenter
import fr.free.nrw.commons.explore.paging.PagingContract
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named

interface SearchDepictionsFragmentPresenter : PagingContract.Presenter<DepictedItem>
/**
 * The presenter class for SearchDepictionsFragment
 */
class SearchDepictionsFragmentPresenterImpl @Inject constructor(
    @Named(CommonsApplicationModule.MAIN_THREAD) mainThreadScheduler: Scheduler,
    dataSourceFactory: PageableDepictionsDataSource
) : BasePagingPresenter<DepictedItem>(mainThreadScheduler, dataSourceFactory),
    SearchDepictionsFragmentPresenter
