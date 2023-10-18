package fr.free.nrw.commons.contributions;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.work.ExistingWorkPolicy;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.bookmarks.BookmarkFragment;
import fr.free.nrw.commons.explore.ExploreFragment;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.navtab.MoreBottomSheetFragment;
import fr.free.nrw.commons.navtab.MoreBottomSheetLoggedOutFragment;
import fr.free.nrw.commons.navtab.NavTab;
import fr.free.nrw.commons.navtab.NavTabLayout;
import fr.free.nrw.commons.navtab.NavTabLoggedOut;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment;
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment.NearbyParentFragmentInstanceReadyCallback;
import fr.free.nrw.commons.notification.NotificationActivity;
import fr.free.nrw.commons.notification.NotificationController;
import fr.free.nrw.commons.quiz.QuizChecker;
import fr.free.nrw.commons.settings.SettingsFragment;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.upload.worker.WorkRequestHelper;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtilWrapper;
import io.reactivex.schedulers.Schedulers;
import java.util.Collections;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class MainActivity  extends BaseActivity
    implements FragmentManager.OnBackStackChangedListener {

    @Inject
    SessionManager sessionManager;
    @Inject
    ContributionController controller;
    @Inject
    ContributionDao contributionDao;
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
    private MediaDetailPagerFragment mediaDetailPagerFragment;
    private NavTabLayout.OnNavigationItemSelectedListener navListener;

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
            if (!contributionsFragment.backButtonClicked()) {
                return false;
            }
        } else {
            onBackPressed();
            showTabs();
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        setContentView(R.layout.main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> {
            onSupportNavigateUp();
        });
        /*
        "first_edit_depict" is a key for getting information about opening the depiction editor
        screen for the first time after opening the app.

        Getting true by the key means the depiction editor screen is opened for the first time
        after opening the app.
        Getting false by the key means the depiction editor screen is not opened for the first time
        after opening the app.
         */
        applicationKvStore.putBoolean("first_edit_depict", true);
        if (applicationKvStore.getBoolean("login_skipped") == true) {
            setTitle(getString(R.string.navigation_item_explore));
            setUpLoggedOutPager();
        } else {
            if (applicationKvStore.getBoolean("firstrun", true)) {
                applicationKvStore.putBoolean("hasAlreadyLaunchedBigMultiupload", false);
            }
            if(savedInstanceState == null){
                //starting a fresh fragment.
                // Open Last opened screen if it is Contributions or Nearby, otherwise Contributions
                if(applicationKvStore.getBoolean("last_opened_nearby")){
                    setTitle(getString(R.string.nearby_fragment));
                    showNearby();
                    loadFragment(NearbyParentFragment.newInstance(),false);
                }else{
                    setTitle(getString(R.string.contributions_fragment));
                    loadFragment(ContributionsFragment.newInstance(),false);
                }
            }
            setUpPager();
            /**
             * Ask the user for media location access just after login
             * so that location in the EXIF metadata of the images shared by the user
             * is retained on devices running Android 10 or above
             */
            if (VERSION.SDK_INT >= VERSION_CODES.Q) {
                PermissionUtils.checkPermissionsAndPerformAction(
                    this,
                    () -> {},
                    R.string.media_location_permission_denied,
                    R.string.add_location_manually,
                    permission.ACCESS_MEDIA_LOCATION);
            }
        }
    }

    public void setSelectedItemId(int id) {
        tabLayout.setSelectedItemId(id);
    }

    private void setUpPager() {
        tabLayout.setOnNavigationItemSelectedListener(navListener = (item) -> {
            if (!item.getTitle().equals(getString(R.string.more))) {
                // do not change title for more fragment
                setTitle(item.getTitle());
            }
            // set last_opened_nearby true if item is nearby screen else set false
            applicationKvStore.putBoolean("last_opened_nearby",
                item.getTitle().equals(getString(R.string.nearby_fragment)));
            final Fragment fragment = NavTab.of(item.getOrder()).newInstance();
            return loadFragment(fragment, true);
        });
    }

    private void setUpLoggedOutPager() {
        loadFragment(ExploreFragment.newInstance(),false);
        tabLayout.setOnNavigationItemSelectedListener(item -> {
            if (!item.getTitle().equals(getString(R.string.more))) {
                // do not change title for more fragment
                setTitle(item.getTitle());
            }
            Fragment fragment = NavTabLoggedOut.of(item.getOrder()).newInstance();
            return loadFragment(fragment,true);
        });
    }

    private boolean loadFragment(Fragment fragment,boolean showBottom ) {
        //showBottom so that we do not show the bottom tray again when constructing
        //from the saved instance state.
        if (fragment instanceof ContributionsFragment) {
            if (activeFragment == ActiveFragment.CONTRIBUTIONS) {
                // scroll to top if already on the Contributions tab
                contributionsFragment.scrollToTop();
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
        } else if (fragment == null && showBottom) {
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
            setTitle(getResources().getString(R.string.contributions_fragment) +" "+ (
                !(uploadCount == 0) ?
                getResources()
                .getQuantityString(R.plurals.contributions_subtitle,
                    uploadCount, uploadCount):getString(R.string.contributions_subtitle_zero)));
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
        outState.putString("activeFragment", activeFragment.name());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String activeFragmentName = savedInstanceState.getString("activeFragment");
        if(activeFragmentName != null) {
            restoreActiveFragment(activeFragmentName);
        }
    }

    private void restoreActiveFragment(@NonNull String fragmentName) {
        if(fragmentName.equals(ActiveFragment.CONTRIBUTIONS.name())) {
            setTitle(getString(R.string.contributions_fragment));
            loadFragment(ContributionsFragment.newInstance(),false);
        }else if(fragmentName.equals(ActiveFragment.NEARBY.name())) {
            setTitle(getString(R.string.nearby_fragment));
            loadFragment(NearbyParentFragment.newInstance(),false);
        }else if(fragmentName.equals(ActiveFragment.EXPLORE.name())) {
            setTitle(getString(R.string.navigation_item_explore));
            loadFragment(ExploreFragment.newInstance(),false);
        }else if(fragmentName.equals(ActiveFragment.BOOKMARK.name())) {
            setTitle(getString(R.string.bookmarks));
            loadFragment(BookmarkFragment.newInstance(),false);
        }
    }

    @Override
    public void onBackPressed() {
        if (contributionsFragment != null && activeFragment == ActiveFragment.CONTRIBUTIONS) {
            // Means that contribution fragment is visible
            if (!contributionsFragment.backButtonClicked()) {//If this one does not wan't to handle
                // the back press, let the activity do so
                super.onBackPressed();
            }
        } else if (nearbyParentFragment != null && activeFragment == ActiveFragment.NEARBY) {
            // Means that nearby fragment is visible
            /* If function nearbyParentFragment.backButtonClick() returns false, it means that the bottomsheet is
              not expanded. So if the back button is pressed, then go back to the Contributions tab */
            if(!nearbyParentFragment.backButtonClicked()){
                getSupportFragmentManager().beginTransaction().remove(nearbyParentFragment).commit();
                setSelectedItemId(NavTab.CONTRIBUTIONS.code());
            }
        } else if (exploreFragment != null && activeFragment == ActiveFragment.EXPLORE) {
            // Means that explore fragment is visible
            if (!exploreFragment.onBackPressed()) {
                if (applicationKvStore.getBoolean("login_skipped")) {
                    super.onBackPressed();
                } else {
                    setSelectedItemId(NavTab.CONTRIBUTIONS.code());
                }
            }
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

    /**
     * Retry all failed uploads as soon as the user returns to the app
     */
    @SuppressLint("CheckResult")
    private void retryAllFailedUploads() {
        contributionDao.
            getContribution(Collections.singletonList(Contribution.STATE_FAILED))
            .subscribeOn(Schedulers.io())
            .subscribe(failedUploads -> {
                for (Contribution contribution: failedUploads) {
                    contributionsFragment.retryUpload(contribution);
                }
            });
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
            WorkRequestHelper.Companion.makeOneTimeWorkRequest(getApplicationContext(),
                ExistingWorkPolicy.APPEND_OR_REPLACE);
            viewUtilWrapper
                .showShortToast(getBaseContext(), getString(R.string.limited_connection_disabled));
        }
    }

    public void centerMapToPlace(Place place) {
        setSelectedItemId(NavTab.NEARBY.code());
        nearbyParentFragment.setNearbyParentFragmentInstanceReadyCallback(new NearbyParentFragmentInstanceReadyCallback() {
            // if mapBox initialize in nearbyParentFragment then MapReady() function called
            // so that nearbyParentFragemt.centerMaptoPlace(place) not throw any null pointer exception
            @Override
            public void onReady() {
                nearbyParentFragment.centerMapToPlace(place);
            }
        });
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

        if ((applicationKvStore.getBoolean("firstrun", true)) &&
            (!applicationKvStore.getBoolean("login_skipped"))) {
            defaultKvStore.putBoolean("inAppCameraFirstRun", true);
            WelcomeActivity.startYourself(this);
        }

        retryAllFailedUploads();
    }

    @Override
    protected void onDestroy() {
        quizChecker.cleanup();
        locationManager.unregisterLocationManager();
        // Remove ourself from hashmap to prevent memory leaks
        locationManager = null;
        super.onDestroy();
    }

    /**
     * Public method to show nearby from the reference of this.
     */
    public void showNearby() {
        tabLayout.setSelectedItemId(NavTab.NEARBY.code());
    }

    public enum ActiveFragment {
        CONTRIBUTIONS,
        NEARBY,
        EXPLORE,
        BOOKMARK,
        MORE
    }

    /**
     * Load default language in onCreate from SharedPreferences
     */
    private void loadLocale(){
        final SharedPreferences preferences = getSharedPreferences("Settings", Activity.MODE_PRIVATE);
        final String language = preferences.getString("language", "");
        final SettingsFragment settingsFragment = new SettingsFragment();
        settingsFragment.setLocale(this, language);
    }

    public NavTabLayout.OnNavigationItemSelectedListener getNavListener(){
        return navListener;
    }
}
