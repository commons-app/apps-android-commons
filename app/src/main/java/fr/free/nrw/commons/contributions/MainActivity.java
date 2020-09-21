package fr.free.nrw.commons.contributions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import butterknife.BindView;
import butterknife.ButterKnife;
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
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment;
import fr.free.nrw.commons.notification.Notification;
import fr.free.nrw.commons.notification.NotificationActivity;
import fr.free.nrw.commons.notification.NotificationController;
import fr.free.nrw.commons.quiz.QuizChecker;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.upload.UploadService;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class MainActivity  extends BaseActivity
    implements FragmentManager.OnBackStackChangedListener, CategoryImagesCallback {

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
    private MediaDetailPagerFragment mediaDetails;

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

    public static final int CONTRIBUTIONS_TAB_POSITION = 0;
    public static final int NEARBY_TAB_POSITION = 1;

    public boolean isContributionsFragmentVisible = true; // False means nearby fragment is visible
    public boolean onOrientationChanged;
    public Menu menu;

    private MenuItem notificationsMenuItem;
    public TextView notificationCount;

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
                // showTabs();
                // Nearby Notification Card View was invisible when Media Details Fragment is active, make it visible again on Contrib List Fragment active, according to preferences
                /*if (defaultKvStore.getBoolean("displayNearbyCardView", true)) {
                    if (contributionsFragment.nearbyNotificationCardView.cardViewVisibilityState == NearbyNotificationCardView.CardViewVisibilityState.READY) {
                        contributionsFragment.nearbyNotificationCardView.setVisibility(View.VISIBLE);
                    }
                } else {
                    contributionsFragment.nearbyNotificationCardView.setVisibility(View.GONE);
                }*/
            } else {
                finish();
            }
        } else if (nearbyParentFragment != null && activeFragment == ActiveFragment.NEARBY) {
            // Means that nearby fragment is visible
                nearbyParentFragment.backButtonClicked();
        } else if (exploreFragment != null && activeFragment == ActiveFragment.EXPLORE) {
            // Means that explore fragment is visible
                exploreFragment.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onBackStackChanged() {
        //initBackButton();
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contribution_activity_notification_menu, menu);

        notificationsMenuItem = menu.findItem(R.id.notifications);
        final View notification = notificationsMenuItem.getActionView();
        notificationCount = notification.findViewById(R.id.notification_count_badge);
        notification.setOnClickListener(view -> {
            NotificationActivity.startYourself(MainActivity.this, "unread");
        });
        this.menu = menu;
        updateMenuItem();
        setNotificationCount();
        return true;
    } */

    @SuppressLint("CheckResult")
    public void setNotificationCount() {
        compositeDisposable.add(notificationController.getNotifications(false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::initNotificationViews,
                        throwable -> Timber.e(throwable, "Error occurred while loading notifications")));
    }

    private void initNotificationViews(List<Notification> notificationList) {
        Timber.d("Number of notifications is %d", notificationList.size());
        if (notificationList.isEmpty()) {
            notificationCount.setVisibility(View.GONE);
        } else {
            notificationCount.setVisibility(View.VISIBLE);
            notificationCount.setText(String.valueOf(notificationList.size()));
        }
    }

    /**
     * Responsible with displaying required menu items according to displayed fragment.
     * Notifications icon when contributions list is visible, list sheet icon when nearby is visible
     */
    public void updateMenuItem() {
        if (menu != null) {
            if (isContributionsFragmentVisible) {
                // Display notifications menu item
                menu.findItem(R.id.notifications).setVisible(true);
                menu.findItem(R.id.list_sheet).setVisible(false);
                Timber.d("Contributions fragment notifications menu item is visible");
            } else {
                // Display bottom list menu item
                menu.findItem(R.id.notifications).setVisible(false);
                menu.findItem(R.id.list_sheet).setVisible(true);
                Timber.d("Nearby fragment list sheet menu item is visible");
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.notifications:
                // Starts notification activity on click to notification icon
                NotificationActivity.startYourself(this, "unread");
                return true;
            case R.id.list_sheet:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void viewPagerNotifyDataSetChanged() {
        // todo for explore
    }

    @Override
    public void onMediaClicked(int position) {
        // todo for explore
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
        setNotificationCount();
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
