package fr.free.nrw.commons.explore.categories

import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.explore.paging.BasePagingPresenter
import fr.free.nrw.commons.explore.paging.PagingContract
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named

interface SearchCategoriesFragmentPresenter : PagingContract.Presenter<String>

class SearchCategoriesFragmentPresenterImpl @Inject constructor(
    @Named(CommonsApplicationModule.MAIN_THREAD) mainThreadScheduler: Scheduler,
    dataSourceFactory: PageableCategoriesDataSource
) : BasePagingPresenter<String>(mainThreadScheduler, dataSourceFactory),
    SearchCategoriesFragmentPresenter
