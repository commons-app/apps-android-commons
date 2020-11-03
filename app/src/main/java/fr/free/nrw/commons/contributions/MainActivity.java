package fr.free.nrw.commons.contributions;

import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.bookmarks.BookmarkFragment;
import fr.free.nrw.commons.category.CategoryImagesCallback;
import fr.free.nrw.commons.explore.ExploreFragment;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.navtab.MoreBottomSheetFragment;
import fr.free.nrw.commons.navtab.MoreBottomSheetLoggedOutFragment;
import fr.free.nrw.commons.navtab.NavTab;
import fr.free.nrw.commons.navtab.NavTabLayout;
import fr.free.nrw.commons.navtab.NavTabLoggedOut;
import fr.free.nrw.commons.nearby.NearbyNotificationCardView;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment;
import fr.free.nrw.commons.notification.NotificationActivity;
import fr.free.nrw.commons.notification.NotificationController;
import fr.free.nrw.commons.quiz.QuizChecker;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.upload.UploadService;
import fr.free.nrw.commons.utils.ViewUtilWrapper;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class MainActivity  extends BaseActivity
    implements FragmentManager.OnBackStackChangedListener {

    @Inject
    SessionManager sessionManager;
    @Inject
    ContributionController controller;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.pager)
    public UnswipableViewPager viewPager;
    @BindView(R.id.fragmentContainer)
    public FrameLayout fragmentContainer;
    @BindView(R.id.fragment_main_nav_tab_layout)
    NavTabLayout tabLayout;

    private ContributionsFragment contributionsFragment;
    private NearbyParentFragment nearbyParentFragment;
    private ExploreFragment exploreFragment;
    private BookmarkFragment bookmarkFragment;
    public ActiveFragment activeFragment;

    @Inject
    public LocationServiceManager locationManager;
    @Inject
    NotificationController notificationController;
    @Inject
    QuizChecker quizChecker;
    @Inject
    @Named("default_preferences")
    public
    JsonKvStore applicationKvStore;
    @Inject
    ViewUtilWrapper viewUtilWrapper;

    public Menu menu;

    /**
     * Consumers should be simply using this method to use this activity.
     *
     * @param context A Context of the application package implementing this class.
     */
    public static void startYourself(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (activeFragment == ActiveFragment.CONTRIBUTIONS) {
            contributionsFragment.backButtonClicked();
        } else {
            onBackPressed();
            showTabs();
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        if (applicationKvStore.getBoolean("login_skipped") == true) {
            setTitle(getString(R.string.explore_tab_title_mobile));
            setUpLoggedOutPager();
        } else {
            setTitle(getString(R.string.contributions_fragment));
            setUpPager();
            initMain();
        }
    }

    public void setSelectedItemId(int id) {
        tabLayout.setSelectedItemId(id);
    }

    private void setUpPager() {
        loadFragment(ContributionsFragment.newInstance());
        tabLayout.setOnNavigationItemSelectedListener(item -> {
            if (!item.getTitle().equals("More")) {
                // do not change title for more fragment
                setTitle(item.getTitle());
            }
            Fragment fragment = NavTab.of(item.getOrder()).newInstance();
            return loadFragment(fragment);
        });
    }

    private void setUpLoggedOutPager() {
        loadFragment(ExploreFragment.newInstance());
        tabLayout.setOnNavigationItemSelectedListener(item -> {
            if (!item.getTitle().equals("More")) {
                // do not change title for more fragment
                setTitle(item.getTitle());
            }
            Fragment fragment = NavTabLoggedOut.of(item.getOrder()).newInstance();
            return loadFragment(fragment);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment instanceof ContributionsFragment) {
            if (activeFragment == ActiveFragment.CONTRIBUTIONS) { // Do nothing if same tab
                return true;
            }
            contributionsFragment = (ContributionsFragment) fragment;
            activeFragment = ActiveFragment.CONTRIBUTIONS;
        } else if (fragment instanceof NearbyParentFragment) {
            if (activeFragment == ActiveFragment.NEARBY) { // Do nothing if same tab
                return true;
            }
            nearbyParentFragment = (NearbyParentFragment) fragment;
            activeFragment = ActiveFragment.NEARBY;
        } else if (fragment instanceof ExploreFragment) {
            if (activeFragment == ActiveFragment.EXPLORE) { // Do nothing if same tab
                return true;
            }
            exploreFragment = (ExploreFragment) fragment;
            activeFragment = ActiveFragment.EXPLORE;
        } else if (fragment instanceof BookmarkFragment) {
            if (activeFragment == ActiveFragment.BOOKMARK) { // Do nothing if same tab
                return true;
            }
            bookmarkFragment = (BookmarkFragment) fragment;
            activeFragment = ActiveFragment.BOOKMARK;
        } else if (fragment == null) {
            if (applicationKvStore.getBoolean("login_skipped") == true) { // If logged out, more sheet is different
                MoreBottomSheetLoggedOutFragment bottomSheet = new MoreBottomSheetLoggedOutFragment();
                bottomSheet.show(getSupportFragmentManager(),
                    "MoreBottomSheetLoggedOut");
            } else {
                MoreBottomSheetFragment bottomSheet = new MoreBottomSheetFragment();
                bottomSheet.show(getSupportFragmentManager(),
                    "MoreBottomSheet");
            }
        }

        if (fragment != null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
            return true;
        }
        return false;
    }

    public void hideTabs() {
        tabLayout.setVisibility(View.GONE);
    }

    public void showTabs() {
        tabLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Adds number of uploads next to tab text "Contributions" then it will look like
     * "Contributions (NUMBER)"
     * @param uploadCount
     */
    public void setNumOfUploads(int uploadCount) {
        if (activeFragment == ActiveFragment.CONTRIBUTIONS) {
            setTitle(getResources().getString(R.string.contributions_fragment) +" "+ getResources()
                .getQuantityString(R.plurals.contributions_subtitle,
                    uploadCount, uploadCount));
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        //quizChecker.initQuizCheck(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("viewPagerCurrentItem", viewPager.getCurrentItem());
    }

    private void initMain() {
        //Do not remove this, this triggers the sync service
        Intent uploadServiceIntent = new Intent(this, UploadService.class);
        uploadServiceIntent.setAction(UploadService.ACTION_START_SERVICE);
        startService(uploadServiceIntent);
    }

    @Override
    public void onBackPressed() {
        if (contributionsFragment != null && activeFragment == ActiveFragment.CONTRIBUTIONS) {
            // Meas that contribution fragment is visible (not nearby fragment)
            if (contributionsFragment.getChildFragmentManager().findFragmentByTag(ContributionsFragment.MEDIA_DETAIL_PAGER_FRAGMENT_TAG) != null) {
                // Means that media details fragment is visible to uer instead of contributions list fragment (As chils fragment)
                // Then we want to go back to contributions list fragment on backbutton pressed from media detail fragment
                contributionsFragment.getChildFragmentManager().popBackStack();
                // Tabs were invisible when Media Details Fragment is active, make them visible again on Contrib List Fragment active
                showTabs();
                // Nearby Notification Card View was invisible when Media Details Fragment is active, make it visible again on Contrib List Fragment active, according to preferences
                if (defaultKvStore.getBoolean("displayNearbyCardView", true)) {
                    if (contributionsFragment.nearbyNotificationCardView.cardViewVisibilityState == NearbyNotificationCardView.CardViewVisibilityState.READY) {
                        contributionsFragment.nearbyNotificationCardView.setVisibility(View.VISIBLE);
                    }
                } else {
                    contributionsFragment.nearbyNotificationCardView.setVisibility(View.GONE);
                }
                contributionsFragment.campaignView.setVisibility(View.VISIBLE);
            } else {
                super.onBackPressed();
            }
        } else if (nearbyParentFragment != null && activeFragment == ActiveFragment.NEARBY) {
            // Means that nearby fragment is visible
            nearbyParentFragment.backButtonClicked();
        } else if (exploreFragment != null && activeFragment == ActiveFragment.EXPLORE) {
            // Means that explore fragment is visible
            exploreFragment.onBackPressed();
        } else if (bookmarkFragment != null && activeFragment == ActiveFragment.BOOKMARK) {
            // Means that bookmark fragment is visible
            bookmarkFragment.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onBackStackChanged() {
        //initBackButton();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.notifications:
                // Starts notification activity on click to notification icon
                NotificationActivity.startYourself(this, "unread");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void toggleLimitedConnectionMode() {
        defaultKvStore.putBoolean(CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED,
            !defaultKvStore
                .getBoolean(CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED, false));
        if (defaultKvStore
            .getBoolean(CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED, false)) {
            viewUtilWrapper
                .showShortToast(getBaseContext(), getString(R.string.limited_connection_enabled));
        } else {
            Intent intent = new Intent(this, UploadService.class);
            intent.setAction(UploadService.PROCESS_PENDING_LIMITED_CONNECTION_MODE_UPLOADS);
            if (VERSION.SDK_INT >= VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            viewUtilWrapper
                .showShortToast(getBaseContext(), getString(R.string.limited_connection_disabled));
        }
    }

    public void centerMapToPlace(Place place) {
        setSelectedItemId(NavTab.NEARBY.code());
        nearbyParentFragment.centerMapToPlace(place);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Timber.d(data!=null?data.toString():"onActivityResult data is null");
        super.onActivityResult(requestCode, resultCode, data);
        controller.handleActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        quizChecker.cleanup();
        locationManager.unregisterLocationManager();
        // Remove ourself from hashmap to prevent memory leaks
        locationManager = null;
        super.onDestroy();
    }

    public enum ActiveFragment {
        CONTRIBUTIONS,
        NEARBY,
        EXPLORE,
        BOOKMARK,
        MORE
    }
}
