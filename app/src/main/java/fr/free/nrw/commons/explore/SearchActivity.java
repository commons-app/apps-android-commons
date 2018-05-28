package fr.free.nrw.commons.explore;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.explore.images.SearchImageFragment;
import fr.free.nrw.commons.explore.images.SearchImageItem;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Represents search screen of this app
 */

public class SearchActivity extends NavigationBaseActivity implements MediaDetailPagerFragment.MediaDetailProvider{

    @BindView(R.id.toolbar_search) Toolbar toolbar;
    @BindView(R.id.searchBox) EditText etSearchKeyword;

    private SearchImageFragment searchImageFragment;
    private FragmentManager supportFragmentManager;
    private MediaDetailPagerFragment mediaDetails;
    SearchImageItem searchImageItem;
    private String query;

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
        RxTextView.textChanges(etSearchKeyword)
                .takeUntil(RxView.detaches(etSearchKeyword))
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( query -> {
                        //update image list
                            if (!TextUtils.isEmpty(query)) {
                                this.query = query.toString();
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

    public void onSearchImageClicked(SearchImageItem searchImageItem, int index) {
        this.searchImageItem = searchImageItem;
        ViewUtil.hideKeyboard(this.findViewById(R.id.searchBox));
        toolbar.setVisibility(View.GONE);
        setToolbarVisibility(true);
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
        if (getSupportFragmentManager().getBackStackEntryCount()==1){
            if (!TextUtils.isEmpty(query)) {
                searchImageFragment.updateImageList(query);
            }
            toolbar.setVisibility(View.VISIBLE);
            setToolbarVisibility(false);
        }else {
            toolbar.setVisibility(View.GONE);
            setToolbarVisibility(true);
        }
        super.onBackPressed();
    }

}
