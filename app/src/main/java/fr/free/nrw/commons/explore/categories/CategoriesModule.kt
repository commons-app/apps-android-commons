package fr.free.nrw.commons.explore.categories

import dagger.Binds
import dagger.Module
import fr.free.nrw.commons.explore.categories.media.CategoryMediaPresenter
import fr.free.nrw.commons.explore.categories.media.CategoryMediaPresenterImpl


@Module
abstract class CategoriesModule {

    @Binds
    abstract fun CategoryMediaPresenterImpl.bindsParentDepictionPresenter()
            : CategoryMediaPresenter
}
