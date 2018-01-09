package fr.free.nrw.commons.featured;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.contributions.ContributionsListFragment;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;

/**
 * Created by root on 09.01.2018.
 */

public class FeaturedImagesActivity
        extends AuthenticatedActivity
        implements FragmentManager.OnBackStackChangedListener {

    private FeaturedImagesListFragment featuredImagesList;
    private MediaDetailPagerFragment mediaDetails;

    @Override
    protected void onAuthCookieAcquired(String authCookie) {

    }

    @Override
    protected void onAuthFailure() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_featured_images);
        ButterKnife.bind(this);

        // Activity can call methods in the fragment by acquiring a
        // reference to the Fragment from FragmentManager, using findFragmentById()
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        featuredImagesList = (FeaturedImagesListFragment)supportFragmentManager
                .findFragmentById(R.id.featuedListFragment);

        supportFragmentManager.addOnBackStackChangedListener(this);
        if (savedInstanceState != null) {
            mediaDetails = (MediaDetailPagerFragment)supportFragmentManager
                    .findFragmentById(R.id.featuredFragmentContainer);

        }
        requestAuthToken();
        initDrawer();
        setTitle(getString(R.string.title_activity_featured_images));
    }

    @Override
    public void onBackStackChanged() {

    }
}
