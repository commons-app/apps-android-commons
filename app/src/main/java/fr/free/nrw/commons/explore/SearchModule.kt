package fr.free.nrw.commons.explore

import dagger.Binds
import dagger.Module
import fr.free.nrw.commons.explore.categories.SearchCategoriesFragmentPresenter
import fr.free.nrw.commons.explore.categories.SearchCategoriesFragmentPresenterImpl
import fr.free.nrw.commons.explore.depictions.search.SearchDepictionsFragmentPresenter
import fr.free.nrw.commons.explore.depictions.search.SearchDepictionsFragmentPresenterImpl
import fr.free.nrw.commons.explore.media.SearchMediaFragmentPresenter
import fr.free.nrw.commons.explore.media.SearchMediaFragmentPresenterImpl

/**
 * The Dagger Module for explore:depictions related presenters and (some other objects maybe in future)
 */
@Module
abstract class SearchModule {
    @Binds
    abstract fun SearchDepictionsFragmentPresenterImpl.bindsSearchDepictionsFragmentPresenter()
            : SearchDepictionsFragmentPresenter

    @Binds
    abstract fun SearchCategoriesFragmentPresenterImpl.bindsSearchCategoriesFragmentPresenter()
            : SearchCategoriesFragmentPresenter

    @Binds
    abstract fun SearchMediaFragmentPresenterImpl.bindsSearchMediaFragmentPresenter()
            : SearchMediaFragmentPresenter
}
