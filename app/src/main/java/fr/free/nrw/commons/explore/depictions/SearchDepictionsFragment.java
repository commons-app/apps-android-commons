package fr.free.nrw.commons.explore.depictions;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static fr.free.nrw.commons.explore.depictions.DepictionAdapterDelegatesKt.depictionDelegate;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil.ItemCallback;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter;
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
import kotlin.Unit;

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
    private RecyclerView.LayoutManager layoutManager;
    private boolean isLoading = true;
    private final int PAGE_SIZE = 25;
    @Inject
    SearchDepictionsFragmentPresenter presenter;
    private DepictionAdapter depictionsAdapter;
    private boolean isLastPage;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_browse_image, container, false);
        ButterKnife.bind(this, rootView);
        if (getActivity().getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT) {
            layoutManager = new LinearLayoutManager(getContext());
        } else {
            layoutManager = new GridLayoutManager(getContext(), 2);
        }
        depictionsRecyclerView.setLayoutManager(layoutManager);
        depictionsAdapter = new DepictionAdapter(
            depictedItem -> {
                WikidataItemDetailsActivity.startYourself(getContext(), depictedItem);
                presenter.saveQuery();
                return Unit.INSTANCE;
            });
        depictionsRecyclerView.setAdapter(depictionsAdapter);
        depictionsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(final RecyclerView recyclerView, final int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull final RecyclerView recyclerView, final int dx, final int dy) {
                super.onScrolled(recyclerView, dx, dy);

                final int visibleItemCount = layoutManager.getChildCount();
                final int totalItemCount = layoutManager.getItemCount();
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
    private void loadMoreItems(final boolean reInitialise) {
        presenter.updateDepictionList(presenter.getQuery(),PAGE_SIZE, reInitialise);
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        presenter.onAttachView(this);
    }

    /**
     * Called when user selects "Items" from Search Activity
     * to load the list of depictions from API
     *
     * @param query string searched in the Explore Activity
     */
    public void updateDepictionList(final String query) {
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
        final String no_depiction = getString(R.string.depictions_not_found);
        depictionNotFound.setText(String.format(Locale.getDefault(), no_depiction, presenter.getQuery()));
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
    public void onSuccess(final List<DepictedItem> mediaList) {
        isLoading = false;
        progressBar.setVisibility(GONE);
        depictionNotFound.setVisibility(GONE);
        bottomProgressBar.setVisibility(GONE);
        depictionsAdapter.addAll(mediaList);
    }

    @Override
    public void loadingDepictions(final boolean isLoading) {
        depictionNotFound.setVisibility(GONE);
        bottomProgressBar.setVisibility(VISIBLE);
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

    /**
     * Inform the view that there are no more items to be loaded for this search query
     * or reset the isLastPage for the current query
     * @param isLastPage
     */
    @Override
    public void setIsLastPage(final boolean isLastPage) {
        this.isLastPage=isLastPage;
        progressBar.setVisibility(GONE);
    }
}
