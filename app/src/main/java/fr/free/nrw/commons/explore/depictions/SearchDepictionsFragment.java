package fr.free.nrw.commons.explore.depictions;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.pedrogomez.renderers.RVRendererAdapter;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.depictions.WikidataItemDetailsActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;

/**
 * Display depictions in search fragment
 */
public class SearchDepictionsFragment extends CommonsDaggerSupportFragment implements SearchDepictionsFragmentContract.View {

    @BindView(R.id.imagesListBox)
    RecyclerView depictionsRecyclerView;
    @BindView(R.id.imageSearchInProgress)
    ProgressBar progressBar;
    @BindView(R.id.imagesNotFound)
    TextView depictionNotFound;
    @BindView(R.id.bottomProgressBar)
    ProgressBar bottomProgressBar;
    RecyclerView.LayoutManager layoutManager;
    private boolean isLoading = true;
    private int PAGE_SIZE = 25;
    @Inject
    SearchDepictionsFragmentPresenter presenter;
    private final SearchDepictionsAdapterFactory adapterFactory = new SearchDepictionsAdapterFactory(new SearchDepictionsRenderer.DepictCallback() {
        @Override
        public void depictsClicked(DepictedItem item) {
            WikidataItemDetailsActivity.startYourself(getContext(), item);
            presenter.saveQuery();
        }

        /**
         *fetch thumbnail image for all the depicted items (if available)
         */
        @Override
        public void fetchThumbnailUrlForEntity(String entityId, int position) {
            presenter.fetchThumbnailForEntityId(entityId,position);
        }

    });
    private RVRendererAdapter<DepictedItem> depictionsAdapter;
    private boolean isLastPage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_browse_image, container, false);
        ButterKnife.bind(this, rootView);
        if (getActivity().getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT) {
            layoutManager = new LinearLayoutManager(getContext());
        } else {
            layoutManager = new GridLayoutManager(getContext(), 2);
        }
        depictionsRecyclerView.setLayoutManager(layoutManager);
        depictionsAdapter = adapterFactory.create();
        depictionsRecyclerView.setAdapter(depictionsAdapter);
        depictionsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition=0;
                if(layoutManager instanceof GridLayoutManager){
                    firstVisibleItemPosition=((GridLayoutManager) layoutManager).findFirstVisibleItemPosition();
                } else {
                    firstVisibleItemPosition=((LinearLayoutManager)layoutManager).findFirstVisibleItemPosition();
                }

                /**
                 * If the user isn't currently loading items and the last page hasn’t been reached,
                 * then it checks against the current position in view to decide whether or not to load more items.
                 */
                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= PAGE_SIZE) {
                        loadMoreItems(false);
                    }
                }
            }
        });
        return rootView;
    }

    /**
     * Fetch PAGE_SIZE number of items
     */
    private void loadMoreItems(boolean reInitialise) {
        presenter.updateDepictionList(presenter.getQuery(),PAGE_SIZE, reInitialise);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        presenter.onAttachView(this);
    }

    /**
     * Called when user selects "Items" from Search Activity
     * to load the list of depictions from API
     *
     * @param query string searched in the Explore Activity
     */
    public void updateDepictionList(String query) {
        presenter.initializeQuery(query);
         if (!NetworkUtils.isInternetConnectionEstablished(getContext())) {
            handleNoInternet();
            return;
        }
        loadMoreItems(true);
    }

    /**
     * Handles the UI updates for a error scenario
     */
    @Override
    public void initErrorView() {
        isLoading = false;
        progressBar.setVisibility(GONE);
        bottomProgressBar.setVisibility(GONE);
        depictionNotFound.setVisibility(VISIBLE);
        String no_depiction = getString(R.string.depictions_not_found);
        depictionNotFound.setText(String.format(Locale.getDefault(), no_depiction, presenter.getQuery()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        depictionsAdapter.clear();
        depictionsRecyclerView.cancelPendingInputEvents();
    }

    /**
     * Handles the UI updates for no internet scenario
     */
    @Override
    public void handleNoInternet() {
        progressBar.setVisibility(GONE);
        ViewUtil.showShortSnackbar(depictionsRecyclerView, R.string.no_internet);
    }

    /**
     * If a non empty list is successfully returned from the api then modify the view
     * like hiding empty labels, hiding progressbar and notifying the apdapter that list of items has been fetched from the API
     */
    @Override
    public void onSuccess(List<DepictedItem> mediaList) {
        isLoading = false;
        progressBar.setVisibility(View.GONE);
        depictionNotFound.setVisibility(GONE);
        bottomProgressBar.setVisibility(GONE);
        int itemCount = layoutManager.getItemCount();
        depictionsAdapter.addAll(mediaList);
        if(itemCount!=0) {
            depictionsAdapter.notifyItemRangeInserted(itemCount, mediaList.size()-1);
        }else{
            depictionsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void loadingDepictions(boolean isLoading) {
        depictionNotFound.setVisibility(GONE);
        bottomProgressBar.setVisibility(View.VISIBLE);
        progressBar.setVisibility(GONE);
        this.isLoading = isLoading;
    }

    @Override
    public void clearAdapter() {
        depictionsAdapter.clear();
    }

    @Override
    public void showSnackbar() {
        ViewUtil.showShortSnackbar(depictionsRecyclerView, R.string.error_loading_depictions);
    }

    @Override
    public RVRendererAdapter<DepictedItem> getAdapter() {
        return depictionsAdapter;
    }

    @Override
    public void onImageUrlFetched(String response, int position) {
         depictionsAdapter.getItem(position).setImageUrl(response);
        depictionsAdapter.notifyItemChanged(position);
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
}
