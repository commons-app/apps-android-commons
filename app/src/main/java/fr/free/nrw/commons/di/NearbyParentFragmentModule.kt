package fr.free.nrw.commons.di

import android.app.Activity
import dagger.Module
import dagger.Provides
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment

@Module
class NearbyParentFragmentModule{

    @Provides
    fun NearbyParentFragment.providesActivity(): Activity = activity!!
}
