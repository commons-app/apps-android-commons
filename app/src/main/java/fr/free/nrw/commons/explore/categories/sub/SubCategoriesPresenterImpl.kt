package fr.free.nrw.commons.explore.categories.sub

import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.explore.paging.BasePagingPresenter
import fr.free.nrw.commons.explore.paging.PagingContract
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named

interface SubCategoriesPresenter : PagingContract.Presenter<String>

class SubCategoriesPresenterImpl @Inject constructor(
    @Named(CommonsApplicationModule.MAIN_THREAD) mainThreadScheduler: Scheduler,
    dataSourceFactory: PageableSubCategoriesDataSource
) : BasePagingPresenter<String>(mainThreadScheduler, dataSourceFactory),
    SubCategoriesPresenter
