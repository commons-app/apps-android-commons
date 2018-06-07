package fr.free.nrw.commons.explore;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.SearchView;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxSearchView;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.explore.images.SearchImageFragment;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Represents search screen of this app
 */

public class SearchActivity extends NavigationBaseActivity implements MediaDetailPagerFragment.MediaDetailProvider{

    @BindView(R.id.toolbar_search) Toolbar toolbar;
    @BindView(R.id.searchBox)
    SearchView searchView;

    private SearchImageFragment searchImageFragment;
    private FragmentManager supportFragmentManager;
    private MediaDetailPagerFragment mediaDetails;
    private String str_query;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);
        initDrawer();
        setTitle(getString(R.string.title_activity_search));
        toolbar.setNavigationOnClickListener(v->onBackPressed());
        supportFragmentManager = getSupportFragmentManager();
        setBrowseImagesFragment();
        searchView.setQueryHint(getString(R.string.search_commons));
        searchView.onActionViewExpanded();
        searchView.clearFocus();
        RxSearchView.queryTextChanges(searchView)
                .takeUntil(RxView.detaches(searchView))
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( query -> {
                            //update image list
                        if (!TextUtils.isEmpty(query)) {
                            str_query = query.toString();
                            searchImageFragment.updateImageList(query.toString());
                        }else {

                            // open search history fragment
                        }
                        }
                );
    }


    private void setBrowseImagesFragment() {
        searchImageFragment = new SearchImageFragment();
        FragmentTransaction transaction = supportFragmentManager.beginTransaction();
        transaction.add(R.id.fragmentContainer, searchImageFragment).commit();
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
        setNavigationBaseToolbarVisibility(true);
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
        mediaDetails.showImage(index);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1){
            // back to search so show search toolbar and hide navigation toolbar
            toolbar.setVisibility(View.VISIBLE);
            setNavigationBaseToolbarVisibility(false);
            if (!TextUtils.isEmpty(str_query)) {
                searchImageFragment.updateImageList(str_query);
            }
        }else {
            toolbar.setVisibility(View.GONE);
            setNavigationBaseToolbarVisibility(true);
        }
        super.onBackPressed();
    }

}
