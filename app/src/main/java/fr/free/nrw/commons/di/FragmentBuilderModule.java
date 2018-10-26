package fr.free.nrw.commons.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsFragment;
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesFragment;
import fr.free.nrw.commons.category.CategorizationFragment;
import fr.free.nrw.commons.category.CategoryImagesListFragment;
import fr.free.nrw.commons.category.SubCategoryListFragment;
import fr.free.nrw.commons.contributions.ContributionsListFragment;
import fr.free.nrw.commons.explore.categories.SearchCategoryFragment;
import fr.free.nrw.commons.explore.images.SearchImageFragment;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesFragment;
import fr.free.nrw.commons.media.MediaDetailFragment;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.nearby.NearbyListFragment;
import fr.free.nrw.commons.nearby.NearbyMapFragment;
import fr.free.nrw.commons.nearby.NoPermissionsFragment;
import fr.free.nrw.commons.settings.SettingsFragment;
import fr.free.nrw.commons.upload.MultipleUploadListFragment;
import fr.free.nrw.commons.upload.SingleUploadFragment;

@Module
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class FragmentBuilderModule {

    @ContributesAndroidInjector
    abstract CategorizationFragment bindCategorizationFragment();

    @ContributesAndroidInjector
    abstract ContributionsListFragment bindContributionsListFragment();

    @ContributesAndroidInjector
    abstract MediaDetailFragment bindMediaDetailFragment();

    @ContributesAndroidInjector
    abstract MediaDetailPagerFragment bindMediaDetailPagerFragment();

    @ContributesAndroidInjector
    abstract NearbyListFragment bindNearbyListFragment();

    @ContributesAndroidInjector
    abstract NearbyMapFragment bindNearbyMapFragment();

    @ContributesAndroidInjector
    abstract NoPermissionsFragment bindNoPermissionsFragment();

    @ContributesAndroidInjector
    abstract SettingsFragment bindSettingsFragment();

    @ContributesAndroidInjector
    abstract MultipleUploadListFragment bindMultipleUploadListFragment();

    @ContributesAndroidInjector
    abstract SingleUploadFragment bindSingleUploadFragment();

    @ContributesAndroidInjector
    abstract CategoryImagesListFragment bindFeaturedImagesListFragment();

    @ContributesAndroidInjector
    abstract SubCategoryListFragment bindSubCategoryListFragment();

    @ContributesAndroidInjector
    abstract SearchImageFragment bindBrowseImagesListFragment();

    @ContributesAndroidInjector
    abstract SearchCategoryFragment bindSearchCategoryListFragment();

    @ContributesAndroidInjector
    abstract RecentSearchesFragment bindRecentSearchesFragment();

    @ContributesAndroidInjector
    abstract BookmarkPicturesFragment bindBookmarkPictureListFragment();

    @ContributesAndroidInjector
    abstract BookmarkLocationsFragment bindBookmarkLocationListFragment();

}
