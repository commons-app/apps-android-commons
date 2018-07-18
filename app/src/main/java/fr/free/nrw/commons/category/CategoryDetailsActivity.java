package fr.free.nrw.commons.category;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.PageTitle;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.explore.ViewPagerAdapter;
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
    private SubCategoryListFragment subCategoryListFragment;
    private SubCategoryListFragment parentCategoryListFragment;
    private MediaDetailPagerFragment mediaDetails;
    private String categoryName;
    @BindView(R.id.mediaContainer) FrameLayout mediaContainer;
    @BindView(R.id.tabLayout) TabLayout tabLayout;
    @BindView(R.id.viewPager) ViewPager viewPager;

    ViewPagerAdapter viewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_details);
        ButterKnife.bind(this);
        supportFragmentManager = getSupportFragmentManager();
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setOffscreenPageLimit(2);
        tabLayout.setupWithViewPager(viewPager);
        setTabs();
        setPageTitle();
        initDrawer();
        forceInitBackButton();
    }

    /**
     * This activity contains 3 tabs and a viewpager. This method is used to set the titles of tab,
     * Set the fragments according to the tab selected in the viewPager.
     */
    private void setTabs() {
        List<Fragment> fragmentList = new ArrayList<>();
        List<String> titleList = new ArrayList<>();
        categoryImagesListFragment = new CategoryImagesListFragment();
        subCategoryListFragment = new SubCategoryListFragment();
        parentCategoryListFragment = new SubCategoryListFragment();
        categoryName = getIntent().getStringExtra("categoryName");
        if (getIntent() != null && categoryName != null) {
            Bundle arguments = new Bundle();
            arguments.putString("categoryName", categoryName);
            arguments.putBoolean("isParentCategory", false);
            categoryImagesListFragment.setArguments(arguments);
            subCategoryListFragment.setArguments(arguments);
            Bundle parentCategoryArguments = new Bundle();
            parentCategoryArguments.putString("categoryName", categoryName);
            parentCategoryArguments.putBoolean("isParentCategory", true);
            parentCategoryListFragment.setArguments(parentCategoryArguments);
        }
        fragmentList.add(categoryImagesListFragment);
        titleList.add("MEDIA");
        fragmentList.add(subCategoryListFragment);
        titleList.add("SUBCATEGORIES");
        fragmentList.add(parentCategoryListFragment);
        titleList.add("PARENT CATEGORIES");
        viewPagerAdapter.setTabData(fragmentList, titleList);
        viewPagerAdapter.notifyDataSetChanged();

    }

    /**
     * Gets the passed categoryName from the intents and displays it as the page title
     */
    private void setPageTitle() {
        if (getIntent() != null && getIntent().getStringExtra("categoryName") != null) {
            setTitle(getIntent().getStringExtra("categoryName"));
        }
    }

    /**
     * This method is called onClick of media inside category details (CategoryImageListFragment).
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        tabLayout.setVisibility(View.GONE);
        viewPager.setVisibility(View.GONE);
        mediaContainer.setVisibility(View.VISIBLE);
        if (mediaDetails == null || !mediaDetails.isVisible()) {
            // set isFeaturedImage true for featured images, to include author field on media detail
            mediaDetails = new MediaDetailPagerFragment(false, true);
            FragmentManager supportFragmentManager = getSupportFragmentManager();
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.mediaContainer, mediaDetails)
                    .addToBackStack(null)
                    .commit();
            supportFragmentManager.executePendingTransactions();
        }
        mediaDetails.showImage(i);
        forceInitBackButton();
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

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1){
            // back to search so show search toolbar and hide navigation toolbar
            tabLayout.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.VISIBLE);
            mediaContainer.setVisibility(View.GONE);
        }
        super.onBackPressed();
    }
}
