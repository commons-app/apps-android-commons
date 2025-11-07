package fr.free.nrw.commons.explore

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import fr.free.nrw.commons.explore.categories.search.SearchCategoriesFragmentPresenter
import fr.free.nrw.commons.explore.categories.search.SearchCategoriesFragmentPresenterImpl
import fr.free.nrw.commons.explore.depictions.search.SearchDepictionsFragmentPresenter
import fr.free.nrw.commons.explore.depictions.search.SearchDepictionsFragmentPresenterImpl
import fr.free.nrw.commons.explore.media.SearchMediaFragmentPresenter
import fr.free.nrw.commons.explore.media.SearchMediaFragmentPresenterImpl

/**
 * The Dagger Module for explore:depictions related presenters and (some other objects maybe in future)
 */
@Module
@InstallIn(FragmentComponent::class)
abstract class SearchModule {
    @Binds
    abstract fun bindsSearchDepictionsFragmentPresenter(impl: SearchDepictionsFragmentPresenterImpl): SearchDepictionsFragmentPresenter

    @Binds
    abstract fun bindsSearchCategoriesFragmentPresenter(impl: SearchCategoriesFragmentPresenterImpl): SearchCategoriesFragmentPresenter

    @Binds
    abstract fun bindsSearchMediaFragmentPresenter(impl: SearchMediaFragmentPresenterImpl): SearchMediaFragmentPresenter
}
