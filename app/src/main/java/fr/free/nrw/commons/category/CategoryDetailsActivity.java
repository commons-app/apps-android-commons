package fr.free.nrw.commons.category;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.PageTitle;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.theme.NavigationBaseActivity;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * This activity displays details of a particular category
 * Its generic and simply takes the name of category name in its start intent to load all images, subcategories in
 * a particular category on wikimedia commons.
 */

public class CategoryDetailsActivity extends NavigationBaseActivity
        implements MediaDetailPagerFragment.MediaDetailProvider,
                    AdapterView.OnItemClickListener{


    private FragmentManager supportFragmentManager;
    private CategoryImagesListFragment categoryImagesListFragment;
    private MediaDetailPagerFragment mediaDetails;
    private String categoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_images);
        ButterKnife.bind(this);
        supportFragmentManager = getSupportFragmentManager();
        setCategoryImagesFragment();
        setPageTitle();
        initDrawer();
        initBackButton();
    }

    /**
     * Gets the categoryName from the intent and initializes the fragment for showing images of that category
     */
    private void setCategoryImagesFragment() {
        categoryImagesListFragment = new CategoryImagesListFragment();
        categoryName = getIntent().getStringExtra("categoryName");
        if (getIntent() != null && categoryName != null) {
            Bundle arguments = new Bundle();
            arguments.putString("categoryName", categoryName);
            categoryImagesListFragment.setArguments(arguments);
            FragmentTransaction transaction = supportFragmentManager.beginTransaction();
            transaction.replace(R.id.fragmentContainer, categoryImagesListFragment)
            .commit();
        }
    }

    /**
     * Gets the passed categoryName from the intents and displays it as the page title
     */
    private void setPageTitle() {
        if (getIntent() != null && getIntent().getStringExtra("categoryName") != null) {
            setTitle(getIntent().getStringExtra("categoryName"));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (mediaDetails == null || !mediaDetails.isVisible()) {
            mediaDetails = new MediaDetailPagerFragment(false, true);
            FragmentManager supportFragmentManager = getSupportFragmentManager();
            supportFragmentManager
                    .beginTransaction()
                    .hide(supportFragmentManager.getFragments().get(supportFragmentManager.getBackStackEntryCount()))
                    .add(R.id.fragmentContainer, mediaDetails)
                    .addToBackStack(null)
                    .commit();
            supportFragmentManager.executePendingTransactions();
        }
        mediaDetails.showImage(i);
        initBackButton();
    }


    /**
     * Consumers should be simply using this method to use this activity.
     * @param context
     * @param categoryName Name of the category for displaying its details
     */
    public static void startYourself(Context context, String categoryName) {
        Intent intent = new Intent(context, CategoryDetailsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("categoryName", categoryName);
        context.startActivity(intent);
    }

    @Override
    public Media getMediaAtPosition(int i) {
        if (categoryImagesListFragment.getAdapter() == null) {
            // not yet ready to return data
            return null;
        } else {
            return (Media) categoryImagesListFragment.getAdapter().getItem(i);
        }
    }

    @Override
    public int getTotalMediaCount() {
        if (categoryImagesListFragment.getAdapter() == null) {
            return 0;
        }
        return categoryImagesListFragment.getAdapter().getCount();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.fragment_category_detail, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_browser_current_category:
                Intent viewIntent = new Intent();
                viewIntent.setAction(Intent.ACTION_VIEW);
                viewIntent.setData(new PageTitle(categoryName).getCanonicalUri());
                //check if web browser available
                if (viewIntent.resolveActivity(this.getPackageManager()) != null) {
                    startActivity(viewIntent);
                } else {
                    Toast toast = Toast.makeText(this, getString(R.string.no_web_browser), LENGTH_SHORT);
                    toast.show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
