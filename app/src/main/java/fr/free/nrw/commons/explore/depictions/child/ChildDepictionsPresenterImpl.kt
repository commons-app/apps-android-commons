package fr.free.nrw.commons.explore.depictions.child

import fr.free.nrw.commons.data.models.upload.depictions.DepictedItem
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.explore.paging.BasePagingPresenter
import fr.free.nrw.commons.explore.paging.PagingContract
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named

interface ChildDepictionsPresenter : PagingContract.Presenter<DepictedItem>

class ChildDepictionsPresenterImpl @Inject constructor(
    @Named(CommonsApplicationModule.MAIN_THREAD) mainThreadScheduler: Scheduler,
    dataSourceFactory: PageableChildDepictionsDataSource
) : BasePagingPresenter<DepictedItem>(mainThreadScheduler, dataSourceFactory),
    ChildDepictionsPresenter
