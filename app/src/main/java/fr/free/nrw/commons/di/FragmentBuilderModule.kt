package fr.free.nrw.commons.di

import fr.free.nrw.commons.bookmarks.BookmarkFragment
import fr.free.nrw.commons.bookmarks.BookmarkListRootFragment
import fr.free.nrw.commons.bookmarks.category.BookmarkCategoriesFragment
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsFragment
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsFragment
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesFragment
import fr.free.nrw.commons.contributions.ContributionsFragment
import fr.free.nrw.commons.contributions.ContributionsListFragment
import fr.free.nrw.commons.customselector.ui.selector.FolderFragment
import fr.free.nrw.commons.customselector.ui.selector.ImageFragment
import fr.free.nrw.commons.explore.ExploreFragment
import fr.free.nrw.commons.explore.ExploreListRootFragment
import fr.free.nrw.commons.explore.ExploreMapRootFragment
import fr.free.nrw.commons.explore.categories.media.CategoriesMediaFragment
import fr.free.nrw.commons.explore.categories.parent.ParentCategoriesFragment
import fr.free.nrw.commons.explore.categories.search.SearchCategoryFragment
import fr.free.nrw.commons.explore.categories.sub.SubCategoriesFragment
import fr.free.nrw.commons.explore.depictions.child.ChildDepictionsFragment
import fr.free.nrw.commons.explore.depictions.media.DepictedImagesFragment
import fr.free.nrw.commons.explore.depictions.parent.ParentDepictionsFragment
import fr.free.nrw.commons.explore.depictions.search.SearchDepictionsFragment
import fr.free.nrw.commons.explore.map.ExploreMapFragment
import fr.free.nrw.commons.explore.media.SearchMediaFragment
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesFragment
import fr.free.nrw.commons.media.MediaDetailFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.navtab.MoreBottomSheetFragment
import fr.free.nrw.commons.navtab.MoreBottomSheetLoggedOutFragment
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import fr.free.nrw.commons.profile.achievements.AchievementsFragment
import fr.free.nrw.commons.profile.leaderboard.LeaderboardFragment
import fr.free.nrw.commons.review.ReviewImageFragment
import fr.free.nrw.commons.settings.SettingsFragment
import fr.free.nrw.commons.upload.FailedUploadsFragment
import fr.free.nrw.commons.upload.PendingUploadsFragment
import fr.free.nrw.commons.upload.categories.UploadCategoriesFragment
import fr.free.nrw.commons.upload.depicts.DepictsFragment
import fr.free.nrw.commons.upload.license.MediaLicenseFragment
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment

/**
 * This Class Represents the Module for dependency injection (using dagger)
 * so, if a developer needs to add a new Fragment to the commons app
 * then that must be mentioned here to inject the dependencies
 *
 * NOTE: This module is DEPRECATED with Hilt. Fragments should use @AndroidEntryPoint instead.
 * This file is kept for reference but all functionality has been migrated to Hilt.
 * The @Module annotation has been removed to prevent Hilt build errors.
 */
@Suppress("unused")
abstract class FragmentBuilderModule {
    // All methods below are deprecated and non-functional
    // Fragments should use @AndroidEntryPoint annotation instead

