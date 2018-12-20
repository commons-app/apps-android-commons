package fr.free.nrw.commons.bookmarks;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.AdapterView;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.theme.NavigationBaseActivity;

public class BookmarksActivity extends NavigationBaseActivity
        implements FragmentManager.OnBackStackChangedListener,
        MediaDetailPagerFragment.MediaDetailProvider,
        AdapterView.OnItemClickListener {

    private FragmentManager supportFragmentManager;
    private BookmarksPagerAdapter adapter;
    private MediaDetailPagerFragment mediaDetails;
    @BindView(R.id.viewPagerBookmarks)
    ViewPager viewPager;
    @BindView(R.id.tabLayoutBookmarks)
    TabLayout tabLayout;

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

        adapter = new BookmarksPagerAdapter(supportFragmentManager, this);
        viewPager.setAdapter(adapter);
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
        if (supportFragmentManager.getBackStackEntryCount() == 0) {
            // The activity has the focus
            adapter.requestPictureListUpdate();
            initDrawer();
        }
    }

    /**
     * This method is called onClick of media inside category details (CategoryImageListFragment).
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (mediaDetails == null || !mediaDetails.isVisible()) {
            mediaDetails = new MediaDetailPagerFragment(false, true);
            supportFragmentManager
                    .beginTransaction()
                    .hide(supportFragmentManager.getFragments().get(supportFragmentManager.getBackStackEntryCount()))
                    .add(R.id.fragmentContainer, mediaDetails)
                    .addToBackStack(null)
                    .commit();
            supportFragmentManager.executePendingTransactions();
        }
        mediaDetails.showImage(i);
        forceInitBackButton();
    }

    /**
     * This method is called on success of API call for featured Images.
     * The viewpager will notified that number of items have changed.
     */
    public void viewPagerNotifyDataSetChanged() {
        if (mediaDetails!=null){
            mediaDetails.notifyDataSetChanged();
        }
    }

    /**
     * This method is called mediaDetailPagerFragment. It returns the Media Object at that Index
     * @param i It is the index of which media object is to be returned which is same as
     *          current index of viewPager.
     * @return Media Object
     */
    @Override
    public Media getMediaAtPosition(int i) {
        if (adapter.getMediaAdapter() == null) {
            // not yet ready to return data
            return null;
        } else {
            return (Media) adapter.getMediaAdapter().getItem(i);
        }
    }

    /**
     * This method is called on from getCount of MediaDetailPagerFragment
     * The viewpager will contain same number of media items as that of media elements in adapter.
     * @return Total Media count in the adapter
     */
    @Override
    public int getTotalMediaCount() {
        if (adapter.getMediaAdapter() == null) {
            return 0;
        }
        return adapter.getMediaAdapter().getCount();
    }

    /**
     * This method is never called but it was in MediaDetailProvider Interface
     * so it needs to be overrided.
     */
    @Override
    public void notifyDatasetChanged() {

    }

    /**
     * This method is never called but it was in MediaDetailProvider Interface
     * so it needs to be overrided.
     */
    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    /**
     * This method is never called but it was in MediaDetailProvider Interface
     * so it needs to be overrided.
     */
    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }
}
