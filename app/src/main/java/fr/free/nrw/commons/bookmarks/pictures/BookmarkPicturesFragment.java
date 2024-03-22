package fr.free.nrw.commons.bookmarks.pictures;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.bookmarks.BookmarkListRootFragment;
import fr.free.nrw.commons.category.GridViewAdapter;
import fr.free.nrw.commons.databinding.FragmentBookmarksPicturesBinding;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

public class BookmarkPicturesFragment extends DaggerFragment {

    private GridViewAdapter gridAdapter;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private FragmentBookmarksPicturesBinding binding;
    @Inject
    BookmarkPicturesController controller;

    /**
     * Create an instance of the fragment with the right bundle parameters
     * @return an instance of the fragment
     */
    public static BookmarkPicturesFragment newInstance() {
        return new BookmarkPicturesFragment();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentBookmarksPicturesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.bookmarkedPicturesList.setOnItemClickListener((AdapterView.OnItemClickListener) getParentFragment());
        initList();
    }

    @Override
    public void onStop() {
        super.onStop();
        controller.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (controller.needRefreshBookmarkedPictures()) {
            binding.bookmarkedPicturesList.setVisibility(GONE);
            if (gridAdapter != null) {
                gridAdapter.clear();
                ((BookmarkListRootFragment)getParentFragment()).viewPagerNotifyDataSetChanged();
            }
            initList();
        }
    }

    /**
     * Checks for internet connection and then initializes
     * the recycler view with bookmarked pictures
     */
    @SuppressLint("CheckResult")
    private void initList() {
        if (!NetworkUtils.isInternetConnectionEstablished(getContext())) {
            handleNoInternet();
            return;
        }

        binding.loadingImagesProgressBar.setVisibility(VISIBLE);
        binding.statusMessage.setVisibility(GONE);

        compositeDisposable.add(controller.loadBookmarkedPictures()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSuccess, this::handleError));
    }

    /**
     * Handles the UI updates for no internet scenario
     */
    private void handleNoInternet() {
        binding.loadingImagesProgressBar.setVisibility(GONE);
        if (gridAdapter == null || gridAdapter.isEmpty()) {
            binding.statusMessage.setVisibility(VISIBLE);
            binding.statusMessage.setText(getString(R.string.no_internet));
        } else {
            ViewUtil.showShortSnackbar(binding.parentLayout, R.string.no_internet);
        }
    }

    /**
     * Logs and handles API error scenario
     * @param throwable
     */
    private void handleError(Throwable throwable) {
        Timber.e(throwable, "Error occurred while loading images inside a category");
        try{
            ViewUtil.showShortSnackbar(binding.getRoot(), R.string.error_loading_images);
            initErrorView();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Handles the UI updates for a error scenario
     */
    private void initErrorView() {
        binding.loadingImagesProgressBar.setVisibility(GONE);
        if (gridAdapter == null || gridAdapter.isEmpty()) {
            binding.statusMessage.setVisibility(VISIBLE);
            binding.statusMessage.setText(getString(R.string.no_images_found));
        } else {
            binding.statusMessage.setVisibility(GONE);
        }
    }

    /**
     * Handles the UI updates when there is no bookmarks
     */
    private void initEmptyBookmarkListView() {
        binding.loadingImagesProgressBar.setVisibility(GONE);
        if (gridAdapter == null || gridAdapter.isEmpty()) {
            binding.statusMessage.setVisibility(VISIBLE);
            binding.statusMessage.setText(getString(R.string.bookmark_empty));
        } else {
            binding.statusMessage.setVisibility(GONE);
        }
    }

    /**
     * Handles the success scenario
     * On first load, it initializes the grid view. On subsequent loads, it adds items to the adapter
     * @param collection List of new Media to be displayed
     */
    private void handleSuccess(List<Media> collection) {
        if (collection == null) {
            initErrorView();
            return;
        }
        if (collection.isEmpty()) {
            initEmptyBookmarkListView();
            return;
        }

        if (gridAdapter == null) {
            setAdapter(collection);
        } else {
            if (gridAdapter.containsAll(collection)) {
                binding.loadingImagesProgressBar.setVisibility(GONE);
                binding.statusMessage.setVisibility(GONE);
                binding.bookmarkedPicturesList.setVisibility(VISIBLE);
                binding.bookmarkedPicturesList.setAdapter(gridAdapter);
                return;
            }
            gridAdapter.addItems(collection);
            ((BookmarkListRootFragment) getParentFragment()).viewPagerNotifyDataSetChanged();
        }
        binding.loadingImagesProgressBar.setVisibility(GONE);
        binding.statusMessage.setVisibility(GONE);
        binding.bookmarkedPicturesList.setVisibility(VISIBLE);
    }

    /**
     * Initializes the adapter with a list of Media objects
     * @param mediaList List of new Media to be displayed
     */
    private void setAdapter(List<Media> mediaList) {
        gridAdapter = new GridViewAdapter(
                this.getContext(),
                R.layout.layout_category_images,
                mediaList
        );
        binding.bookmarkedPicturesList.setAdapter(gridAdapter);
    }

    /**
     * It return an instance of gridView adapter which helps in extracting media details
     * used by the gridView
     * @return  GridView Adapter
     */
    public ListAdapter getAdapter() {
        return binding.bookmarkedPicturesList.getAdapter();
    }

}
