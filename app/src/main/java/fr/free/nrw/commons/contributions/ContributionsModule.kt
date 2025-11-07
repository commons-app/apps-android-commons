package fr.free.nrw.commons.contributions

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

/**
 * The Dagger Module for contributions-related presenters and other dependencies
 */
@Module
@InstallIn(ActivityComponent::class)
abstract class ContributionsModule {

    @Binds
    abstract fun bindsContributionsPresenter(
        presenter: ContributionsPresenter?
    ): ContributionsContract.UserActionListener?
}