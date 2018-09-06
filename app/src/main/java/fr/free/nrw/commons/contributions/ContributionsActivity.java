package fr.free.nrw.commons.contributions;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.nearby.NearbyFragment;
import fr.free.nrw.commons.upload.UploadService;

import static android.content.ContentResolver.requestSync;

public  class       ContributionsActivity
        extends     AuthenticatedActivity {

    @Inject
    SessionManager sessionManager;
    @BindView(R.id.tab_layout)
    TabLayout tabLayout;
    @BindView(R.id.pager)
    ViewPager viewPager;


    public Intent uploadServiceIntent;
    public boolean isAuthCookieAcquired = false;

    public ContributionsActivityPagerAdapter contributionsActivityPagerAdapter;
    private final int CONTRIBUTIONS_TAB_POSITION = 0;
    private final int NEARBY_TAB_POSITION = 1;

    public ContributionsFragment contributionsFragment;
    private NearbyFragment nearbyFragment;
    public boolean isContributionsFragmentVisible = true; // False means nearby fragment is visible

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contributions);
        ButterKnife.bind(this);

        requestAuthToken();
        initDrawer();
        setTitle(getString(R.string.navigation_item_home)); // Should I create a new string variable with another name instead?

        // Add tabs and fragments after Auth cookie received
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
        /*if (contributionsFragment != null) {
            contributionsFragment.onAuthCookieAcquired(uploadServiceIntent);
        }*/
    }

    private void addTabsAndFragments() {
        contributionsActivityPagerAdapter = new ContributionsActivityPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(contributionsActivityPagerAdapter);

        tabLayout.addTab(tabLayout.newTab().setText(getResources().getString(R.string.contributions_fragment)));
        tabLayout.addTab(tabLayout.newTab().setText(getResources().getString(R.string.nearby_fragment)));

        //contributionsFragment = ((NewContributionsFragment)contributionsActivityPagerAdapter.getItem(CONTRIBUTIONS_TAB_POSITION));

        if (uploadServiceIntent != null) { // If auth cookie already acquired
            // TODO Neslihan ((ContributionsFragment)contributionsActivityPagerAdapter.getItem(CONTRIBUTIONS_TAB_POSITION)).onAuthCookieAcquired(uploadServiceIntent);
        }
        //nearbyFragment = ((NewNearbyFragment)contributionsActivityPagerAdapter.getItem(NEARBY_TAB_POSITION));

        setTabAndViewPagerSynchronisation();
    }

    @Override
    protected void onAuthFailure() {

    }

    public class ContributionsActivityPagerAdapter extends FragmentPagerAdapter {
        FragmentManager fragmentManager;
        private boolean isContributionsListFragment = true; // to know what to put in first tab, Contributions of Media Details
        public ContributionsFragment contributionsFragment;
        public NearbyFragment nearbyFragment;

        public ContributionsActivityPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
            this.fragmentManager = fragmentManager;
        }

        @Override
        public int getCount() {
            return 2;
        }

        /*
         * Do not use getItem method to access fragments on pager adapter. User reference vairables
         * instead.
         * */
        @Override
        public Fragment getItem(int position) {
            switch (position){
                case 0:
                    ContributionsFragment retainedContributionsFragment = getContributionsFragment(0);
                    if (retainedContributionsFragment != null) {

                        /**
                         * ContributionsFragment is parent of ContributionsListFragment and
                         * MediaDetailsFragment. If below decides which child will be visible.
                         */
                        if (isContributionsListFragment) {
                            // TODO: Neslihan retainedContributionsFragment.setContributionsListFragment();
                        } else {
                            // TODO: Neslihan retainedContributionsFragment.setMediaDetailPagerFragment();
                        }
                        contributionsFragment = retainedContributionsFragment;
                        return retainedContributionsFragment;
                    }

                    // If we reach here, retainedContributionsFragment is null
                    contributionsFragment = new ContributionsFragment();
                    String contributionsFragmentTag = makeFragmentName(R.id.pager, 0);
                    fragmentManager.beginTransaction()
                            .replace(R.id.pager, contributionsFragment, contributionsFragmentTag)
                            .addToBackStack(contributionsFragmentTag)
                            .commit();

                    return contributionsFragment;
                case 1:
                    if (getNearbyFragment(1) != null) {
                        return getNearbyFragment(1);
                    }
                    return new NearbyFragment();// nearby places needs photo
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

}
