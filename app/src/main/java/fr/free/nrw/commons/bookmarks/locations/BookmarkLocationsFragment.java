package fr.free.nrw.commons.bookmarks.locations;

import android.Manifest.permission;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionController;
import fr.free.nrw.commons.databinding.FragmentBookmarksLocationsBinding;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.fragments.CommonPlaceClickActions;
import fr.free.nrw.commons.nearby.fragments.PlaceAdapter;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import kotlin.Unit;

public class BookmarkLocationsFragment extends DaggerFragment {

    public FragmentBookmarksLocationsBinding binding;

    @Inject BookmarkLocationsController controller;
    @Inject ContributionController contributionController;
    @Inject BookmarkLocationsDao bookmarkLocationDao;
    @Inject CommonPlaceClickActions commonPlaceClickActions;
    private PlaceAdapter adapter;
    private ActivityResultLauncher<String[]> inAppCameraLocationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
        @Override
        public void onActivityResult(Map<String, Boolean> result) {
            boolean areAllGranted = true;
            for(final boolean b : result.values()) {
                areAllGranted = areAllGranted && b;
            }

            if (areAllGranted) {
                contributionController.locationPermissionCallback.onLocationPermissionGranted();
            } else {
                if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
                    contributionController.handleShowRationaleFlowCameraLocation(getActivity(), inAppCameraLocationPermissionLauncher);
                } else {
                    contributionController.locationPermissionCallback.onLocationPermissionDenied(getActivity().getString(R.string.in_app_camera_location_permission_denied));
                }
            }
        }
    });

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
        binding = FragmentBookmarksLocationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.loadingImagesProgressBar.setVisibility(View.VISIBLE);
        binding.listView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PlaceAdapter(bookmarkLocationDao,
            place -> Unit.INSTANCE,
            (place, isBookmarked) -> {
                adapter.remove(place);
                return Unit.INSTANCE;
            },
            commonPlaceClickActions,
            inAppCameraLocationPermissionLauncher
        );
        binding.listView.setAdapter(adapter);
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
        adapter.setItems(places);
        binding.loadingImagesProgressBar.setVisibility(View.GONE);
        if (places.size() <= 0) {
            binding.statusMessage.setText(R.string.bookmark_empty);
            binding.statusMessage.setVisibility(View.VISIBLE);
        } else {
            binding.statusMessage.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        contributionController.handleActivityResult(getActivity(), requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
