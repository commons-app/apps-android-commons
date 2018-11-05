package fr.free.nrw.commons.explore.categories;


import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pedrogomez.renderers.RVRendererAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.category.CategoryDetailsActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.recentsearches.RecentSearch;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Displays the category search screen.
 */

public class SearchCategoryFragment extends CommonsDaggerSupportFragment {

    private static int TIMEOUT_SECONDS = 15;

    @BindView(R.id.imagesListBox)
    RecyclerView categoriesRecyclerView;
    @BindView(R.id.imageSearchInProgress)
    ProgressBar progressBar;
    @BindView(R.id.imagesNotFound)
    TextView categoriesNotFoundView;
    String query;

    @Inject RecentSearchesDao recentSearchesDao;
    @Inject MediaWikiApi mwApi;
    @Inject @Named("default_preferences") SharedPreferences prefs;

    private RVRendererAdapter<String> categoriesAdapter;
    private List<String> queryList = new ArrayList<>();

    private final SearchCategoriesAdapterFactory adapterFactory = new SearchCategoriesAdapterFactory(item -> {
        // Called on Click of a individual category Item
        // Open Category Details activity
        CategoryDetailsActivity.startYourself(getContext(), item);
        saveQuery(query);
    });

    /**
     * This method saves Search Query in the Recent Searches Database.
     * @param query
     */
    private void saveQuery(String query) {
        RecentSearch recentSearch = recentSearchesDao.find(query);

        // Newly searched query...
        if (recentSearch == null) {
            recentSearch = new RecentSearch(null, query, new Date());
        }
        else {
            recentSearch.setLastSearched(new Date());
        }
        recentSearchesDao.save(recentSearch);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_browse_image, container, false);
        ButterKnife.bind(this, rootView);
        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        else{
            categoriesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        }
        ArrayList<String> items = new ArrayList<>();
        categoriesAdapter = adapterFactory.create(items);
        categoriesRecyclerView.setAdapter(categoriesAdapter);
        categoriesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // check if end of recycler view is reached, if yes then add more results to existing results
                if (!recyclerView.canScrollVertically(1)) {
                    addCategoriesToList(query);
                }
            }
        });
        return rootView;
    }

    /**
     * Checks for internet connection and then initializes the recycler view with 25 categories of the searched query
     * Clearing categoryAdapter every time new keyword is searched so that user can see only new results
     */
    public void updateCategoryList(String query) {
        this.query = query;
        categoriesNotFoundView.setVisibility(GONE);
        if (!NetworkUtils.isInternetConnectionEstablished(getContext())) {
            handleNoInternet();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        queryList.clear();
        categoriesAdapter.clear();
        Observable.fromCallable(() -> mwApi.searchCategory(query,queryList.size()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .subscribe(this::handleSuccess, this::handleError);
    }


    /**
     * Adds more results to existing search results
     */
    public void addCategoriesToList(String query) {
        this.query = query;
        progressBar.setVisibility(View.VISIBLE);
        Observable.fromCallable(() -> mwApi.searchCategory(query,queryList.size()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .subscribe(this::handlePaginationSuccess, this::handleError);
    }

    /**
     * Handles the success scenario
     * it initializes the recycler view by adding items to the adapter
     * @param mediaList
     */
    private void handlePaginationSuccess(List<String> mediaList) {
        queryList.addAll(mediaList);
        progressBar.setVisibility(View.GONE);
        categoriesAdapter.addAll(mediaList);
        categoriesAdapter.notifyDataSetChanged();
    }



    /**
     * Handles the success scenario
     * it initializes the recycler view by adding items to the adapter
     * @param mediaList
     */
    private void handleSuccess(List<String> mediaList) {
        queryList = mediaList;
        if (mediaList == null || mediaList.isEmpty()) {
            initErrorView();
        }
        else {

            progressBar.setVisibility(View.GONE);
            categoriesAdapter.addAll(mediaList);
            categoriesAdapter.notifyDataSetChanged();

            // check if user is waiting for 5 seconds if yes then save search query to history.
            Handler handler = new Handler();
            handler.postDelayed(() -> saveQuery(query), 5000);
        }
    }

    /**
     * Logs and handles API error scenario
     * @param throwable
     */
    private void handleError(Throwable throwable) {
        Timber.e(throwable, "Error occurred while loading queried categories");
        try {
            initErrorView();
            ViewUtil.showSnackbar(categoriesRecyclerView, R.string.error_loading_categories);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Handles the UI updates for a error scenario
     */
    private void initErrorView() {
        progressBar.setVisibility(GONE);
        categoriesNotFoundView.setVisibility(VISIBLE);
        categoriesNotFoundView.setText(getString(R.string.categories_not_found, query));
    }

    /**
     * Handles the UI updates for no internet scenario
     */
    private void handleNoInternet() {
        progressBar.setVisibility(GONE);
        ViewUtil.showSnackbar(categoriesRecyclerView, R.string.no_internet);
    }
}
