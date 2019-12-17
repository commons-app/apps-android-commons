package fr.free.nrw.commons.bookmarks.locations;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pedrogomez.renderers.RVRendererAdapter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionController;
import fr.free.nrw.commons.nearby.NearbyAdapterFactory;
import fr.free.nrw.commons.nearby.Place;

public class BookmarkLocationsFragment extends DaggerFragment {

    @BindView(R.id.statusMessage) TextView statusTextView;
    @BindView(R.id.loadingImagesProgressBar) ProgressBar progressBar;
    @BindView(R.id.listView) RecyclerView recyclerView;
    @BindView(R.id.parentLayout) RelativeLayout parentLayout;

    @Inject BookmarkLocationsController controller;
    private NearbyAdapterFactory adapterFactory;
    @Inject ContributionController contributionController;

    /**
     * Create an instance of the fragment with the right bundle parameters
     * @return an instance of the fragment
     */
    public static BookmarkLocationsFragment newInstance() {
        return new BookmarkLocationsFragment();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_bookmarks_locations, container, false);
        ButterKnife.bind(this, v);
        adapterFactory = new NearbyAdapterFactory(this, contributionController);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapterFactory.create(new ArrayList<>(), this::initList));
    }

    @Override
    public void onResume() {
        super.onResume();
        initList();
    }

    /**
     * Initialize the recycler view with bookmarked locations
     */
    private void initList() {
        List<Place> places = controller.loadFavoritesLocations();
        adapterFactory.updateAdapterData(places, (RVRendererAdapter<Place>) recyclerView.getAdapter());
        progressBar.setVisibility(View.GONE);
        if (places.size() <= 0) {
            statusTextView.setText(R.string.bookmark_empty);
            statusTextView.setVisibility(View.VISIBLE);
        } else {
            statusTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        contributionController.handleActivityResult(getActivity(), requestCode, resultCode, data);
    }
}
