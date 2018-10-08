package fr.free.nrw.commons.bookmarks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;

public class BookmarksActivity extends AuthenticatedActivity
        implements FragmentManager.OnBackStackChangedListener {

    private FragmentManager supportFragmentManager;
    @BindView(R.id.viewPagerBookmarks)
    ViewPager viewPager;
    @BindView(R.id.tabLayoutBookmarks)
    TabLayout tabLayout;

    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        // noop
    }

    @Override
    protected void onAuthFailure() {
        // noop
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);
        ButterKnife.bind(this);

        // Activity can call methods in the fragment by acquiring a
        // reference to the Fragment from FragmentManager, using findFragmentById()
        supportFragmentManager = getSupportFragmentManager();
        supportFragmentManager.addOnBackStackChangedListener(this);
        requestAuthToken();
        initDrawer();

        tabLayout.setupWithViewPager(viewPager);
    }

    /**
     * Consumers should be simply using this method to use this activity.
     * @param context A Context of the application package implementing this class.
     */
    public static void startYourself(Context context) {
        Intent intent = new Intent(context, BookmarksActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }

    @Override
    public void onBackStackChanged() {
        // noop
    }
}
