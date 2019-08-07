package fr.free.nrw.commons.depictions;

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

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Actvity for shhowing image list after selected an item from SearchActivity In Explore
 */

public class DepictedImagesActivity extends NavigationBaseActivity implements FragmentManager.OnBackStackChangedListener, MediaDetailPagerFragment.MediaDetailProvider,
        AdapterView.OnItemClickListener, DepictedImagesContract.View {

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
    @Inject
    DepictedImagesPresenter presenter;
    private GridViewAdapter gridAdapter;
    private FragmentManager supportFragmentManager;
    private MediaDetailPagerFragment mediaDetails;
    private String entityId = null;
    private boolean hasMoreImages = true;
    private boolean isLoading = true;
    private int mediaSize = 0;

    /**
     * Consumers should be simply using this method to use this activity.
     *
     * @param context      A Context of the application package implementing this class.
     * @param depictedItem Name of the depicts for displaying its details
     */
    public static void startYourself(Context context, DepictedItem depictedItem) {
        Intent intent = new Intent(context, DepictedImagesActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("depictsName", depictedItem.getDepictsLabel());
        intent.putExtra("entityId", depictedItem.getEntityId());
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_depict_detail);
        ButterKnife.bind(this);
        gridView.setOnItemClickListener(this);
        supportFragmentManager = getSupportFragmentManager();
        supportFragmentManager.addOnBackStackChangedListener(this);
        setPageTitle();
        initDrawer();
        forceInitBackButton();
        presenter.onAttachView(this);
        initList();
        setScrollListener();
    }

    private void initList() {
        presenter.initList(entityId);
        if (!NetworkUtils.isInternetConnectionEstablished(this)) {
            handleNoInternet();
        } else presenter.initList(entityId);
    }

    /**
     * Gets the passed depictsName from the intents and displays it as the page title
     */
    private void setPageTitle() {
        if (getIntent() != null && getIntent().getStringExtra("depictsName") != null) {
            setTitle(getIntent().getStringExtra("depictsName"));
            entityId = getIntent().getStringExtra("entityId");
        }
    }

    @Override
    public void onBackPressed() {
        if (supportFragmentManager.getBackStackEntryCount() == 1) {
            // back to search so show search toolbar and hide navigation toolbar
            mediaContainer.setVisibility(View.GONE);
        }
        super.onBackPressed();
    }

    /**
     * Handles the UI updates for no internet scenario
     */
    @Override
    public void handleNoInternet() {
        progressBar.setVisibility(GONE);
        if (gridAdapter == null || gridAdapter.isEmpty()) {
            statusTextView.setVisibility(VISIBLE);
            statusTextView.setText(getString(R.string.no_internet));
        } else {
            ViewUtil.showShortSnackbar(parentLayout, R.string.no_internet);
        }
    }

    /**
     * Handles the UI updates for a error scenario
     */
    @Override
    public void initErrorView() {
        progressBar.setVisibility(GONE);
        if (gridAdapter == null || gridAdapter.isEmpty()) {
            statusTextView.setVisibility(VISIBLE);
            statusTextView.setText(getString(R.string.no_images_found));
        } else {
            statusTextView.setVisibility(GONE);
        }
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
                    if (!NetworkUtils.isInternetConnectionEstablished(getBaseContext())) {
                        handleNoInternet();
                        return;
                    } else {
                        presenter.fetchMoreImages();
                    }
                }
                if (!hasMoreImages) {
                    progressBar.setVisibility(GONE);
                }
            }
        });
    }

    @Override
    public void handleLabelforImage(String s, int position) {
        if (!s.trim().equals(getString(R.string.detail_caption_empty))) {
            gridAdapter.getItem(position).setThumbnailTitle(s);
            gridAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void showSnackBar() {
        ViewUtil.showShortSnackbar(parentLayout, R.string.error_loading_images);
    }


    @Override
    public Media getMediaAtPosition(int i) {
        if (gridAdapter == null) {
            // not yet ready to return data
            return null;
        } else {
            return gridAdapter.getItem(i);
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
    @Override
    public void viewPagerNotifyDataSetChanged() {
        if (mediaDetails != null) {
            mediaDetails.notifyDataSetChanged();
        }
    }

    @Override
    public void progressBarVisible(Boolean value) {
        if (value) {
            progressBar.setVisibility(VISIBLE);
        } else {
            progressBar.setVisibility(GONE);
        }
    }

    /**
     * It return an instance of gridView adapter which helps in extracting media details
     * used by the gridView
     *
     * @return GridView Adapter
     */
    @Override
    public ListAdapter getAdapter() {
        return gridAdapter;
    }

    /**
     * Initializes the adapter with a list of Media objects
     *
     * @param mediaList List of new Media to be displayed
     */
    @Override
    public void setAdapter(List<Media> mediaList) {
        gridAdapter = new fr.free.nrw.commons.depictions.GridViewAdapter(this, R.layout.layout_depict_image, mediaList);
        gridView.setAdapter(gridAdapter);
    }

    @Override
    public void addItemsToAdapter(List<Media> media) {
        gridAdapter.addAll(media);
        gridAdapter.notifyDataSetChanged();
    }

    @Override
    public void setLoadingStatus(Boolean value) {
        if (!value) {
            statusTextView.setVisibility(GONE);
        }
        isLoading = value;
    }

    /**
     * On clicking any image from this list it should show the mediaDetails
     */

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

    /**
     * Handles the success scenario
     * On first load, it initializes the grid view. On subsequent loads, it adds items to the adapter
     *
     * @param collection List of new Media to be displayed
     */
    @Override
    public void handleSuccess(List<Media> collection) {
        if (collection == null || collection.isEmpty()) {
            initErrorView();
            hasMoreImages = false;
            return;
        }

        presenter.addItemsToQueryList(collection);
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        progressBar.setVisibility(GONE);
        isLoading = false;
        statusTextView.setVisibility(GONE);
        for (Media m : collection) {
            presenter.replaceTitlesWithCaptions(m.getDisplayTitle(), mediaSize++);
        }
    }
}
