package fr.free.nrw.commons.explore

import dagger.Binds
import dagger.Module
import fr.free.nrw.commons.explore.categories.SearchCategoriesFragmentContract
import fr.free.nrw.commons.explore.depictions.SearchDepictionsFragmentContract
import fr.free.nrw.commons.explore.depictions.SearchDepictionsFragmentPresenter
import fr.free.nrw.commons.explore.media.SearchMediaFragmentContract
import fr.free.nrw.commons.explore.media.SearchMediaFragmentPresenter

/**
 * The Dagger Module for explore:depictions related presenters and (some other objects maybe in future)
 */
@Module
abstract class SearchModule {
    @Binds
    abstract fun SearchDepictionsFragmentPresenter.bindsSearchDepictionsFragmentPresenter()
            : SearchDepictionsFragmentContract.Presenter

    @Binds
    abstract fun SearchCategoriesFragmentPresenter.bindsSearchCategoriesFragmentPresenter()
            : SearchCategoriesFragmentContract.Presenter

    @Binds
    abstract fun SearchMediaFragmentPresenter.bindsSearchMediaFragmentPresenter()
            : SearchMediaFragmentContract.Presenter
}
