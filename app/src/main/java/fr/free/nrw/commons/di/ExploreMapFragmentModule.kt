package fr.free.nrw.commons.di

import android.app.Activity
import dagger.Module
import dagger.Provides
import fr.free.nrw.commons.explore.map.ExploreMapFragment

@Module
class ExploreMapFragmentModule{

    @Provides
    fun ExploreMapFragment.providesActivity(): Activity = activity!!
}
