package fr.free.nrw.commons.bookmarks.locations;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pedrogomez.renderers.RVRendererAdapter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionController;
import fr.free.nrw.commons.nearby.NearbyAdapterFactory;
import fr.free.nrw.commons.nearby.Place;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class BookmarkLocationsFragment extends DaggerFragment {

    @BindView(R.id.statusMessage) TextView statusTextView;
    @BindView(R.id.loadingImagesProgressBar) ProgressBar progressBar;
    @BindView(R.id.listView) RecyclerView recyclerView;
    @BindView(R.id.parentLayout) RelativeLayout parentLayout;

    @Inject
    BookmarkLocationsController controller;
    @Inject @Named("direct_nearby_upload_prefs") SharedPreferences directPrefs;
    private NearbyAdapterFactory adapterFactory;
    private ContributionController contributionController;

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
        contributionController = new ContributionController(this);
        adapterFactory = new NearbyAdapterFactory(this, contributionController);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(
                adapterFactory.create(
                        new ArrayList<Place>(),
                        () -> {
                            initList();
                        }
                )
        );
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Timber.d("onRequestPermissionsResult: req code = " + " perm = " + permissions + " grant =" + grantResults);

        switch (requestCode) {
            // 4 = "Read external storage" allowed when gallery selected
            case 4: {
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    Timber.d("Call controller.startGalleryPick()");
                    contributionController.startGalleryPick();
                }
            }
            break;

            // 5 = "Write external storage" allowed when camera selected
            case 5: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Call controller.startCameraCapture()");
                    contributionController.startCameraCapture();
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Timber.d("OnActivityResult() parameters: Req code: %d Result code: %d Data: %s",
                    requestCode, resultCode, data);
            String wikidataEntityId = directPrefs.getString("WikiDataEntityId", null);
            if (requestCode == ContributionController.SELECT_FROM_CAMERA) {
                // If coming from camera, pass null as uri. Because camera photos get saved to a
                // fixed directory
                contributionController.handleImagePicked(requestCode, null, true, wikidataEntityId);
            } else {
                contributionController.handleImagePicked(requestCode, data.getData(), true, wikidataEntityId);
            }
        } else {
            Timber.e("OnActivityResult() parameters: Req code: %d Result code: %d Data: %s",
                    requestCode, resultCode, data);
        }
    }
}
