package fr.free.nrw.commons.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsContentProvider
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesContentProvider
import fr.free.nrw.commons.category.CategoryContentProvider
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesContentProvider
import fr.free.nrw.commons.recentlanguages.RecentLanguagesContentProvider

/**
 * This Class Represents the Module for dependency injection (using dagger)
 * so, if a developer needs to add a new ContentProvider to the commons app
 * then that must be mentioned here to inject the dependencies
 */
@Module
@Suppress("unused")
abstract class ContentProviderBuilderModule {
    @ContributesAndroidInjector
    abstract fun bindCategoryContentProvider(): CategoryContentProvider

    @ContributesAndroidInjector
    abstract fun bindRecentSearchesContentProvider(): RecentSearchesContentProvider

    @ContributesAndroidInjector
    abstract fun bindBookmarkContentProvider(): BookmarkPicturesContentProvider

    @ContributesAndroidInjector
    abstract fun bindBookmarkItemContentProvider(): BookmarkItemsContentProvider

    @ContributesAndroidInjector
    abstract fun bindRecentLanguagesContentProvider(): RecentLanguagesContentProvider
}
