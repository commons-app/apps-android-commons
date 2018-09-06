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

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contributions);
        ButterKnife.bind(this);

        requestAuthToken();
        initDrawer();
        setTitle(getString(R.string.navigation_item_home)); // Should I create a new string with another name instead?

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

        //addTabsAndFragments();
        isAuthCookieAcquired = true;
        /*if (contributionsFragment != null) {
            contributionsFragment.onAuthCookieAcquired(uploadServiceIntent);
        }*/
    }

    @Override
    protected void onAuthFailure() {

    }

}
