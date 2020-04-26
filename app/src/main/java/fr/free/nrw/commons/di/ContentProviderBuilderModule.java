package fr.free.nrw.commons.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsContentProvider;
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesContentProvider;
import fr.free.nrw.commons.category.CategoryContentProvider;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesContentProvider;

/**
 * This Class Represents the Module for dependency injection (using dagger)
 * so, if a developer needs to add a new ContentProvider to the commons app
 * then that must be mentioned here to inject the dependencies
 */
@Module
@SuppressWarnings({ "WeakerAccess", "unused" })
public abstract class ContentProviderBuilderModule {

	@ContributesAndroidInjector
	abstract CategoryContentProvider bindCategoryContentProvider();

	@ContributesAndroidInjector
	abstract RecentSearchesContentProvider bindRecentSearchesContentProvider();

	@ContributesAndroidInjector
	abstract BookmarkPicturesContentProvider bindBookmarkContentProvider();

	@ContributesAndroidInjector
	abstract BookmarkLocationsContentProvider bindBookmarkLocationContentProvider();
}
