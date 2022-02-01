package fr.free.nrw.commons.explore.map

import dagger.Binds
import dagger.Module

/**
 * The Dagger Module for explore:map related presenters and (some other objects maybe in future)
 */
@Module
abstract class ExploreMapModule {
    @Binds
    abstract fun ExploreMapMediaPresenterIml.bindsDepictedImagesContractPresenter()
            : ExploreMapMediaPresenter
}