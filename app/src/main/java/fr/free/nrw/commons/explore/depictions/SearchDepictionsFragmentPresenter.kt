package fr.free.nrw.commons.explore.depictions

import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.explore.BaseSearchPresenter
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named

/**
 * The presenter class for SearchDepictionsFragment
 */
class SearchDepictionsFragmentPresenter @Inject constructor(
    @Named(CommonsApplicationModule.MAIN_THREAD) mainThreadScheduler: Scheduler,
    dataSourceFactory: PageableDepictionsDataSource
) : BaseSearchPresenter<DepictedItem>(mainThreadScheduler, dataSourceFactory),
    SearchDepictionsFragmentContract.Presenter
