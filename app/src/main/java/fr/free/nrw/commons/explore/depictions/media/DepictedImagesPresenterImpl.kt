package fr.free.nrw.commons.explore.depictions.media

import fr.free.nrw.commons.models.Media
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.explore.paging.BasePagingPresenter
import fr.free.nrw.commons.explore.paging.PagingContract
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named

interface DepictedImagesPresenter : PagingContract.Presenter<Media>

/**
 * Presenter for DepictedImagesFragment
 */
class DepictedImagesPresenterImpl @Inject constructor(
    @Named(CommonsApplicationModule.MAIN_THREAD) mainThreadScheduler: Scheduler,
    dataSourceFactory: PageableDepictedMediaDataSource
) : BasePagingPresenter<Media>(mainThreadScheduler, dataSourceFactory),
    DepictedImagesPresenter
