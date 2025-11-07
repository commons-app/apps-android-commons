package fr.free.nrw.commons.explore.categories

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import fr.free.nrw.commons.explore.categories.media.CategoryMediaPresenter
import fr.free.nrw.commons.explore.categories.media.CategoryMediaPresenterImpl
import fr.free.nrw.commons.explore.categories.parent.ParentCategoriesPresenter
import fr.free.nrw.commons.explore.categories.parent.ParentCategoriesPresenterImpl
import fr.free.nrw.commons.explore.categories.sub.SubCategoriesPresenter
import fr.free.nrw.commons.explore.categories.sub.SubCategoriesPresenterImpl

@Module
@InstallIn(FragmentComponent::class)
abstract class CategoriesModule {
    @Binds
    abstract fun bindsCategoryMediaPresenter(impl: CategoryMediaPresenterImpl): CategoryMediaPresenter

    @Binds
    abstract fun bindsSubCategoriesPresenter(impl: SubCategoriesPresenterImpl): SubCategoriesPresenter

    @Binds
    abstract fun bindsParentCategoriesPresenter(impl: ParentCategoriesPresenterImpl): ParentCategoriesPresenter
}
