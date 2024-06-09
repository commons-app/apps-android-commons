package fr.free.nrw.commons.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.free.nrw.commons.bookmarks.BookmarkFragment;
import fr.free.nrw.commons.bookmarks.BookmarkListRootFragment;
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsFragment;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsFragment;
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesFragment;
import fr.free.nrw.commons.contributions.ContributionsFragment;
import fr.free.nrw.commons.contributions.ContributionsListFragment;
import fr.free.nrw.commons.customselector.ui.selector.FolderFragment;
import fr.free.nrw.commons.customselector.ui.selector.ImageFragment;
import fr.free.nrw.commons.explore.ExploreFragment;
import fr.free.nrw.commons.explore.ExploreListRootFragment;
import fr.free.nrw.commons.explore.ExploreMapRootFragment;
import fr.free.nrw.commons.explore.map.ExploreMapFragment;
import fr.free.nrw.commons.explore.categories.media.CategoriesMediaFragment;
import fr.free.nrw.commons.explore.categories.parent.ParentCategoriesFragment;
import fr.free.nrw.commons.explore.categories.search.SearchCategoryFragment;
import fr.free.nrw.commons.explore.categories.sub.SubCategoriesFragment;
import fr.free.nrw.commons.explore.depictions.child.ChildDepictionsFragment;
import fr.free.nrw.commons.explore.depictions.media.DepictedImagesFragment;
import fr.free.nrw.commons.explore.depictions.parent.ParentDepictionsFragment;
import fr.free.nrw.commons.explore.depictions.search.SearchDepictionsFragment;
import fr.free.nrw.commons.explore.media.SearchMediaFragment;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesFragment;
import fr.free.nrw.commons.media.MediaDetailFragment;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.navtab.MoreBottomSheetFragment;
import fr.free.nrw.commons.navtab.MoreBottomSheetLoggedOutFragment;
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment;
import fr.free.nrw.commons.profile.achievements.AchievementsFragment;
import fr.free.nrw.commons.profile.leaderboard.LeaderboardFragment;
import fr.free.nrw.commons.review.ReviewImageFragment;
import fr.free.nrw.commons.settings.SettingsFragment;
import fr.free.nrw.commons.upload.FailedUploadsFragment;
import fr.free.nrw.commons.upload.PendingUploadsFragment;
import fr.free.nrw.commons.upload.categories.UploadCategoriesFragment;
import fr.free.nrw.commons.upload.depicts.DepictsFragment;
import fr.free.nrw.commons.upload.license.MediaLicenseFragment;
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment;

/**
 * This Class Represents the Module for dependency injection (using dagger)
 * so, if a developer needs to add a new Fragment to the commons app
 * then that must be mentioned here to inject the dependencies
 */
@Module
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class FragmentBuilderModule {

    @ContributesAndroidInjector
    abstract ContributionsListFragment bindContributionsListFragment();

    @ContributesAndroidInjector
    abstract MediaDetailFragment bindMediaDetailFragment();

    @ContributesAndroidInjector
    abstract FolderFragment bindFolderFragment();

    @ContributesAndroidInjector
    abstract ImageFragment bindImageFragment();

    @ContributesAndroidInjector
    abstract MediaDetailPagerFragment bindMediaDetailPagerFragment();

    @ContributesAndroidInjector
    abstract SettingsFragment bindSettingsFragment();

    @ContributesAndroidInjector
    abstract DepictedImagesFragment bindDepictedImagesFragment();

    @ContributesAndroidInjector
    abstract SearchMediaFragment bindBrowseImagesListFragment();

    @ContributesAndroidInjector
    abstract SearchCategoryFragment bindSearchCategoryListFragment();

    @ContributesAndroidInjector
    abstract SearchDepictionsFragment bindSearchDepictionListFragment();

    @ContributesAndroidInjector
    abstract RecentSearchesFragment bindRecentSearchesFragment();

    @ContributesAndroidInjector
    abstract ContributionsFragment bindContributionsFragment();

    @ContributesAndroidInjector(modules = NearbyParentFragmentModule.class)
    abstract NearbyParentFragment bindNearbyParentFragment();

    @ContributesAndroidInjector
    abstract BookmarkPicturesFragment bindBookmarkPictureListFragment();

    @ContributesAndroidInjector(modules = BookmarkLocationsFragmentModule.class)
    abstract BookmarkLocationsFragment bindBookmarkLocationListFragment();

    @ContributesAndroidInjector(modules = BookmarkItemsFragmentModule.class)
    abstract BookmarkItemsFragment bindBookmarkItemListFragment();

    @ContributesAndroidInjector
    abstract ReviewImageFragment bindReviewOutOfContextFragment();

    @ContributesAndroidInjector
    abstract UploadMediaDetailFragment bindUploadMediaDetailFragment();

    @ContributesAndroidInjector
    abstract UploadCategoriesFragment bindUploadCategoriesFragment();

    @ContributesAndroidInjector
    abstract DepictsFragment bindDepictsFragment();

    @ContributesAndroidInjector
    abstract MediaLicenseFragment bindMediaLicenseFragment();

    @ContributesAndroidInjector
    abstract ParentDepictionsFragment bindParentDepictionsFragment();

    @ContributesAndroidInjector
    abstract ChildDepictionsFragment bindChildDepictionsFragment();

    @ContributesAndroidInjector
    abstract CategoriesMediaFragment bindCategoriesMediaFragment();

    @ContributesAndroidInjector
    abstract SubCategoriesFragment bindSubCategoriesFragment();

    @ContributesAndroidInjector
    abstract ParentCategoriesFragment bindParentCategoriesFragment();

    @ContributesAndroidInjector
    abstract ExploreFragment bindExploreFragmentFragment();

    @ContributesAndroidInjector
    abstract ExploreListRootFragment bindExploreFeaturedRootFragment();

    @ContributesAndroidInjector(modules = ExploreMapFragmentModule.class)
    abstract ExploreMapFragment bindExploreNearbyUploadsFragment();

    @ContributesAndroidInjector
    abstract ExploreMapRootFragment bindExploreNearbyUploadsRootFragment();

    @ContributesAndroidInjector
    abstract BookmarkListRootFragment bindBookmarkListRootFragment();

    @ContributesAndroidInjector
    abstract BookmarkFragment bindBookmarkFragmentFragment();

    @ContributesAndroidInjector
    abstract MoreBottomSheetFragment bindMoreBottomSheetFragment();

    @ContributesAndroidInjector
    abstract MoreBottomSheetLoggedOutFragment bindMoreBottomSheetLoggedOutFragment();

    @ContributesAndroidInjector
    abstract AchievementsFragment bindAchievementsFragment();

    @ContributesAndroidInjector
    abstract LeaderboardFragment bindLeaderboardFragment();

    @ContributesAndroidInjector
    abstract PendingUploadsFragment bindPendingUploadsFragment();

    @ContributesAndroidInjector
    abstract FailedUploadsFragment bindFailedUploadsFragment();
}
