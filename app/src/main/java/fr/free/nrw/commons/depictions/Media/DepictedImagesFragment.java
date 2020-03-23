package fr.free.nrw.commons.depictions.Media;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.depictions.GridViewAdapter;
import fr.free.nrw.commons.depictions.WikidataItemDetailsActivity;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

/**
 * Fragment for showing image list after selected an item from SearchActivity In Explore
 */
public class DepictedImagesFragment extends DaggerFragment implements DepictedImagesContract.View {


    public static final String PAGE_ID_PREFIX = "M";
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
    private String entityId = null;
    private boolean isLastPage;
    private boolean isLoading = true;
    private int mediaSize = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_depict_image, container, false);
        ButterKnife.bind(this, v);
        presenter.onAttachView(this);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        gridView.setOnItemClickListener((AdapterView.OnItemClickListener) getActivity());
        initViews();
    }

    /**
     * Initializes the UI elements for the fragment
     * Setup the grid view to and scroll listener for it
     */
    private void initViews() {
        String depictsName = getArguments().getString("wikidataItemName");
        entityId = getArguments().getString("entityId");
        if (getArguments() != null && depictsName != null) {
            initList();
            setScrollListener();
        }
    }

    private void initList() {
        presenter.initList(entityId);
        if (!NetworkUtils.isInternetConnectionEstablished(getContext())) {
            handleNoInternet();
        } else {
            presenter.initList(entityId);
        }
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
                if (!isLastPage && !isLoading && (firstVisibleItem + visibleItemCount >= totalItemCount)) {
                    isLoading = true;
                    if (!NetworkUtils.isInternetConnectionEstablished(getContext())) {
                        handleNoInternet();
                    } else {
                        presenter.fetchMoreImages();
                    }
                }
                if (isLastPage) {
                    progressBar.setVisibility(GONE);
                }
            }
        });
    }

    /**
     * Seat caption to the image at the given position
     */
    @Override
    public void handleLabelforImage(String caption, int position) {
        if (!caption.trim().equals(getString(R.string.detail_caption_empty))) {
            gridAdapter.getItem(position).setThumbnailTitle(caption);
            gridAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Display snackbar
     */
    @Override
    public void showSnackBar() {
        ViewUtil.showShortSnackbar(parentLayout, R.string.error_loading_images);
    }

    /**
     * Set visibility of progressbar depending on the boolean value
     */
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
        gridAdapter = new fr.free.nrw.commons.depictions.GridViewAdapter(getContext(), R.layout.layout_depict_image, mediaList);
        gridView.setAdapter(gridAdapter);
    }

    /**
     * adds list to adapter
     */
    @Override
    public void addItemsToAdapter(List<Media> media) {
        gridAdapter.addAll(media);
        gridAdapter.notifyDataSetChanged();
    }

    /**
     * Sets loading status depending on the boolean value
     */
    @Override
    public void setLoadingStatus(Boolean value) {
        if (!value) {
            statusTextView.setVisibility(GONE);
        }
        isLoading = value;
    }

    /**
     * Inform the view that there are no more items to be loaded for this search query
     * or reset the isLastPage for the current query
     * @param isLastPage
     */
    @Override
    public void setIsLastPage(boolean isLastPage) {
        this.isLastPage=isLastPage;
        progressBar.setVisibility(GONE);
    }


    /**
     * Handles the success scenario
     * On first load, it initializes the grid view. On subsequent loads, it adds items to the adapter
     *
     * @param collection List of new Media to be displayed
     */
    @Override
    public void handleSuccess(List<Media> collection) {
       presenter.addItemsToQueryList(collection);
        if (gridAdapter == null) {
            setAdapter(collection);
        } else {
            if (gridAdapter.containsAll(collection)) {
                return;
            }
            gridAdapter.addItems(collection);

            try {
                ((WikidataItemDetailsActivity) getContext()).viewPagerNotifyDataSetChanged();
            } catch (RuntimeException e) {
                Timber.e(e);
            }
        }
        progressBar.setVisibility(GONE);
        isLoading = false;
        statusTextView.setVisibility(GONE);
        for (Media media : collection) {
            presenter.replaceTitlesWithCaptions(PAGE_ID_PREFIX +media.getPageId(), mediaSize++);
        }
    }
}
