package fr.free.nrw.commons.depictions.SubClass;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pedrogomez.renderers.RVRendererAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.depictions.DepictionDetailsActivity;
import fr.free.nrw.commons.explore.depictions.SearchDepictionsAdapterFactory;
import fr.free.nrw.commons.explore.depictions.SearchDepictionsRenderer;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class SubDepictionListFragment extends DaggerFragment implements SubDepictionListContract.View {

    @BindView(R.id.imagesListBox)
    RecyclerView depictionsRecyclerView;
    @BindView(R.id.imageSearchInProgress)
    ProgressBar progressBar;
    @BindView(R.id.imagesNotFound)
    TextView depictionNotFound;
    @BindView(R.id.bottomProgressBar)
    ProgressBar bottomProgressBar;
    private boolean isParentDepiction = true;
    private RVRendererAdapter<DepictedItem> depictionsAdapter;
    private boolean hasMoreImages = true;
    private boolean isLoading = true;
    RecyclerView.LayoutManager layoutManager;

    @Inject SubDepictionListPresenter presenter;

    private final SearchDepictionsAdapterFactory adapterFactory = new SearchDepictionsAdapterFactory(new SearchDepictionsRenderer.DepictCallback() {
        @Override
        public void depictsClicked(DepictedItem item) {
            // Open SubDepiction Details page
            DepictionDetailsActivity.startYourself(getContext(), item);
            presenter.saveQuery();
        }

        @Override
        public void fetchThumbnailUrlForEntity(String entityId, int position) {
            presenter.fetchThumbnailForEntityId(entityId, position);
        }

    });

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_browse_image, container, false);
        ButterKnife.bind(this, v);
        presenter.onAttachView(this);
        isParentDepiction = false;
        depictionNotFound.setVisibility(GONE);
        if (!NetworkUtils.isInternetConnectionEstablished(getContext())) {
            handleNoInternet();
        } else {
            progressBar.setVisibility(View.VISIBLE);
            try {
                presenter.initSubDepictionList();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            depictionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        else{
            depictionsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        }
        ArrayList<DepictedItem> items = new ArrayList<>();
        depictionsAdapter = adapterFactory.create(items);
        depictionsRecyclerView.setAdapter(depictionsAdapter);
        return v;
    }

    private void handleNoInternet() {
        progressBar.setVisibility(GONE);
        ViewUtil.showShortSnackbar(depictionsRecyclerView, R.string.no_internet);
    }

    @Override
    public void onImageUrlFetched(String response, int position) {
        depictionsAdapter.getItem(position).setImageUrl(response);
        depictionsAdapter.notifyItemChanged(position);
    }

    @Override
    public void onSuccess(List<DepictedItem> mediaList) {
        isLoading = false;
        hasMoreImages = false;
        progressBar.setVisibility(View.GONE);
        depictionNotFound.setVisibility(GONE);
        bottomProgressBar.setVisibility(GONE);
        int itemCount=layoutManager.getItemCount();
        depictionsAdapter.addAll(mediaList);
        if(itemCount!=0) {
            depictionsAdapter.notifyItemRangeInserted(itemCount, mediaList.size()-1);
        }else{
            depictionsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void initErrorView() {
        hasMoreImages = false;
        progressBar.setVisibility(GONE);
        bottomProgressBar.setVisibility(GONE);
        depictionNotFound.setVisibility(VISIBLE);
        String no_depiction = getString(R.string.depictions_not_found);
        depictionNotFound.setText(String.format(Locale.getDefault(), no_depiction, presenter.getQuery()));

    }

    @Override
    public void showSnackbar() {
        ViewUtil.showShortSnackbar(depictionsRecyclerView, R.string.error_loading_depictions);
    }

    @Override
    public void setIsLastPage(boolean b) {

    }

    @Override
    public boolean isParentDepiction() {
        return isParentDepiction;
    }
}
