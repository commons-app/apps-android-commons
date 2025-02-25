package fr.free.nrw.commons.contributions

import dagger.Binds
import dagger.Module

/**
 * The Dagger Module for contributions-related presenters and other dependencies
 */
@Module
abstract class ContributionsModule {

    @Binds
    abstract fun bindsContributionsPresenter(
        presenter: ContributionsPresenter?
    ): ContributionsContract.UserActionListener?
}