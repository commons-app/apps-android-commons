package fr.free.nrw.commons.depictions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.explore.depictions.DepictsClient;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class DepictionsDetailActivity extends NavigationBaseActivity implements FragmentManager.OnBackStackChangedListener, MediaDetailPagerFragment.MediaDetailProvider,
        AdapterView.OnItemClickListener {

    private static int TIMEOUT_SECONDS = 15;

    private GridViewAdapter gridAdapter;
    private FragmentManager supportFragmentManager;

    private MediaDetailPagerFragment mediaDetails;
    @BindView(R.id.mediaContainer)
    FrameLayout mediaContainer;

    @BindView(R.id.statusMessage)
    TextView statusTextView;
    @BindView(R.id.loadingImagesProgressBar)
    ProgressBar progressBar;
    @BindView(R.id.depicts_image_list)
    GridView gridView;
    @BindView(R.id.parentLayout)
    RelativeLayout parentLayout;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private boolean hasMoreImages = true;
    private boolean isLoading = true;
    private String depictName = null;
    private String entityId = null;
    private List<Media> queryList = new ArrayList<>();
    @Inject
    DepictsClient depictsClient;

    @Inject
    @Named("default_preferences")
    JsonKvStore depictionKvStore;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_depict_detail);
        ButterKnife.bind(this);
        gridView.setOnItemClickListener((AdapterView.OnItemClickListener) this);
        supportFragmentManager = getSupportFragmentManager();
        supportFragmentManager.addOnBackStackChangedListener(this);
        setPageTitle();
        initDrawer();
        forceInitBackButton();
    }

    /**
     * Gets the passed depictsName from the intents and displays it as the page title
     */
    private void setPageTitle() {
        if (getIntent() != null && getIntent().getStringExtra("depictsName") != null) {
            setTitle(getIntent().getStringExtra("depictsName"));
            entityId = getIntent().getStringExtra("entityId");
            //resetQueryContinueValues(depictName);
            initList();
            setScrollListener();
        }
    }

    /**
     * Checks for internet connection and then initializes the grid view with first 10 images of that depiction
     */
    @SuppressLint("CheckResult")
    private void initList() {
        if (!NetworkUtils.isInternetConnectionEstablished(this)) {
            handleNoInternet();
            return;
        }

        isLoading = true;
        progressBar.setVisibility(VISIBLE);
        compositeDisposable.add(depictsClient.fetchImagesForDepictedItem(entityId, 25, 0)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
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
        Timber.e(throwable, "Error occurred while loading images inside items");
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
        gridAdapter = new fr.free.nrw.commons.depictions.GridViewAdapter(this, R.layout.layout_depict_image, mediaList);
        gridView.setAdapter(gridAdapter);
    }


    /**
     * Query continue values determine the last page that was loaded for the particular keyword
     * This method resets those values, so that the results can be queried from the first page itself
     * @param keyword
     */
    private void resetQueryContinueValues(String keyword) {
        depictionKvStore.remove("query_continue_" + keyword);
    }

    /**
     * Sets the scroll listener for the grid view so that more images are fetched when the user scrolls down
     * Checks if the item has more images before loading
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
     * Fetches more images for the item and adds it to the grid view adapter
     */
    @SuppressLint("CheckResult")
    private void fetchMoreImages() {
        if (!NetworkUtils.isInternetConnectionEstablished(this)) {
            handleNoInternet();
            return;
        }

        progressBar.setVisibility(VISIBLE);
        compositeDisposable.add(depictsClient.fetchImagesForDepictedItem(entityId, 25, queryList.size())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .subscribe(this::handlePaginationSuccess, this::handleError));
    }

    private void handlePaginationSuccess(List<Media> media) {
        queryList.addAll(media);
        progressBar.setVisibility(View.GONE);
        gridAdapter.addAll(media);
        gridAdapter.notifyDataSetChanged();
        isLoading = false;
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

        queryList.addAll(collection);
        if (gridAdapter == null) {
            setAdapter(collection);
        } else {
            if (gridAdapter.containsAll(collection)) {
                hasMoreImages = false;
                return;
            }
            gridAdapter.addItems(collection);

            try {
                viewPagerNotifyDataSetChanged();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        progressBar.setVisibility(GONE);
        isLoading = false;
        statusTextView.setVisibility(GONE);
    }

    /**
     * This method is called when viewPager has reached its end.
     * Fetches more images for the depicts and adds it to the grid view and viewpager adapter
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

    @Override
    public Media getMediaAtPosition(int i) {
        if (gridAdapter == null) {
            // not yet ready to return data
            return null;
        } else {
            return (Media) gridAdapter.getItem(i);
        }
    }

    @Override
    public int getTotalMediaCount() {
        if (gridAdapter == null) {
            return 0;
        }
        return gridAdapter.getCount();
    }

    /**
     * This method is called on success of API call for Images inside an item.
     * The viewpager will notified that number of items have changed.
     */
    public void viewPagerNotifyDataSetChanged() {
        if (mediaDetails!=null){
            mediaDetails.notifyDataSetChanged();
        }
    }

    /**
     * Consumers should be simply using this method to use this activity.
     * @param context  A Context of the application package implementing this class.
     * @param depictedItem Name of the depicts for displaying its details
     */
    public static void startYourself(Context context, DepictedItem depictedItem) {
        Intent intent = new Intent(context, DepictionsDetailActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("depictsName", depictedItem.getDepictsLabel());
        intent.putExtra("entityId", depictedItem.getEntityId());
        context.startActivity(intent);
    }

    /**
     * It return an instance of gridView adapter which helps in extracting media details
     * used by the gridView
     * @return  GridView Adapter
     */
    public ListAdapter getAdapter() {
        return gridAdapter;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mediaContainer.setVisibility(View.VISIBLE);
        if (mediaDetails == null || !mediaDetails.isVisible()) {
            mediaDetails = new MediaDetailPagerFragment(false, true);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.mediaContainer, mediaDetails)
                    .addToBackStack(null)
                    .commit();

        }
        mediaDetails.showImage(position);
        forceInitBackButton();
    }

    @Override
    public void onBackStackChanged() {
        if (supportFragmentManager.getBackStackEntryCount() == 0) {
            initDrawer();
        }
    }
}
