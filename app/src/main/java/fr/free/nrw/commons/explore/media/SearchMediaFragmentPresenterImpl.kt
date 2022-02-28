package fr.free.nrw.commons.explore.media

import fr.free.nrw.commons.data.models.Media
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.explore.paging.PagingContract
import fr.free.nrw.commons.explore.paging.BasePagingPresenter
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named

interface SearchMediaFragmentPresenter : PagingContract.Presenter<Media>

class SearchMediaFragmentPresenterImpl @Inject constructor(
    @Named(CommonsApplicationModule.MAIN_THREAD) mainThreadScheduler: Scheduler,
    dataSourceFactory: PageableMediaDataSource
) : BasePagingPresenter<Media>(mainThreadScheduler, dataSourceFactory),
    SearchMediaFragmentPresenter
