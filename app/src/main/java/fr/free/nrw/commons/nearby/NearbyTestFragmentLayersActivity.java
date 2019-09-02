package fr.free.nrw.commons.nearby;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.ContributionsFragment;
import fr.free.nrw.commons.contributions.UnswipableViewPager;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.upload.UploadService;
import timber.log.Timber;

import static android.content.ContentResolver.requestSync;

public class NearbyTestFragmentLayersActivity extends AuthenticatedActivity {

    @Inject
    public SessionManager sessionManager;

    @BindView(R.id.tab_layout)
    TabLayout tabLayout;
    @BindView(R.id.pager)
    public UnswipableViewPager viewPager;
    public static final int CONTRIBUTIONS_TAB_POSITION = 0;
    public static final int NEARBY_TAB_POSITION = 1;
    public Intent uploadServiceIntent;
    public boolean isAuthCookieAcquired = false;
    public ContributionsActivityPagerAdapter contributionsActivityPagerAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_test_fragment_layers);
        ButterKnife.bind(this);
        requestAuthToken();
        initDrawer();
        setTitle(getString(R.string.navigation_item_home)+"2"); // Should I create a new string variable with another name instead?

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

    @Override
    protected void onAuthFailure() {

    }

    private void addTabsAndFragments() {
        contributionsActivityPagerAdapter = new ContributionsActivityPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(contributionsActivityPagerAdapter);

        tabLayout.addTab(tabLayout.newTab().setText(getResources().getString(R.string.contributions_fragment)));
        tabLayout.addTab(tabLayout.newTab().setText(getResources().getString(R.string.nearby_fragment)));

        // Set custom view to add nearby info icon next to text
        View nearbyTabLinearLayout = LayoutInflater.from(this).inflate(R.layout.custom_nearby_tab_layout, null);
        tabLayout.getTabAt(1).setCustomView(nearbyTabLinearLayout);

        setTabAndViewPagerSynchronisation();
    }

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

                        break;
                    case NEARBY_TAB_POSITION:
                        Timber.d("Nearby tab selected");
                        tabLayout.getTabAt(NEARBY_TAB_POSITION).select();
                        ((NearbyTestLayersFragment)contributionsActivityPagerAdapter.getItem(1)).nearbyParentFragmentPresenter.onTabSelected();
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
                        return retainedContributionsFragment;
                    } else {
                        // If we reach here, retainedContributionsFragment is null
                        return new ContributionsFragment();

                    }

                case 1:
                    NearbyTestLayersFragment retainedNearbyFragment = getNearbyFragment(1);
                    if (retainedNearbyFragment != null) {
                        return retainedNearbyFragment;
                    } else {
                        // If we reach here, retainedNearbyFragment is null
                        return new NearbyTestLayersFragment();
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
        private NearbyTestLayersFragment getNearbyFragment(int position) {
            String tag = makeFragmentName(R.id.pager, position);
            return (NearbyTestLayersFragment)fragmentManager.findFragmentByTag(tag);
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
}
