package fr.free.nrw.commons.explore.map

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.explore.paging.BasePagingMapPresenter
import fr.free.nrw.commons.explore.paging.PagingContract
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named

interface ExploreMapMediaPresenter : PagingContract.Presenter<Media>

/**
 * Presenter for ExploreMapFragment
 */
class ExploreMapMediaPresenterIml @Inject constructor(
    @Named(CommonsApplicationModule.MAIN_THREAD) mainThreadScheduler: Scheduler,
    dataSourceFactory: ExploreMapMediaDataSource
) : BasePagingMapPresenter<Media>(mainThreadScheduler, dataSourceFactory),
    ExploreMapMediaPresenter