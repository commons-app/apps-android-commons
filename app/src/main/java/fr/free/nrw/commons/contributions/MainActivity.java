package fr.free.nrw.commons.contributions;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.navtab.NavTabFragmentPagerAdapter;
import fr.free.nrw.commons.navtab.NavTabLayout;
import fr.free.nrw.commons.notification.Notification;
import fr.free.nrw.commons.notification.NotificationController;
import fr.free.nrw.commons.quiz.QuizChecker;
import fr.free.nrw.commons.upload.UploadService;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.content.ContentResolver.requestSync;
import static fr.free.nrw.commons.location.LocationServiceManager.LOCATION_REQUEST;

public class MainActivity extends AuthenticatedActivity implements FragmentManager.OnBackStackChangedListener , AdapterView.OnItemClickListener{

    @Inject
    SessionManager sessionManager;
    @Inject
    ContributionController controller;
    @BindView(R.id.pager)
    public UnswipableViewPager viewPager;
    @BindView(R.id.fragment_main_nav_tab_layout)
    NavTabLayout tabLayout;
    private MediaDetailPagerFragment mediaDetails;



    @Inject
    public LocationServiceManager locationManager;
    @Inject
    NotificationController notificationController;
    @Inject
    QuizChecker quizChecker;


    public Intent uploadServiceIntent;
    public boolean isAuthCookieAcquired = false;

    public final int CONTRIBUTIONS_TAB_POSITION = 0;


    private boolean onOrientationChanged = false;

    private MenuItem notificationsMenuItem;
    private TextView notificationCount;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ButterKnife.bind(this);

        requestAuthToken();
        initDrawer();
        setTitle(getString(R.string.navigation_item_home)); // Should I create a new string variable with another name instead?
        setUpPager();

