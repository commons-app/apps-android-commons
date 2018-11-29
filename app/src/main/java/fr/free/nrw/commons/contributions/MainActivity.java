package fr.free.nrw.commons.contributions;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;


import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.nearby.NearbyFragment;
import fr.free.nrw.commons.nearby.NearbyMapFragment;
import fr.free.nrw.commons.nearby.NearbyNotificationCardView;
import fr.free.nrw.commons.notification.NotificationActivity;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.upload.UploadService;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import timber.log.Timber;

import static android.content.ContentResolver.requestSync;
import static fr.free.nrw.commons.location.LocationServiceManager.LOCATION_REQUEST;

public class MainActivity extends AuthenticatedActivity implements FragmentManager.OnBackStackChangedListener {

    @Inject
    SessionManager sessionManager;
    @BindView(R.id.tab_layout)
    TabLayout tabLayout;
    @BindView(R.id.pager)
    public UnswipableViewPager viewPager;
    @Inject
    public LocationServiceManager locationManager;
    @Inject
    @Named("default_preferences")
    public SharedPreferences prefs;


    public Intent uploadServiceIntent;
    public boolean isAuthCookieAcquired = false;

    public ContributionsActivityPagerAdapter contributionsActivityPagerAdapter;
    public final int CONTRIBUTIONS_TAB_POSITION = 0;
    public final int NEARBY_TAB_POSITION = 1;

    public boolean isContributionsFragmentVisible = true; // False means nearby fragment is visible
    private Menu menu;
    private boolean isThereUnreadNotifications = false;

