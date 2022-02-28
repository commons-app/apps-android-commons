package fr.free.nrw.commons.explore.depictions.parent

import fr.free.nrw.commons.data.models.upload.depictions.DepictedItem
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.explore.paging.BasePagingPresenter
import fr.free.nrw.commons.explore.paging.PagingContract
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named

interface ParentDepictionsPresenter : PagingContract.Presenter<DepictedItem>

class ParentDepictionsPresenterImpl @Inject constructor(
    @Named(CommonsApplicationModule.MAIN_THREAD) mainThreadScheduler: Scheduler,
    dataSourceFactory: PageableParentDepictionsDataSource
) : BasePagingPresenter<DepictedItem>(mainThreadScheduler, dataSourceFactory),
    ParentDepictionsPresenter {

}
