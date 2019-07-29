package fr.free.nrw.commons.depictions;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.explore.SearchActivity;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.theme.NavigationBaseActivity;

public class DepictionsImageActivity  extends NavigationBaseActivity
        implements FragmentManager.OnBackStackChangedListener,
        MediaDetailPagerFragment.MediaDetailProvider,
        AdapterView.OnItemClickListener {

    private FragmentManager supportFragmentManager;
    private DepictionsImageListFragment depictionsImageListFragment;
    private MediaDetailPagerFragment mediaDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_images);
        ButterKnife.bind(this);

        // Activity can call methods in the fragment by acquiring a
        // reference to the Fragment from FragmentManager, using findFragmentById()
        supportFragmentManager = getSupportFragmentManager();
        setDepictsImagesFragment();
        supportFragmentManager.addOnBackStackChangedListener(this);
        initDrawer();
        setPageTitle();
    }

    /**
     * Gets the depictsName from the intent and initializes the fragment for showing images of that depiction
     */
    private void setDepictsImagesFragment() {
        depictionsImageListFragment = new DepictionsImageListFragment();
        String depictsName = getIntent().getStringExtra("depictsName");
        if (getIntent() != null && depictsName != null) {
            Bundle arguments = new Bundle();
            arguments.putString("depictsName", depictsName);
            depictionsImageListFragment.setArguments(arguments);
            FragmentTransaction transaction = supportFragmentManager.beginTransaction();
            transaction
                    .add(R.id.fragmentContainer, depictionsImageListFragment)
                    .commit();
        }
    }

    /**
     * Gets the passed title from the intents and displays it as the page title
     */
    private void setPageTitle() {
        if (getIntent() != null && getIntent().getStringExtra("title") != null) {
            setTitle(getIntent().getStringExtra("title"));
        }
    }

    /**
     * This method is called on backPressed of anyFragment in the activity.
     * We are changing the icon here from back to hamburger icon.
     */
    @Override
    public void onBackPressed() {
        initDrawer();
        super.onBackPressed();
    }

    /**
     * Consumers should be simply using this method to use this activity.
     * @param context A Context of the application package implementing this class.
     * @param title Page title
     * @param depictsName Name of the category for displaying its images
     */
    public static void startYourself(Context context, String title, String depictsName) {
        Intent intent = new Intent(context, DepictionsImageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("title", title);
        intent.putExtra("categoryName", depictsName);
        context.startActivity(intent);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mediaDetails == null || !mediaDetails.isVisible()) {
            // set isFeaturedImage true for featured images, to include author field on media detail
            mediaDetails = new MediaDetailPagerFragment(false, true);
            FragmentManager supportFragmentManager = getSupportFragmentManager();
            supportFragmentManager
                    .beginTransaction()
                    .hide(supportFragmentManager.getFragments().get(supportFragmentManager.getBackStackEntryCount()))
                    .add(R.id.fragmentContainer, mediaDetails)
                    .addToBackStack(null)
                    .commit();
            // Reason for using hide, add instead of replace is to maintain scroll position after
            // coming back to the search activity. See https://github.com/commons-app/apps-android-commons/issues/1631
            // https://stackoverflow.com/questions/11353075/how-can-i-maintain-fragment-state-when-added-to-the-back-stack/19022550#19022550            supportFragmentManager.executePendingTransactions();
        }
        mediaDetails.showImage(position);
        forceInitBackButton();
    }

    @Override
    public void onBackStackChanged() {

    }

    @Override
    public Media getMediaAtPosition(int i) {
        if (depictionsImageListFragment.getAdapter() == null) {
            // not yet ready to return data
            return null;
        } else {
            return (Media) depictionsImageListFragment.getAdapter().getItem(i);
        }
    }

    /**
     * This method handles the logic on ItemSelect in toolbar menu
     * Currently only 1 choice is available to open search page of the app
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_search:
                NavigationBaseActivity.startActivityWithFlags(this, SearchActivity.class);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This method inflates the menu in the toolbar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public int getTotalMediaCount() {
        if (depictionsImageListFragment.getAdapter() == null) {
            return 0;
        }
        return depictionsImageListFragment.getAdapter().getCount();
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
     * This method is called when viewPager has reached its end.
     * Fetches more images using search query and adds it to the gridView and viewpager adapter
     */
    public void requestMoreImages() {
        if (depictionsImageListFragment!=null){
            depictionsImageListFragment.fetchMoreImagesViewPager();
        }
    }
}