    private boolean onOrientationChanged = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contributions);
        ButterKnife.bind(this);

        requestAuthToken();
        initDrawer();
        setTitle(getString(R.string.navigation_item_home)); // Should I create a new string variable with another name instead?

        if (savedInstanceState != null ) {
            onOrientationChanged = true; // Will be used in nearby fragment to determine significant update of map

            //If nearby map was visible, call on Tab Selected to call all nearby operations
            /*if (savedInstanceState.getInt("viewPagerCurrentItem") == 1) {
                ((NearbyFragment)contributionsActivityPagerAdapter.getItem(1)).onTabSelected(onOrientationChanged);
            }*/
        }
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

        addTabsAndFragments();
        isAuthCookieAcquired = true;
        if (contributionsActivityPagerAdapter.getItem(0) != null) {
            ((ContributionsFragment)contributionsActivityPagerAdapter.getItem(0)).onAuthCookieAcquired(uploadServiceIntent);
        }
    }

    private void addTabsAndFragments() {
        contributionsActivityPagerAdapter = new ContributionsActivityPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(contributionsActivityPagerAdapter);

        tabLayout.addTab(tabLayout.newTab().setText(getResources().getString(R.string.contributions_fragment)));
        tabLayout.addTab(tabLayout.newTab().setText(getResources().getString(R.string.nearby_fragment)));

        // Set custom view to add nearby info icon next to text
        View nearbyTabLinearLayout = LayoutInflater.from(this).inflate(R.layout.custom_nearby_tab_layout, null);
        ImageView nearbyInfo = nearbyTabLinearLayout.findViewById(R.id.nearby_info_image);
        tabLayout.getTabAt(1).setCustomView(nearbyTabLinearLayout);

        nearbyInfo.setOnClickListener(view ->
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.title_activity_nearby)
                    .setMessage(R.string.showcase_view_whole_nearby_activity)
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.cancel())
                    .create()
                    .show()
        );

        if (uploadServiceIntent != null) {
            // If auth cookie already acquired notify contrib fragment so that it san operate auth required actions
            ((ContributionsFragment)contributionsActivityPagerAdapter.getItem(CONTRIBUTIONS_TAB_POSITION)).onAuthCookieAcquired(uploadServiceIntent);
        }
        setTabAndViewPagerSynchronisation();
    }

    /**
     * Adds number of uploads next to tab text "Contributions" then it will look like
     * "Contributions (NUMBER)"
     * @param uploadCount
     */
    public void setNumOfUploads(int uploadCount) {
        tabLayout.getTabAt(0).setText(getResources().getString(R.string.contributions_fragment) +" "+ getResources()
                .getQuantityString(R.plurals.contributions_subtitle,
                        uploadCount, uploadCount));
    }

    /**
     * Normally tab layout and view pager has no relation, which means when you swipe view pager
     * tab won't change and vice versa. So we have to notify each of them.
     */
    private void setTabAndViewPagerSynchronisation() {
        //viewPager.canScrollHorizontally(false);
        viewPager.setFocusableInTouchMode(true);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case CONTRIBUTIONS_TAB_POSITION:
                        Timber.d("Contributions tab selected");
                        tabLayout.getTabAt(CONTRIBUTIONS_TAB_POSITION).select();
                        isContributionsFragmentVisible = true;
                        updateMenuItem();

                        break;
                    case NEARBY_TAB_POSITION:
                        Timber.d("Nearby tab selected");
                        tabLayout.getTabAt(NEARBY_TAB_POSITION).select();
                        isContributionsFragmentVisible = false;
                        updateMenuItem();
                        // Do all permission and GPS related tasks on tab selected, not on create
                        ((NearbyFragment)contributionsActivityPagerAdapter.getItem(1)).onTabSelected(onOrientationChanged);
                        break;
                    default:
                        tabLayout.getTabAt(CONTRIBUTIONS_TAB_POSITION).select();
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
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
    protected void onAuthFailure() {

    }

    @Override
    public void onBackPressed() {
        String contributionsFragmentTag = ((ContributionsActivityPagerAdapter) viewPager.getAdapter()).makeFragmentName(R.id.pager, 0);
        String nearbyFragmentTag = ((ContributionsActivityPagerAdapter) viewPager.getAdapter()).makeFragmentName(R.id.pager, 1);
        if (getSupportFragmentManager().findFragmentByTag(contributionsFragmentTag) != null && isContributionsFragmentVisible) {
            // Meas that contribution fragment is visible (not nearby fragment)
            ContributionsFragment contributionsFragment = (ContributionsFragment) getSupportFragmentManager().findFragmentByTag(contributionsFragmentTag);

            if (contributionsFragment.getChildFragmentManager().findFragmentByTag(ContributionsFragment.MEDIA_DETAIL_PAGER_FRAGMENT_TAG) != null) {
                // Means that media details fragment is visible to uer instead of contributions list fragment (As chils fragment)
                // Then we want to go back to contributions list fragment on backbutton pressed from media detail fragment
                contributionsFragment.getChildFragmentManager().popBackStack();
                // Tabs were invisible when Media Details Fragment is active, make them visible again on Contrib List Fragment active
                showTabs();
                // Nearby Notification Card View was invisible when Media Details Fragment is active, make it visible again on Contrib List Fragment active, according to preferences
                if (prefs.getBoolean("displayNearbyCardView", true)) {
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
            // Meas that nearby fragment is visible (not contributions fragment)
            // Set current item to contributions activity instead of closing the activity
            viewPager.setCurrentItem(0);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onBackStackChanged() {
        initBackButton();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contribution_activity_notification_menu, menu);

        if (!isThereUnreadNotifications) {
            // TODO: used vectors are not compatible with API 19 and below, change them
            menu.findItem(R.id.notifications).setIcon(ContextCompat.getDrawable(this, R.drawable.ic_notification_white_clip_art));
        } else {
            menu.findItem(R.id.notifications).setIcon(ContextCompat.getDrawable(this, R.drawable.ic_notification_white_clip_art_dot));
        }

        this.menu = menu;

        updateMenuItem();

        return true;
    }

    /**
     * Responsible with displaying required menu items according to displayed fragment.
     * Notifications icon when contributions list is visible, list sheet icon when nearby is visible
     */
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
                NavigationBaseActivity.startActivityWithFlags(this, NotificationActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                finish();
                return true;

            case R.id.list_sheet:
                if (contributionsActivityPagerAdapter.getItem(1) != null) {
                    ((NearbyFragment)contributionsActivityPagerAdapter.getItem(1)).listOptionMenuIteClicked();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean deviceHasCamera() {
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    /**
     * Update notification icon if there is an unread notification
     * @param isThereUnreadNotifications true if user didn't visit notifications activity since
     *                                   latest notification came to account
     */
    public void updateNotificationIcon(boolean isThereUnreadNotifications) {
        if (!isThereUnreadNotifications) {
            this.isThereUnreadNotifications = false;
            menu.findItem(R.id.notifications).setIcon(ContextCompat.getDrawable(this, R.drawable.ic_notification_white_clip_art));
        } else {
            this.isThereUnreadNotifications = true;
            menu.findItem(R.id.notifications).setIcon(ContextCompat.getDrawable(this, R.drawable.ic_notification_white_clip_art_dot));
        }
    }

    public class ContributionsActivityPagerAdapter extends FragmentPagerAdapter {
        FragmentManager fragmentManager;
        private boolean isContributionsListFragment = true; // to know what to put in first tab, Contributions of Media Details


        public ContributionsActivityPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
            this.fragmentManager = fragmentManager;
        }

        @Override
        public int getCount() {
            return 2;
        }

        /*
         * Do not use getItem method to access fragments on pager adapter. User reference variables
         * instead.
         * */
        @Override
        public Fragment getItem(int position) {
            switch (position){
                case 0:
                    ContributionsFragment retainedContributionsFragment = getContributionsFragment(0);
                    if (retainedContributionsFragment != null) {
                        //  ContributionsFragment is parent of ContributionsListFragment and
                        //  MediaDetailsFragment. If below decides which child will be visible.
                        if (isContributionsListFragment) {
                            retainedContributionsFragment.setContributionsListFragment();
                        } else {
                            retainedContributionsFragment.setMediaDetailPagerFragment();
                        }
                        return retainedContributionsFragment;
                    } else {
                        // If we reach here, retainedContributionsFragment is null
                        return new ContributionsFragment();

                    }

                case 1:
                    NearbyFragment retainedNearbyFragment = getNearbyFragment(1);
                    if (retainedNearbyFragment != null) {
                        return retainedNearbyFragment;
                    } else {
                        // If we reach here, retainedNearbyFragment is null
                        return new NearbyFragment();
                    }
                default:
                    return null;
            }
        }

        /**
         * Generates fragment tag with makeFragmentName method to get retained contributions fragment
         * @param position index of tabs, in our case 0 or 1
         * @return
         */
        private ContributionsFragment getContributionsFragment(int position) {
            String tag = makeFragmentName(R.id.pager, position);
            return (ContributionsFragment)fragmentManager.findFragmentByTag(tag);
        }

        /**
         * Generates fragment tag with makeFragmentName method to get retained nearby fragment
         * @param position index of tabs, in our case 0 or 1
         * @return
         */
        private NearbyFragment getNearbyFragment(int position) {
            String tag = makeFragmentName(R.id.pager, position);
            return (NearbyFragment)fragmentManager.findFragmentByTag(tag);
        }

        /**
         * A simple hack to use retained fragment when getID is called explicitly, if we don't use
         * this method, a new fragment will be initialized on each explicit calls of getID
         * @param viewId id of view pager
         * @param index index of tabs, in our case 0 or 1
         * @return
         */
        public String makeFragmentName(int viewId, int index) {
            return "android:switcher:" + viewId + ":" + index;
        }

        /**
         * In first tab we can have ContributionsFragment or Media details fragment. This method
         * is responsible to update related boolean
         * @param isContributionsListFragment true when contribution fragment should be visible, false
         *                                means user clicked to MediaDetails
         */
        private void updateContributionFragmentTabContent(boolean isContributionsListFragment) {
            this.isContributionsListFragment = isContributionsListFragment;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ContributionsListFragment contributionsListFragment =
                        (ContributionsListFragment) contributionsActivityPagerAdapter
                        .getItem(0).getChildFragmentManager()
                        .findFragmentByTag(ContributionsFragment.CONTRIBUTION_LIST_FRAGMENT_TAG);
        contributionsListFragment.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Location permission given");
                    ((ContributionsFragment)contributionsActivityPagerAdapter
                            .getItem(0)).locationManager.registerLocationManager();
                } else {
                    // If nearby fragment is visible and location permission is not given, send user back to contrib fragment
                    if (!isContributionsFragmentVisible) {
                        viewPager.setCurrentItem(CONTRIBUTIONS_TAB_POSITION);

                    // TODO: If contrib fragment is visible and location permission is not given, display permission request button
                    } else {

                    }
                }
                return;
            }

            default:
                return;
        }
    }

    @Override
    protected void onDestroy() {
        locationManager.unregisterLocationManager();
        // Remove ourself from hashmap to prevent memory leaks
        locationManager = null;
        super.onDestroy();
    }
}
