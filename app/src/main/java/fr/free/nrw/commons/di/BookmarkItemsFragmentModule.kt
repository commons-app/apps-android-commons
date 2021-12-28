package fr.free.nrw.commons.di

import android.app.Activity
import dagger.Module
import dagger.Provides
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsFragment

@Module
class BookmarkItemsFragmentModule {
    @Provides
    fun BookmarkItemsFragment.providesActivity(): Activity = activity!!
}
