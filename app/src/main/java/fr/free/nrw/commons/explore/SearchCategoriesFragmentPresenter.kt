package fr.free.nrw.commons.explore

import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.explore.categories.SearchCategoriesFragmentContract
import fr.free.nrw.commons.explore.categories.PageableCategoriesDataSource
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named

class SearchCategoriesFragmentPresenter @Inject constructor(
    @Named(CommonsApplicationModule.MAIN_THREAD) mainThreadScheduler: Scheduler,
    dataSourceFactory: PageableCategoriesDataSource
) : BasePagingPresenter<String>(mainThreadScheduler, dataSourceFactory),
    SearchCategoriesFragmentContract.Presenter
