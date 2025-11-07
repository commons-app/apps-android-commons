package fr.free.nrw.commons.explore.depictions

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import fr.free.nrw.commons.explore.depictions.child.ChildDepictionsPresenter
import fr.free.nrw.commons.explore.depictions.child.ChildDepictionsPresenterImpl
import fr.free.nrw.commons.explore.depictions.media.DepictedImagesPresenter
import fr.free.nrw.commons.explore.depictions.media.DepictedImagesPresenterImpl
import fr.free.nrw.commons.explore.depictions.parent.ParentDepictionsPresenter
import fr.free.nrw.commons.explore.depictions.parent.ParentDepictionsPresenterImpl

/**
 * The Dagger Module for explore:depictions related presenters and (some other objects maybe in future)
 */
@Module
@InstallIn(FragmentComponent::class)
abstract class DepictionModule {
    @Binds
    abstract fun bindsParentDepictionPresenter(impl: ParentDepictionsPresenterImpl): ParentDepictionsPresenter

    @Binds
    abstract fun bindsChildDepictionPresenter(impl: ChildDepictionsPresenterImpl): ChildDepictionsPresenter

    @Binds
    abstract fun bindsDepictedImagesContractPresenter(impl: DepictedImagesPresenterImpl): DepictedImagesPresenter
}