        if (savedInstanceState != null) {
            onOrientationChanged = true; // Will be used in nearby fragment to determine significant update of map

            //If nearby map was visible, call on Tab Selected to call all nearby operations
            /*if (savedInstanceState.getInt("viewPagerCurrentItem") == 1) {
                ((NearbyFragment)contributionsActivityPagerAdapter.getItem(1)).onTabSelected(onOrientationChanged);
            }*/
        }
    }

    private void setUpPager() {
        viewPager.setAdapter(new NavTabFragmentPagerAdapter(getSupportFragmentManager()));
        viewPager.setOffscreenPageLimit(2);
        tabLayout.setOnNavigationItemSelectedListener(item -> {
            viewPager.setCurrentItem(item.getOrder());
            return true;
        });
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("viewPagerCurrentItem", viewPager.getCurrentItem());
    }

    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        // Do a sync everytime we get here!
        requestSync(sessionManager.getCurrentAccount(), BuildConfig.CONTRIBUTION_AUTHORITY, new Bundle());
        uploadServiceIntent = new Intent(this, UploadService.class);
        uploadServiceIntent.setAction(UploadService.ACTION_START_SERVICE);
        startService(uploadServiceIntent);

        //Todo: after needs to happen at addTabsAndFragments();
        isAuthCookieAcquired = true;
        /*if (contributionsActivityPagerAdapter.getItem(0) != null) {
            ((ContributionsFragment)contributionsActivityPagerAdapter.getItem(0)).onAuthCookieAcquired(uploadServiceIntent);
        }*/
    }


    @Override
    protected void onAuthFailure() {

    }

    @Override
    public void onBackPressed() {
       /* Todo: after stability
       DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        String contributionsFragmentTag = ((ContributionsActivityPagerAdapter) viewPager.getAdapter()).makeFragmentName(R.id.pager, 0);
        String nearbyFragmentTag = ((ContributionsActivityPagerAdapter) viewPager.getAdapter()).makeFragmentName(R.id.pager, 1);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (getSupportFragmentManager().findFragmentByTag(contributionsFragmentTag) != null && isContributionsFragmentVisible) {
            // Meas that contribution fragment is visible (not nearby fragment)
            ContributionsFragment contributionsFragment = (ContributionsFragment) getSupportFragmentManager().findFragmentByTag(contributionsFragmentTag);

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
            } else {
                finish();
            }
        } else if (getSupportFragmentManager().findFragmentByTag(nearbyFragmentTag) != null && !isContributionsFragmentVisible) {
            // Means that nearby fragment is visible (not contributions fragment)
            NearbyFragment nearbyFragment = (NearbyFragment) contributionsActivityPagerAdapter.getItem(1);

            if(nearbyFragment.isBottomSheetExpanded()) {
                // Back should first hide the bottom sheet if it is expanded
                nearbyFragment.listOptionMenuItemClicked();
            } else {
                // Otherwise go back to contributions fragment
                viewPager.setCurrentItem(0);
            }
        } else {
            super.onBackPressed();
        }*/
    }

    @Override
    public void onBackStackChanged() {
        initBackButton();
    }

   /* Todo: after set up menu
   @Override
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
        //updateMenuItem();
        setNotificationCount();
        return true;
    }*/

    @SuppressLint("CheckResult")
    private void setNotificationCount() {
        compositeDisposable.add(Observable.fromCallable(() -> notificationController.getNotifications(false))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::initNotificationViews,
                        throwable -> Timber.e(throwable, "Error occurred while loading notifications")));
    }

    private void initNotificationViews(List<Notification> notificationList) {
     /*   Timber.d("Number of notifications is %d", notificationList.size());
        if (notificationList.isEmpty()) {
            notificationCount.setVisibility(View.GONE);
        } else {
            //notificationCount.setVisibility(View.VISIBLE);
            notificationCount.setText(String.valueOf(notificationList.size()));
        }*/
    }

    /*Todo: after set up menu
     */

    /**
     * Responsible with displaying required menu items according to displayed fragment.
     * Notifications icon when contributions list is visible, list sheet icon when nearby is visible
     *//*

    private void updateMenuItem() {
        if (menu != null) {
            if (isContributionsFragmentVisible) {
                // Display notifications menu item
                menu.findItem(R.id.notifications).setVisible(true);
                menu.findItem(R.id.list_sheet).setVisible(false);
                Timber.d("Contributions activity notifications menu item is visible");
            } else {
                // Display bottom list menu item
                menu.findItem(R.id.notifications).setVisible(false);
                menu.findItem(R.id.list_sheet).setVisible(true);
                Timber.d("Contributions activity list sheet menu item is visible");
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
                if (contributionsActivityPagerAdapter.getItem(1) != null) {
                    ((NearbyFragment)contributionsActivityPagerAdapter.getItem(1)).listOptionMenuItemClicked();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Timber.d(data != null ? data.toString() : "onActivityResult data is null");
        super.onActivityResult(requestCode, resultCode, data);
        controller.handleActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST: {
                //Todo after set this right
                // If request is cancelled, the result arrays are empty.
               /* if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Location permission given");
                    ((ContributionsFragment)contributionsActivityPagerAdapter
                            .getItem(0)).locationManager.registerLocationManager();
                } else {
                    // If nearby fragment is visible and location permission is not given, send user back to contrib fragment
                    if (!isContributionsFragmentVisible) {
                        viewPager.setCurrentItem(CONTRIBUTIONS_TAB_POSITION);

                        // TODO: If contrib fragment is visible and location permission is not given, display permission request button
                    }
                }*/
                return;
            }

            default:
                return;
        }
    }

    public void hideTabs() {
        changeDrawerIconToBackButton();
        if (tabLayout != null) {
            tabLayout.setVisibility(View.GONE);
        }
    }

    public void showTabs() {
        changeDrawerIconToDefault();
        if (tabLayout != null) {
            tabLayout.setVisibility(View.VISIBLE);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        setNotificationCount();
        quizChecker.initQuizCheck(this);
    }

    @Override
    protected void onDestroy() {
        quizChecker.cleanup();
        locationManager.unregisterLocationManager();
        // Remove ourself from hashmap to prevent memory leaks
        locationManager = null;
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int i, long id) {
        if (mediaDetails == null || !mediaDetails.isVisible()) {
            // set isFeaturedImage true for featured images, to include author field on media detail
            mediaDetails = new MediaDetailPagerFragment(false, true);
            FragmentManager supportFragmentManager = getSupportFragmentManager();
            supportFragmentManager
                    .beginTransaction()
                    .hide(supportFragmentManager.getFragments().get(supportFragmentManager.getBackStackEntryCount()))
                    .add(R.id.fragmentContainer, mediaDetails)
                    .addToBackStack(null)
                    .commit();
            // Reason for using hide, add instead of replace is to maintain scroll position after
            // coming back to the search activity. See https://github.com/commons-app/apps-android-commons/issues/1631
            // https://stackoverflow.com/questions/11353075/how-can-i-maintain-fragment-state-when-added-to-the-back-stack/19022550#19022550            supportFragmentManager.executePendingTransactions();
        }
        mediaDetails.showImage(i);
        forceInitBackButton();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
