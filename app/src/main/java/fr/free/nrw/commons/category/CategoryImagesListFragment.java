package fr.free.nrw.commons.category;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Displays images for a particular category with load more on scrolling incorporated
 */
public class CategoryImagesListFragment extends DaggerFragment {

    private static int TIMEOUT_SECONDS = 15;

    private GridViewAdapter gridAdapter;

    @BindView(R.id.statusMessage)
    TextView statusTextView;
    @BindView(R.id.loadingImagesProgressBar) ProgressBar progressBar;
    @BindView(R.id.categoryImagesList) GridView gridView;

    private boolean hasMoreImages = true;
    private boolean isLoading;
    private String categoryName = null;

    @Inject CategoryImageController controller;
    @Inject @Named("category_prefs") SharedPreferences categoryPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_category_images, container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        gridView.setOnItemClickListener((AdapterView.OnItemClickListener) getActivity());
        initViews();
    }

    /**
     * Initializes the UI elements for the fragment
     * Setup the grid view to and scroll listener for it
     */
    private void initViews() {
        String categoryName = getArguments().getString("categoryName");
        if (getArguments() != null && categoryName != null) {
            this.categoryName = categoryName;
            resetQueryContinueValues(categoryName);
            initList();
            setScrollListener();
        }
    }

    /**
     * Query continue values determine the last page that was loaded for the particular keyword
     * This method resets those values, so that the results can be queried from the first page itself
     * @param keyword
     */
    private void resetQueryContinueValues(String keyword) {
        SharedPreferences.Editor editor = categoryPreferences.edit();
        editor.remove(keyword);
        editor.apply();
    }

    /**
     * Checks for internet connection and then initializes the grid view with first 10 images of that category
     */
    @SuppressLint("CheckResult")
    private void initList() {
        if(!NetworkUtils.isInternetConnectionEstablished(getContext())) {
            handleNoInternet();
            return;
        }

        isLoading = true;
        progressBar.setVisibility(VISIBLE);
        Observable.fromCallable(() -> controller.getCategoryImages(categoryName))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .subscribe(this::handleSuccess, this::handleError);
    }

    /**
     * Handles the UI updates for no internet scenario
     */
    private void handleNoInternet() {
        progressBar.setVisibility(GONE);
        if (gridAdapter == null || gridAdapter.isEmpty()) {
            statusTextView.setVisibility(VISIBLE);
            statusTextView.setText(getString(R.string.no_internet));
        } else {
            ViewUtil.showSnackbar(gridView, R.string.no_internet);
        }
    }

    /**
     * Logs and handles API error scenario
     * @param throwable
     */
    private void handleError(Throwable throwable) {
        Timber.e(throwable, "Error occurred while loading featured images");
        initErrorView();
    }

    /**
     * Handles the UI updates for a error scenario
     */
    private void initErrorView() {
        ViewUtil.showSnackbar(gridView, R.string.error_loading_images);
        progressBar.setVisibility(GONE);
        if (gridAdapter == null || gridAdapter.isEmpty()) {
            statusTextView.setVisibility(VISIBLE);
            statusTextView.setText(getString(R.string.no_images_found));
        } else {
            statusTextView.setVisibility(GONE);
        }
    }

    /**
     * Initializes the adapter with a list of Media objects
     * @param mediaList
     */
    private void setAdapter(List<Media> mediaList) {
        gridAdapter = new GridViewAdapter(this.getContext(), R.layout.layout_category_images, mediaList);
        gridView.setAdapter(gridAdapter);
    }

    /**
     * Sets the scroll listener for the grid view so that more images are fetched when the user scrolls down
     * Checks if the category has more images before loading
     * Also checks whether images are currently being fetched before triggering another request
     */
    private void setScrollListener() {
        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (hasMoreImages && !isLoading && (firstVisibleItem + visibleItemCount + 1 >= totalItemCount)) {
                    isLoading = true;
                    fetchMoreImages();
                }
            }
        });
    }

    /**
     * Fetches more images for the category and adds it to the grid view adapter
     */
    @SuppressLint("CheckResult")
    private void fetchMoreImages() {
        if(!NetworkUtils.isInternetConnectionEstablished(getContext())) {
            handleNoInternet();
            return;
        }

        progressBar.setVisibility(VISIBLE);
        Observable.fromCallable(() -> controller.getCategoryImages(categoryName))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .subscribe(this::handleSuccess, this::handleError);
    }

    /**
     * Handles the success scenario
     * On first load, it initializes the grid view. On subsequent loads, it adds items to the adapter
     * @param collection
     */
    private void handleSuccess(List<Media> collection) {
        if(collection == null || collection.isEmpty()) {
            initErrorView();
            hasMoreImages = false;
            return;
        }

        if(gridAdapter == null) {
            setAdapter(collection);
        } else {
            gridAdapter.addItems(collection);
        }

        progressBar.setVisibility(GONE);
        isLoading = false;
        statusTextView.setVisibility(GONE);
    }

    public ListAdapter getAdapter() {
        return gridView.getAdapter();
    }

    /**
     * This method will be called on back pressed of CategoryImagesActivity.
     * It initializes the grid view by setting adapter.
     */
    @Override
    public void onResume() {
        gridView.setAdapter(gridAdapter);
        super.onResume();
    }
}
