package fr.free.nrw.commons.depictions.Media

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.explore.BaseSearchPresenter
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named

/**
 * Presenter for DepictedImagesFragment
 */
class DepictedImagesPresenter @Inject constructor(
    @Named(CommonsApplicationModule.MAIN_THREAD) mainThreadScheduler: Scheduler,
    dataSourceFactory: PageableDepictedMediaDataSource
) : BaseSearchPresenter<Media>(mainThreadScheduler, dataSourceFactory),
    DepictedImagesContract.Presenter
