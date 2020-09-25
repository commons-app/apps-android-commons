package fr.free.nrw.commons.explore.categories.parent

import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.explore.paging.BasePagingPresenter
import fr.free.nrw.commons.explore.paging.PagingContract
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named


interface ParentCategoriesPresenter : PagingContract.Presenter<String>

class ParentCategoriesPresenterImpl @Inject constructor(
    @Named(CommonsApplicationModule.MAIN_THREAD) mainThreadScheduler: Scheduler,
    dataSourceFactory: PageableParentCategoriesDataSource
) : BasePagingPresenter<String>(mainThreadScheduler, dataSourceFactory),
    ParentCategoriesPresenter
