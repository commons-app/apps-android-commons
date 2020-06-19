package fr.free.nrw.commons.explore.categories

import dagger.Binds
import dagger.Module
import fr.free.nrw.commons.explore.categories.media.CategoryMediaPresenter
import fr.free.nrw.commons.explore.categories.media.CategoryMediaPresenterImpl
import fr.free.nrw.commons.explore.categories.parent.ParentCategoriesPresenter
import fr.free.nrw.commons.explore.categories.parent.ParentCategoriesPresenterImpl
import fr.free.nrw.commons.explore.categories.sub.SubCategoriesPresenter
import fr.free.nrw.commons.explore.categories.sub.SubCategoriesPresenterImpl


@Module
abstract class CategoriesModule {

    @Binds
    abstract fun CategoryMediaPresenterImpl.bindsCategoryMediaPresenter()
            : CategoryMediaPresenter

    @Binds
    abstract fun SubCategoriesPresenterImpl.bindsSubCategoriesPresenter()
            : SubCategoriesPresenter

    @Binds
    abstract fun ParentCategoriesPresenterImpl.bindsParentCategoriesPresenter()
            : ParentCategoriesPresenter
}
