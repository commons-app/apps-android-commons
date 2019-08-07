package fr.free.nrw.commons.explore.depictions;

import android.content.Context;
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
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.depictions.DepictedImagesActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

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
    @Inject
    SearchDepictionsFragmentPresenter presenter;
    private final SearchDepictionsAdapterFactory adapterFactory = new SearchDepictionsAdapterFactory(item -> {
        // Called on Click of a individual depicted Item
        // Open Depiction Details activity
        DepictedImagesActivity.startYourself(getContext(), item);
        presenter.saveQuery();
    });
    private RVRendererAdapter<DepictedItem> depictionsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_browse_image, container, false);
        ButterKnife.bind(this, rootView);
        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            depictionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        } else {
            depictionsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        }
        ArrayList<DepictedItem> items = new ArrayList<>();
        depictionsAdapter = adapterFactory.create(items);
        depictionsRecyclerView.setAdapter(depictionsAdapter);
        depictionsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // check if end of recycler view is reached, if yes then add more results to existing results
                if (!recyclerView.canScrollVertically(1)) {
                    presenter.addDepictionsToList();
                }
            }
        });
        return rootView;
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
        presenter.updateDepictionList(query);
    }

    /**
     * Handles the UI updates for a error scenario
     */
    @Override
    public void initErrorView() {
        progressBar.setVisibility(GONE);
        bottomProgressBar.setVisibility(GONE);
        depictionNotFound.setVisibility(VISIBLE);
        String no_depiction = getString(R.string.depictions_not_found);
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

    @Override
    public void onSuccess(List<DepictedItem> mediaList) {
        progressBar.setVisibility(View.GONE);
        depictionNotFound.setVisibility(GONE);
        bottomProgressBar.setVisibility(GONE);
        depictionsAdapter.addAll(mediaList);
        depictionsAdapter.notifyDataSetChanged();
    }

    @Override
    public void loadingDepictions() {
        depictionNotFound.setVisibility(GONE);
        bottomProgressBar.setVisibility(View.VISIBLE);
        progressBar.setVisibility(GONE);
    }

    @Override
    public void clearAdapter() {
        depictionsAdapter.clear();
    }

    @Override
    public void showSnackbar() {
        ViewUtil.showShortSnackbar(depictionsRecyclerView, R.string.error_loading_depictions);
    }
}
