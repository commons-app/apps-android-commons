package fr.free.nrw.commons.explore.categories.media

import fr.free.nrw.commons.data.models.Media
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.explore.paging.BasePagingPresenter
import fr.free.nrw.commons.explore.paging.PagingContract
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named

interface CategoryMediaPresenter : PagingContract.Presenter<Media>

/**
 * Presenter for DepictedImagesFragment
 */
class CategoryMediaPresenterImpl @Inject constructor(
    @Named(CommonsApplicationModule.MAIN_THREAD) mainThreadScheduler: Scheduler,
    dataSourceFactory: PageableCategoriesMediaDataSource
) : BasePagingPresenter<Media>(mainThreadScheduler, dataSourceFactory),
    CategoryMediaPresenter
