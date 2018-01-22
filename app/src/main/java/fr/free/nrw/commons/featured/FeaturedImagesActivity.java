package fr.free.nrw.commons.featured;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.AdapterView;

import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;

/**
 * This activity displays pic of the days of last xx days
 */

public class FeaturedImagesActivity
        extends AuthenticatedActivity
        implements FragmentManager.OnBackStackChangedListener,
                    MediaDetailPagerFragment.MediaDetailProvider,
                    AdapterView.OnItemClickListener{

    private FeaturedImagesListFragment featuredImagesListFragment;
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
        featuredImagesListFragment = (FeaturedImagesListFragment)supportFragmentManager
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

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (mediaDetails == null || !mediaDetails.isVisible()) {
            // set isFeaturedImage true for featured images, to include author field on media detail
            mediaDetails = new MediaDetailPagerFragment(false, true);
            FragmentManager supportFragmentManager = getSupportFragmentManager();
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.featuredFragmentContainer, mediaDetails)
                    .addToBackStack(null)
                    .commit();
            supportFragmentManager.executePendingTransactions();
        }
        mediaDetails.showImage(i);
    }

    @Override
    public Media getMediaAtPosition(int i) {
        if (featuredImagesListFragment.getAdapter() == null) {
            // not yet ready to return data
            return null;
        } else {
            return ((FeaturedImage)featuredImagesListFragment.getAdapter().getItem(i)).getImage();
        }
    }

    @Override
    public int getTotalMediaCount() {
        if (featuredImagesListFragment.getAdapter() == null) {
            return 0;
        }
        return featuredImagesListFragment.getAdapter().getCount();
    }

    @Override
    public void notifyDatasetChanged() {

    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }
}
