package fr.free.nrw.commons.explore.categories;


import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import fr.free.nrw.commons.category.CategoryClient;
import fr.free.nrw.commons.category.CategoryDetailsActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.recentsearches.RecentSearch;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
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
    @BindView(R.id.bottomProgressBar)
    ProgressBar bottomProgressBar;
    boolean isLoadingCategories;

    @Inject RecentSearchesDao recentSearchesDao;
    @Inject CategoryClient categoryClient;

    @Inject
    @Named("default_preferences")
    JsonKvStore basicKvStore;

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
        bottomProgressBar.setVisibility(View.VISIBLE);
        progressBar.setVisibility(GONE);
        queryList.clear();
        categoriesAdapter.clear();
        compositeDisposable.add(categoryClient.searchCategories(query,25)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .doOnSubscribe(disposable -> saveQuery(query))
                .collect(ArrayList<String>::new, ArrayList::add)
                .subscribe(this::handleSuccess, this::handleError));
    }


    /**
     * Adds 25 more results to existing search results
     */
    public void addCategoriesToList(String query) {
        if(isLoadingCategories) return;
        isLoadingCategories=true;
        this.query = query;
        bottomProgressBar.setVisibility(View.VISIBLE);
        progressBar.setVisibility(GONE);
        compositeDisposable.add(categoryClient.searchCategories(query,25, queryList.size())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .collect(ArrayList<String>::new, ArrayList::add)
                .subscribe(this::handlePaginationSuccess, this::handleError));
    }

    /**
     * Handles the success scenario
     * it initializes the recycler view by adding items to the adapter
     */
    private void handlePaginationSuccess(List<String> mediaList) {
        queryList.addAll(mediaList);
        progressBar.setVisibility(View.GONE);
        bottomProgressBar.setVisibility(GONE);
        categoriesAdapter.addAll(mediaList);
        categoriesAdapter.notifyDataSetChanged();
        isLoadingCategories=false;
    }



    /**
     * Handles the success scenario
     * it initializes the recycler view by adding items to the adapter
     */
    private void handleSuccess(List<String> mediaList) {
        queryList = mediaList;
        if (mediaList == null || mediaList.isEmpty()) {
            initErrorView();
        }
        else {

            bottomProgressBar.setVisibility(View.GONE);
            progressBar.setVisibility(GONE);
            categoriesAdapter.addAll(mediaList);
            categoriesAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Logs and handles API error scenario
     */
    private void handleError(Throwable throwable) {
        Timber.e(throwable, "Error occurred while loading queried categories");
        try {
            initErrorView();
            ViewUtil.showShortSnackbar(categoriesRecyclerView, R.string.error_loading_categories);
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
        categoriesNotFoundView.setText(getString(R.string.categories_not_found));
    }

    /**
     * Handles the UI updates for no internet scenario
     */
    private void handleNoInternet() {
        progressBar.setVisibility(GONE);
        ViewUtil.showShortSnackbar(categoriesRecyclerView, R.string.no_internet);
    }
}
