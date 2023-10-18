package fr.free.nrw.commons.explore;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SearchView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.tabs.TabLayout;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxSearchView;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.ViewPagerAdapter;
import fr.free.nrw.commons.category.CategoryImagesCallback;
import fr.free.nrw.commons.explore.categories.search.SearchCategoryFragment;
import fr.free.nrw.commons.explore.depictions.search.SearchDepictionsFragment;
import fr.free.nrw.commons.explore.media.SearchMediaFragment;
import fr.free.nrw.commons.explore.models.RecentSearch;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesFragment;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.FragmentUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import timber.log.Timber;

/**
 * Represents search screen of this app
 */

public class SearchActivity extends BaseActivity
        implements MediaDetailPagerFragment.MediaDetailProvider, CategoryImagesCallback {

    @BindView(R.id.toolbar_search) Toolbar toolbar;
    @BindView(R.id.searchHistoryContainer) FrameLayout searchHistoryContainer;
    @BindView(R.id.mediaContainer) FrameLayout mediaContainer;
    @BindView(R.id.searchBox) SearchView searchView;
    @BindView(R.id.tab_layout) TabLayout tabLayout;
    @BindView(R.id.viewPager) ViewPager viewPager;

    @Inject
    RecentSearchesDao recentSearchesDao;

    private SearchMediaFragment searchMediaFragment;
    private SearchCategoryFragment searchCategoryFragment;
    private SearchDepictionsFragment searchDepictionsFragment;
    private RecentSearchesFragment recentSearchesFragment;
    private FragmentManager supportFragmentManager;
    private MediaDetailPagerFragment mediaDetails;
    ViewPagerAdapter viewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);
        setTitle(getString(R.string.title_activity_search));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v->onBackPressed());
        supportFragmentManager = getSupportFragmentManager();
        setSearchHistoryFragment();
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setOffscreenPageLimit(2); // Because we want all the fragments to be alive
        tabLayout.setupWithViewPager(viewPager);
        setTabs();
        searchView.setQueryHint(getString(R.string.search_commons));
        searchView.onActionViewExpanded();
        searchView.clearFocus();

    }

    /**
     * This method sets the search history fragment.
     * Search history fragment is displayed when query is empty.
     */
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
        searchMediaFragment = new SearchMediaFragment();
        searchDepictionsFragment = new SearchDepictionsFragment();
        searchCategoryFragment= new SearchCategoryFragment();
        fragmentList.add(searchMediaFragment);
        titleList.add(getResources().getString(R.string.search_tab_title_media).toUpperCase());
        fragmentList.add(searchCategoryFragment);
        titleList.add(getResources().getString(R.string.search_tab_title_categories).toUpperCase());
        fragmentList.add(searchDepictionsFragment);
        titleList.add(getResources().getString(R.string.search_tab_title_depictions).toUpperCase());

        viewPagerAdapter.setTabData(fragmentList, titleList);
        viewPagerAdapter.notifyDataSetChanged();
        compositeDisposable.add(RxSearchView.queryTextChanges(searchView)
                .takeUntil(RxView.detaches(searchView))
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSearch, Timber::e
                ));
    }

    private void handleSearch(final CharSequence query) {
        if (!TextUtils.isEmpty(query)) {
            saveRecentSearch(query.toString());
            viewPager.setVisibility(View.VISIBLE);
            tabLayout.setVisibility(View.VISIBLE);
            searchHistoryContainer.setVisibility(View.GONE);

            if (FragmentUtils.isFragmentUIActive(searchDepictionsFragment)) {
                searchDepictionsFragment.onQueryUpdated(query.toString());
            }

            if (FragmentUtils.isFragmentUIActive(searchMediaFragment)) {
                searchMediaFragment.onQueryUpdated(query.toString());
            }

            if (FragmentUtils.isFragmentUIActive(searchCategoryFragment)) {
                searchCategoryFragment.onQueryUpdated(query.toString());
            }

         }
        else {
            //Open RecentSearchesFragment
            recentSearchesFragment.updateRecentSearches();
            viewPager.setVisibility(View.GONE);
            tabLayout.setVisibility(View.GONE);
            setSearchHistoryFragment();
            searchHistoryContainer.setVisibility(View.VISIBLE);
        }
    }

    private void saveRecentSearch(@NonNull final String query) {
        final RecentSearch recentSearch = recentSearchesDao.find(query);
        // Newly searched query...
        if (recentSearch == null) {
            recentSearchesDao.save(new RecentSearch(null, query, new Date()));
        } else {
            recentSearch.setLastSearched(new Date());
            recentSearchesDao.save(recentSearch);
        }
    }

    /**
     * returns Media Object at position
     * @param i position of Media in the imagesRecyclerView adapter.
     */
    @Override
    public Media getMediaAtPosition(int i) {
        return searchMediaFragment.getMediaAtPosition(i);
    }

    /**
     * returns total number of images present in the imagesRecyclerView adapter.
     */
    @Override
    public int getTotalMediaCount() {
       return searchMediaFragment.getTotalMediaCount();
    }

    @Override
    public Integer getContributionStateAt(int position) {
        return null;
    }

    /**
     * Reload media detail fragment once media is nominated
     *
     * @param index item position that has been nominated
     */
    @Override
    public void refreshNominatedMedia(int index) {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            onBackPressed();
            onMediaClicked(index);
        }
    }

    /**
     * This method is called on success of API call for image Search.
     * The viewpager will notified that number of items have changed.
     */
    @Override
    public void viewPagerNotifyDataSetChanged() {
        if (mediaDetails!=null){
            mediaDetails.notifyDataSetChanged();
        }
    }

    /**
     * Open media detail pager fragment on click of image in search results
     * @param index item index that should be opened
     */
    @Override
    public void onMediaClicked(int index) {
        ViewUtil.hideKeyboard(this.findViewById(R.id.searchBox));
        tabLayout.setVisibility(View.GONE);
        viewPager.setVisibility(View.GONE);
        mediaContainer.setVisibility(View.VISIBLE);
        searchView.setVisibility(View.GONE);// to remove searchview when mediaDetails fragment open
        if (mediaDetails == null || !mediaDetails.isVisible()) {
            // set isFeaturedImage true for featured images, to include author field on media detail
            mediaDetails = MediaDetailPagerFragment.newInstance(false, true);
            supportFragmentManager
                    .beginTransaction()
                    .hide(supportFragmentManager.getFragments().get(supportFragmentManager.getBackStackEntryCount()))
                    .add(R.id.mediaContainer, mediaDetails)
                    .addToBackStack(null)
                    .commit();
            // Reason for using hide, add instead of replace is to maintain scroll position after
            // coming back to the search activity. See https://github.com/commons-app/apps-android-commons/issues/1631
            // https://stackoverflow.com/questions/11353075/how-can-i-maintain-fragment-state-when-added-to-the-back-stack/19022550#19022550
            supportFragmentManager.executePendingTransactions();
        }
        mediaDetails.showImage(index);
    }

    /**
     * This method is called on Screen Rotation
     */
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

    /**
     * This method is called on backPressed of anyFragment in the activity.
     * If condition is called when mediaDetailFragment is opened.
     */
    @Override
    public void onBackPressed() {
        //Remove the backstack entry that gets added when share button is clicked
        //fixing:https://github.com/commons-app/apps-android-commons/issues/2296
        if (getSupportFragmentManager().getBackStackEntryCount() == 2) {
            supportFragmentManager
                .beginTransaction()
                .remove(mediaDetails)
                .commit();
            supportFragmentManager.popBackStack();
            supportFragmentManager.executePendingTransactions();
        }
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            // back to search so show search toolbar and hide navigation toolbar
            searchView.setVisibility(View.VISIBLE);//set the searchview
            tabLayout.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.VISIBLE);
            mediaContainer.setVisibility(View.GONE);
        } else {
            toolbar.setVisibility(View.GONE);
        }
        super.onBackPressed();
    }

    /**
     * This method is called on click of a recent search to update query in SearchView.
     * @param query Recent Search Query
     */
    public void updateText(String query) {
        searchView.setQuery(query, true);
        // Clear focus of searchView now. searchView.clearFocus(); does not seem to work Check the below link for more details.
        // https://stackoverflow.com/questions/6117967/how-to-remove-focus-without-setting-focus-to-another-control/15481511
        viewPager.requestFocus();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        //Dispose the disposables when the activity is destroyed
        compositeDisposable.dispose();
    }
}
