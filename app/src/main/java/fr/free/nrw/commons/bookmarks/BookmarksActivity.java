package fr.free.nrw.commons.bookmarks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentManager;

import javax.inject.Inject;

import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionController;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.theme.NavigationBaseActivity;

public class BookmarksActivity extends NavigationBaseActivity
        implements FragmentManager.OnBackStackChangedListener {

    private FragmentManager supportFragmentManager;

    @Inject
    ContributionController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);
        ButterKnife.bind(this);

        // Activity can call methods in the fragment by acquiring a
        // reference to the Fragment from FragmentManager, using findFragmentById()
        supportFragmentManager = getSupportFragmentManager();
        supportFragmentManager.addOnBackStackChangedListener(this);
        initDrawer();
        initViews();
    }

    private void initViews() {
        BookmarksFragment fragment = new BookmarksFragment();
        supportFragmentManager
                .beginTransaction()
                .add(R.id.bookmarksFragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Consumers should be simply using this method to use this activity.
     * @param context A Context of the application package implementing this class.
     */
    public static void startYourself(Context context) {
        Intent intent = new Intent(context, BookmarksActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    @Override
    public void onBackStackChanged() {
        if (supportFragmentManager.getBackStackEntryCount() == 0) {
            // The activity has the focus
            initDrawer();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        controller.handleActivityResult(this, requestCode, resultCode, data);
    }
}
