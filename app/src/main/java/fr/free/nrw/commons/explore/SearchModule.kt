package fr.free.nrw.commons.explore

import dagger.Binds
import dagger.Module
import fr.free.nrw.commons.explore.categories.SearchCategoriesFragmentContract
import fr.free.nrw.commons.explore.depictions.SearchDepictionsFragmentContract
import fr.free.nrw.commons.explore.depictions.SearchDepictionsFragmentPresenter

/**
 * The Dagger Module for explore:depictions related presenters and (some other objects maybe in future)
 */
@Module
abstract class SearchModule {
    @Binds
    abstract fun bindsSearchDepictionsFragmentPresenter(
        presenter: SearchDepictionsFragmentPresenter
    ): SearchDepictionsFragmentContract.Presenter

    @Binds
    abstract fun bindsSearchCategoriesFragmentPresenter(
        presenter: SearchCategoriesFragmentPresenter?
    ): SearchCategoriesFragmentContract.Presenter?

}
