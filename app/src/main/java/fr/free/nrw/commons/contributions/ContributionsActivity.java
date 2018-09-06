package fr.free.nrw.commons.contributions;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.auth.SessionManager;

public  class       ContributionsActivity
        extends     AuthenticatedActivity {

    @Inject
    SessionManager sessionManager;

    @BindView(R.id.tab_layout)
    TabLayout tabLayout;
    @BindView(R.id.pager)
    ViewPager viewPager;

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

    }

    @Override
    protected void onAuthFailure() {

    }

}
