package fr.free.nrw.commons.explore.depictions

import dagger.Binds
import dagger.Module
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
abstract class DepictionModule {

    @Binds
    abstract fun ParentDepictionsPresenterImpl.bindsParentDepictionPresenter()
            : ParentDepictionsPresenter

    @Binds
    abstract fun ChildDepictionsPresenterImpl.bindsChildDepictionPresenter()
            : ChildDepictionsPresenter

    @Binds
    abstract fun DepictedImagesPresenterImpl.bindsDepictedImagesContractPresenter()
            : DepictedImagesPresenter
}
