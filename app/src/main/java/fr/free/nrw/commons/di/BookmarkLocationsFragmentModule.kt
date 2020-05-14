package fr.free.nrw.commons.di

import android.app.Activity
import dagger.Module
import dagger.Provides
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsFragment

@Module
class BookmarkLocationsFragmentModule {
    @Provides
    fun BookmarkLocationsFragment.providesActivity(): Activity = activity!!
}