    /*
    @ContributesAndroidInjector
    abstract fun bindContributionsListFragment(): ContributionsListFragment

    @ContributesAndroidInjector
    abstract fun bindMediaDetailFragment(): MediaDetailFragment

    @ContributesAndroidInjector
    abstract fun bindFolderFragment(): FolderFragment

    @ContributesAndroidInjector
    abstract fun bindImageFragment(): ImageFragment

    @ContributesAndroidInjector
    abstract fun bindMediaDetailPagerFragment(): MediaDetailPagerFragment

    @ContributesAndroidInjector
    abstract fun bindSettingsFragment(): SettingsFragment

    @ContributesAndroidInjector
    abstract fun bindDepictedImagesFragment(): DepictedImagesFragment

    @ContributesAndroidInjector
    abstract fun bindBrowseImagesListFragment(): SearchMediaFragment

    @ContributesAndroidInjector
    abstract fun bindSearchCategoryListFragment(): SearchCategoryFragment

    @ContributesAndroidInjector
    abstract fun bindSearchDepictionListFragment(): SearchDepictionsFragment

    @ContributesAndroidInjector
    abstract fun bindRecentSearchesFragment(): RecentSearchesFragment

    @ContributesAndroidInjector
    abstract fun bindContributionsFragment(): ContributionsFragment

    @ContributesAndroidInjector(modules = [NearbyParentFragmentModule::class])
    abstract fun bindNearbyParentFragment(): NearbyParentFragment

    @ContributesAndroidInjector
    abstract fun bindBookmarkPictureListFragment(): BookmarkPicturesFragment

    @ContributesAndroidInjector(modules = [BookmarkLocationsFragmentModule::class])
    abstract fun bindBookmarkLocationListFragment(): BookmarkLocationsFragment

    @ContributesAndroidInjector(modules = [BookmarkItemsFragmentModule::class])
    abstract fun bindBookmarkItemListFragment(): BookmarkItemsFragment

    @ContributesAndroidInjector
    abstract fun bindBookmarkCategoriesListFragment(): BookmarkCategoriesFragment

    @ContributesAndroidInjector
    abstract fun bindReviewOutOfContextFragment(): ReviewImageFragment

    @ContributesAndroidInjector
    abstract fun bindUploadMediaDetailFragment(): UploadMediaDetailFragment

    @ContributesAndroidInjector
    abstract fun bindUploadCategoriesFragment(): UploadCategoriesFragment

    @ContributesAndroidInjector
    abstract fun bindDepictsFragment(): DepictsFragment

    @ContributesAndroidInjector
    abstract fun bindMediaLicenseFragment(): MediaLicenseFragment

    @ContributesAndroidInjector
    abstract fun bindParentDepictionsFragment(): ParentDepictionsFragment

    @ContributesAndroidInjector
    abstract fun bindChildDepictionsFragment(): ChildDepictionsFragment

    @ContributesAndroidInjector
    abstract fun bindCategoriesMediaFragment(): CategoriesMediaFragment

    @ContributesAndroidInjector
    abstract fun bindSubCategoriesFragment(): SubCategoriesFragment

    @ContributesAndroidInjector
    abstract fun bindParentCategoriesFragment(): ParentCategoriesFragment

    @ContributesAndroidInjector
    abstract fun bindExploreFragmentFragment(): ExploreFragment

    @ContributesAndroidInjector
    abstract fun bindExploreFeaturedRootFragment(): ExploreListRootFragment

    @ContributesAndroidInjector(modules = [ExploreMapFragmentModule::class])
    abstract fun bindExploreNearbyUploadsFragment(): ExploreMapFragment

    @ContributesAndroidInjector
    abstract fun bindExploreNearbyUploadsRootFragment(): ExploreMapRootFragment

    @ContributesAndroidInjector
    abstract fun bindBookmarkListRootFragment(): BookmarkListRootFragment

    @ContributesAndroidInjector
    abstract fun bindBookmarkFragmentFragment(): BookmarkFragment

    @ContributesAndroidInjector
    abstract fun bindMoreBottomSheetFragment(): MoreBottomSheetFragment

    @ContributesAndroidInjector
    abstract fun bindMoreBottomSheetLoggedOutFragment(): MoreBottomSheetLoggedOutFragment

    @ContributesAndroidInjector
    abstract fun bindAchievementsFragment(): AchievementsFragment

    @ContributesAndroidInjector
    abstract fun bindLeaderboardFragment(): LeaderboardFragment

    @ContributesAndroidInjector
    abstract fun bindPendingUploadsFragment(): PendingUploadsFragment

    @ContributesAndroidInjector
    abstract fun bindFailedUploadsFragment(): FailedUploadsFragment
    */
}
