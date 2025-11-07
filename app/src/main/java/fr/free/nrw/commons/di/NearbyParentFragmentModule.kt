package fr.free.nrw.commons.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent

/**
 * NOTE: This module is deprecated. Hilt automatically provides Activity in FragmentComponent.
 * This module is kept empty to avoid breaking the build, but can be removed entirely.
 */
@Module
@InstallIn(FragmentComponent::class)
class NearbyParentFragmentModule {
    // Hilt automatically provides Activity in FragmentComponent
    // No custom provider needed
}
