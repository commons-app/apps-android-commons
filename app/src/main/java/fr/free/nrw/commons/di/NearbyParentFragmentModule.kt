package fr.free.nrw.commons.di

import android.app.Activity
import dagger.Module
import dagger.Provides
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import javax.inject.Named

@Module
class NearbyParentFragmentModule {
    @Provides
    fun NearbyParentFragment.providesActivity(): Activity = activity!!
    @Provides
    fun providesApplicationKvStore(
        @Named("default_preferences") kvStore: JsonKvStore
    ): JsonKvStore {
        return kvStore
    }
}
