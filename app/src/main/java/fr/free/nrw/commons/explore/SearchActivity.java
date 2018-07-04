package fr.free.nrw.commons.explore;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SearchView;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxSearchView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.explore.categories.SearchCategoryFragment;
import fr.free.nrw.commons.explore.images.SearchImageFragment;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesFragment;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Represents search screen of this app
 */

public class SearchActivity extends NavigationBaseActivity implements MediaDetailPagerFragment.MediaDetailProvider{

    @BindView(R.id.toolbar_search) Toolbar toolbar;
    @BindView(R.id.searchHistoryContainer) FrameLayout searchHistoryContainer;
    @BindView(R.id.mediaContainer) FrameLayout mediaContainer;
    @BindView(R.id.searchBox) SearchView searchView;
    @BindView(R.id.tabLayout) TabLayout tabLayout;
    @BindView(R.id.viewPager) ViewPager viewPager;

    private SearchImageFragment searchImageFragment;
    private SearchCategoryFragment searchCategoryFragment;
    private RecentSearchesFragment recentSearchesFragment;
    private FragmentManager supportFragmentManager;
    private MediaDetailPagerFragment mediaDetails;
    private String query;
    ViewPagerAdapter viewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);
        initDrawer();
        setTitle(getString(R.string.title_activity_search));
        toolbar.setNavigationOnClickListener(v->onBackPressed());
        supportFragmentManager = getSupportFragmentManager();
        setSearchHistoryFragment();
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        setTabs();
        searchView.setQueryHint(getString(R.string.search_commons));
        searchView.onActionViewExpanded();
        searchView.clearFocus();

    }

    private void setSearchHistoryFragment() {
        recentSearchesFragment = new RecentSearchesFragment();
        FragmentTransaction transaction = supportFragmentManager.beginTransaction();
        transaction.add(R.id.searchHistoryContainer, recentSearchesFragment).commit();
    }

    /**
     * Sets the titles in the tabLayout and fragments in the viewPager
     */
    public void setTabs() {
        List<Fragment> fragmentList = new ArrayList<>();
        List<String> titleList = new ArrayList<>();
        searchImageFragment = new SearchImageFragment();
        searchCategoryFragment= new SearchCategoryFragment();
        fragmentList.add(searchImageFragment);
        titleList.add("MEDIA");
        fragmentList.add(searchCategoryFragment);
        titleList.add("CATEGORIES");

        viewPagerAdapter.setTabData(fragmentList, titleList);
        viewPagerAdapter.notifyDataSetChanged();
        RxSearchView.queryTextChanges(searchView)
                .takeUntil(RxView.detaches(searchView))
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( query -> {
                            //update image list
                            if (!TextUtils.isEmpty(query)) {
                                viewPager.setVisibility(View.VISIBLE);
                                tabLayout.setVisibility(View.VISIBLE);
                                searchHistoryContainer.setVisibility(View.GONE);
                                this.query = query.toString();
                                searchImageFragment.updateImageList(query.toString());
                                searchCategoryFragment.updateCategoryList(query.toString());
                            }else {
                                viewPager.setVisibility(View.GONE);
                                tabLayout.setVisibility(View.GONE);
                                searchHistoryContainer.setVisibility(View.VISIBLE);
                                recentSearchesFragment.updateRecentSearches();
                                // open search history fragment
                            }
                        }
                );
    }

    @Override
    public Media getMediaAtPosition(int i) {
        return searchImageFragment.getImageAtPosition(i);
    }

    @Override
    public int getTotalMediaCount() {
       return searchImageFragment.getTotalImagesCount();
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

    /**
     * Open media detail pager fragment on click of image in search results
     * @param index item index that should be opened
     */
    public void onSearchImageClicked(int index) {
        ViewUtil.hideKeyboard(this.findViewById(R.id.searchBox));
        toolbar.setVisibility(View.GONE);
        tabLayout.setVisibility(View.GONE);
        viewPager.setVisibility(View.GONE);
        mediaContainer.setVisibility(View.VISIBLE);
        setNavigationBaseToolbarVisibility(true);
        if (mediaDetails == null || !mediaDetails.isVisible()) {
            // set isFeaturedImage true for featured images, to include author field on media detail
            mediaDetails = new MediaDetailPagerFragment(false, true);
            FragmentManager supportFragmentManager = getSupportFragmentManager();
            supportFragmentManager
                    .beginTransaction()
                    .hide(supportFragmentManager.getFragments().get(supportFragmentManager.getBackStackEntryCount()))
                    .add(R.id.mediaContainer, mediaDetails)
                    .addToBackStack(null)
                    .commit();
            supportFragmentManager.executePendingTransactions();
        }
        mediaDetails.showImage(index);
        forceInitBackButton();
    }

    @Override
    protected void onResume() {
        if (supportFragmentManager.getBackStackEntryCount()==1){
            //FIXME: Temporary fix for screen rotation inside media details. If we don't call onBackPressed then fragment stack is increasing every time.
            //FIXME: Similar issue like this https://github.com/commons-app/apps-android-commons/issues/894
            // This is called on screen rotation when user is inside media details. Ideally it should show Media Details but since we are not saving the state now. We are throwing the user to search screen otherwise the app was crashing.
            // 
            onBackPressed();
        }
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1){
            // back to search so show search toolbar and hide navigation toolbar
            toolbar.setVisibility(View.VISIBLE);
            tabLayout.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.VISIBLE);
            mediaContainer.setVisibility(View.GONE);
            setNavigationBaseToolbarVisibility(false);
        }else {
            toolbar.setVisibility(View.GONE);
            setNavigationBaseToolbarVisibility(true);
        }
        super.onBackPressed();
    }

    public void updateText(String query) {
        searchView.setQuery(query, true);
        // Clear focus of searchView now. searchView.clearFocus(); does not seem to work Check the below link for more details.
        // https://stackoverflow.com/questions/6117967/how-to-remove-focus-without-setting-focus-to-another-control/15481511
        viewPager.requestFocus();
    }
}
