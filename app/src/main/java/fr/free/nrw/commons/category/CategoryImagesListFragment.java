package fr.free.nrw.commons.category;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static fr.free.nrw.commons.depictions.Media.DepictedImagesFragment.PAGE_ID_PREFIX;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.media.MediaClient;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

/**
 * Displays images for a particular category with load more on scrolling incorporated
 */
public class CategoryImagesListFragment extends DaggerFragment {

    /**
     * counts the total number of items loaded from the API
     */
    private int mediaSize = 0;

    private GridViewAdapter gridAdapter;

    @BindView(R.id.statusMessage)
    TextView statusTextView;
    @BindView(R.id.loadingImagesProgressBar) ProgressBar progressBar;
    @BindView(R.id.categoryImagesList) GridView gridView;
    @BindView(R.id.parentLayout) RelativeLayout parentLayout;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private boolean hasMoreImages = true;
    private boolean isLoading = true;
    private String categoryName = null;

    @Inject MediaClient mediaClient;
    @Inject
    @Named("default_preferences")
    JsonKvStore categoryKvStore;

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
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
        categoryKvStore.remove("query_continue_" + keyword);
    }

    /**
     * Checks for internet connection and then initializes the grid view with first 10 images of that category
     */
    @SuppressLint("CheckResult")
    private void initList() {
        if (!NetworkUtils.isInternetConnectionEstablished(getContext())) {
            handleNoInternet();
            return;
        }

        isLoading = true;
        progressBar.setVisibility(VISIBLE);
        compositeDisposable.add(mediaClient.getMediaListFromCategory(categoryName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSuccess, this::handleError));
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
            ViewUtil.showShortSnackbar(parentLayout, R.string.no_internet);
        }
    }

    /**
     * Logs and handles API error scenario
     * @param throwable
     */
    private void handleError(Throwable throwable) {
        Timber.e(throwable, "Error occurred while loading images inside a category");
        try{
            ViewUtil.showShortSnackbar(parentLayout, R.string.error_loading_images);
            initErrorView();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Handles the UI updates for a error scenario
     */
    private void initErrorView() {
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
     * @param mediaList List of new Media to be displayed
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
                if (!hasMoreImages){
                    progressBar.setVisibility(GONE);
                }
            }
        });
    }

    /**
     * This method is called when viewPager has reached its end.
     * Fetches more images for the category and adds it to the grid view and viewpager adapter
     */
    public void fetchMoreImagesViewPager(){
        if (hasMoreImages && !isLoading) {
            isLoading = true;
            fetchMoreImages();
        }
        if (!hasMoreImages){
            progressBar.setVisibility(GONE);
        }
    }

    /**
     * Fetches more images for the category and adds it to the grid view adapter
     */
    @SuppressLint("CheckResult")
    private void fetchMoreImages() {
        if (!NetworkUtils.isInternetConnectionEstablished(getContext())) {
            handleNoInternet();
            return;
        }

        progressBar.setVisibility(VISIBLE);
        compositeDisposable.add(mediaClient.getMediaListFromCategory(categoryName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSuccess, this::handleError));
    }

    /**
     * Handles the success scenario
     * On first load, it initializes the grid view. On subsequent loads, it adds items to the adapter
     * @param collection List of new Media to be displayed
     */
    private void handleSuccess(List<Media> collection) {
        if (collection == null || collection.isEmpty()) {
            initErrorView();
            hasMoreImages = false;
            return;
        }

        if (gridAdapter == null) {
            setAdapter(collection);
        } else {
            if (gridAdapter.containsAll(collection)) {
                hasMoreImages = false;
                return;
            }
            gridAdapter.addItems(collection);
            ((CategoryImagesCallback) getContext()).viewPagerNotifyDataSetChanged();
        }

        progressBar.setVisibility(GONE);
        isLoading = false;
        statusTextView.setVisibility(GONE);
        for (Media m : collection) {
            final String pageId = m.getPageId();
            if (pageId != null) {
                replaceTitlesWithCaptions(PAGE_ID_PREFIX + pageId, mediaSize++);
            }
        }
    }

    /**
     * fetch captions for the image using filename and replace title of on the image thumbnail(if captions are available)
     * else show filename
     */
    public void replaceTitlesWithCaptions(String wikibaseIdentifier, int i) {
        compositeDisposable.add(mediaClient.getCaptionByWikibaseIdentifier(wikibaseIdentifier)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(subscriber -> {
                    handleLabelforImage(subscriber, i);
                }));

    }

    /**
     * If caption is available for the image, then modify grid adapter
     * to show captions
     */
    private void handleLabelforImage(String s, int position) {
        if (!s.trim().equals(getString(R.string.detail_caption_empty))) {
            gridAdapter.getItem(position).setThumbnailTitle(s);
            gridAdapter.notifyDataSetChanged();
        }
    }

    /**
     * It return an instance of gridView adapter which helps in extracting media details
     * used by the gridView
     * @return  GridView Adapter
     */
    public ListAdapter getAdapter() {
        return gridAdapter;
    }

}
