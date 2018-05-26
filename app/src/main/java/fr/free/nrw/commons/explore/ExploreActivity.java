package fr.free.nrw.commons.explore;

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
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.category.CategoryImagesListFragment;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.theme.NavigationBaseActivity;

/**
 * This activity displays pictures of a multiple categories using viewpager, tabs.
 * This activity is currently being used to display a list of featured images, picture of the day in tabs.
 * They are nothing but another category on wikimedia commons.
 */

public class ExploreActivity extends NavigationBaseActivity implements MediaDetailPagerFragment.MediaDetailProvider,
                    AdapterView.OnItemClickListener{

    private static final String FEATURED_IMAGES_CATEGORY = "Category:Featured_pictures_on_Wikimedia_Commons";
    private static final String POTD_CATEGORY = "Category:Pictures_of_the_day_(2018)";
    private FragmentManager supportFragmentManager;
    private CategoryImagesListFragment featuredImagesFragment;
    private CategoryImagesListFragment pictureOfTheDayFragment;
    private MediaDetailPagerFragment mediaDetails;
    @BindView(R.id.tabLayout)
    TabLayout tabLayout;
    @BindView(R.id.viewPager)
    ViewPager viewPager;
    ViewPagerAdapter viewPagerAdapter;
    @BindView(R.id.fragmentContainer)
    FrameLayout fragmentContainer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);
        ButterKnife.bind(this);

        // Activity can call methods in the fragment by acquiring a
        // reference to the Fragment from FragmentManager, using findFragmentById()
        supportFragmentManager = getSupportFragmentManager();
        initDrawer();
        setTitle(R.string.title_activity_explore);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        if (savedInstanceState != null) {
            mediaDetails = (MediaDetailPagerFragment) supportFragmentManager
                    .findFragmentById(R.id.fragmentContainer);

        }
        setTabs();
    }

    public void setTabs() {
        List<Fragment> fragmentList = new ArrayList<>();
        List<String> titleList = new ArrayList<>();

        pictureOfTheDayFragment = new CategoryImagesListFragment();
        Bundle pictureOfTheDayArguments = new Bundle();
        pictureOfTheDayArguments.putString("categoryName", POTD_CATEGORY);
        pictureOfTheDayFragment.setArguments(pictureOfTheDayArguments);

        featuredImagesFragment = new CategoryImagesListFragment();
        Bundle arguments = new Bundle();
        arguments.putString("categoryName", FEATURED_IMAGES_CATEGORY);
        featuredImagesFragment.setArguments(arguments);
        fragmentList.add(featuredImagesFragment);
        titleList.add(getResources().getString(R.string.title_featured_images));
        fragmentList.add(pictureOfTheDayFragment);
        titleList.add(getResources().getString(R.string.title_picture_of_the_day));

        viewPagerAdapter.setTabData(fragmentList, titleList);
        viewPagerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        tabLayout.setVisibility(View.VISIBLE);
        viewPager.setVisibility(View.VISIBLE);
        fragmentContainer.setVisibility(View.GONE);
        super.onBackPressed();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//        Toast.makeText(this,adapterView+"",Toast.LENGTH_SHORT).show();
        tabLayout.setVisibility(View.GONE);
        viewPager.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        if (mediaDetails == null || !mediaDetails.isVisible()) {
            // set isFeaturedImage true for featured images, to include author field on media detail
            mediaDetails = new MediaDetailPagerFragment(false, true);
            FragmentManager supportFragmentManager = getSupportFragmentManager();
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, mediaDetails)
                    .addToBackStack(null)
                    .commit();
            supportFragmentManager.executePendingTransactions();
        }
        mediaDetails.showImage(i);
    }

    @Override
    public Media getMediaAtPosition(int i) {
        if (tabLayout.getSelectedTabPosition()==0){
            if (featuredImagesFragment.getAdapter() == null) {
                // not yet ready to return data
                return null;
            } else {
                return (Media) featuredImagesFragment.getAdapter().getItem(i);
            }
        }else {
            if (pictureOfTheDayFragment.getAdapter() == null) {
                // not yet ready to return data
                return null;
            } else {
                return (Media) pictureOfTheDayFragment.getAdapter().getItem(i);
            }
        }
    }

    @Override
    public int getTotalMediaCount() {
        if (tabLayout.getSelectedTabPosition()==0) {
            if (featuredImagesFragment.getAdapter() == null) {
                return 0;
            }
            return featuredImagesFragment.getAdapter().getCount();
        }else {
            if (pictureOfTheDayFragment.getAdapter() == null) {
                return 0;
            }
            return pictureOfTheDayFragment.getAdapter().getCount();
        }
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
        inflater.inflate(R.menu.menu_explore, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_search:
                    // Start Search Activity
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
