package fr.free.nrw.commons.contributions;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.category.CategoryImagesCallback;
import fr.free.nrw.commons.di.CommonsDaggerAppCompatActivity;
import fr.free.nrw.commons.explore.ExploreFragment;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.navtab.NavTab;
import fr.free.nrw.commons.navtab.NavTabLayout;
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment;
import fr.free.nrw.commons.notification.Notification;
import fr.free.nrw.commons.notification.NotificationActivity;
import fr.free.nrw.commons.notification.NotificationController;
import fr.free.nrw.commons.quiz.QuizChecker;
import fr.free.nrw.commons.upload.UploadService;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

public class MainActivity  extends CommonsDaggerAppCompatActivity
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

    @Inject
    public LocationServiceManager locationManager;
    @Inject
    NotificationController notificationController;
    @Inject
    QuizChecker quizChecker;

    public static final int CONTRIBUTIONS_TAB_POSITION = 0;
    public static final int NEARBY_TAB_POSITION = 1;

    public boolean isContributionsFragmentVisible = true; // False means nearby fragment is visible
    public boolean onOrientationChanged;
    private Menu menu;

    private MenuItem notificationsMenuItem;
    private TextView notificationCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        setTitle(getString(R.string.contributions_fragment));
        setUpPager();
        initMain();
    }

    private void setUpPager() {
        loadFragment(ContributionsFragment.newInstance());
        tabLayout.setOnNavigationItemSelectedListener(item -> {
            setTitle(item.getTitle());
            Fragment fragment = NavTab.of(item.getOrder()).newInstance();
            return loadFragment(fragment);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment instanceof ContributionsFragment) {
            Log.d("deneme7","1");
            contributionsFragment = (ContributionsFragment) fragment;
        } else if (fragment instanceof NearbyParentFragment) {
            Log.d("deneme7","2");
            nearbyParentFragment = (NearbyParentFragment) fragment;
        } else if (fragment instanceof ExploreFragment) {
            Log.d("deneme7","3");
            exploreFragment = (ExploreFragment) fragment;
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
        if (contributionsFragment != null && isContributionsFragmentVisible) {
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
        } else if (nearbyParentFragment != null && !isContributionsFragmentVisible) {
            // Means that nearby fragment is visible (not contributions fragment)
            if (null != nearbyParentFragment) {
                nearbyParentFragment.backButtonClicked();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onBackStackChanged() {
        //initBackButton();
    }

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
        updateMenuItem();
        setNotificationCount();
        return true;
    }

    @SuppressLint("CheckResult")
    private void setNotificationCount() {
        /*compositeDisposable.add(notificationController.getNotifications(false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::initNotificationViews,
                        throwable -> Timber.e(throwable, "Error occurred while loading notifications")));*/
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
    private void updateMenuItem() {
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

    public class ContributionsActivityPagerAdapter extends FragmentPagerAdapter {
        FragmentManager fragmentManager;

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
                        return retainedContributionsFragment;
                    } else {
                        // If we reach here, retainedContributionsFragment is null
                        return new ContributionsFragment();

                    }

                case 1:
                    nearbyParentFragment = getNearbyFragment(1);
                    if (nearbyParentFragment != null) {
                        return nearbyParentFragment;
                    } else {
                        // If we reach here, retainedNearbyFragment is null
                        nearbyParentFragment=new NearbyParentFragment();
                        return nearbyParentFragment;
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
        private NearbyParentFragment getNearbyFragment(int position) {
            String tag = makeFragmentName(R.id.pager, position);
            return (NearbyParentFragment)fragmentManager.findFragmentByTag(tag);
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
}
