package fr.free.nrw.commons.contributions;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.auth.SessionManager;
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

    //public ContributionsActivityPagerAdapter contributionsActivityPagerAdapter;
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

        //prepareForContributions();
        //prepareForNearby();
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
            ((NewContributionsFragment)contributionsActivityPagerAdapter.getItem(CONTRIBUTIONS_TAB_POSITION)).onAuthCookieAcquired(uploadServiceIntent);
        }
        //nearbyFragment = ((NewNearbyFragment)contributionsActivityPagerAdapter.getItem(NEARBY_TAB_POSITION));

        setTabAndViewPagerSynchronisation();
    }

    @Override
    protected void onAuthFailure() {

    }

}
