package fr.free.nrw.commons.bookmarks.pictures;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.bookmarks.BookmarksActivity;
import fr.free.nrw.commons.category.GridViewAdapter;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class BookmarkPicturesFragment extends DaggerFragment {

    private static final int TIMEOUT_SECONDS = 15;

    private GridViewAdapter gridAdapter;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @BindView(R.id.statusMessage) TextView statusTextView;
    @BindView(R.id.loadingImagesProgressBar) ProgressBar progressBar;
    @BindView(R.id.bookmarkedPicturesList) GridView gridView;
    @BindView(R.id.parentLayout) RelativeLayout parentLayout;

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
        View v = inflater.inflate(R.layout.fragment_bookmarks_pictures, container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        gridView.setOnItemClickListener((AdapterView.OnItemClickListener) getActivity());
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
    }

    @Override
    public void onResume() {
        super.onResume();
        if (controller.needRefreshBookmarkedPictures()) {
            gridView.setVisibility(GONE);
            if (gridAdapter != null) {
                gridAdapter.clear();
                try {
                    ((BookmarksActivity) getContext()).viewPagerNotifyDataSetChanged();
                }catch (Exception e){
                    e.printStackTrace();
                }
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

        progressBar.setVisibility(VISIBLE);
        statusTextView.setVisibility(GONE);

        compositeDisposable.add(controller.loadBookmarkedPictures()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .subscribe(this::handleSuccess, this::handleError));
    }

    /**
     * Handles the UI updates for no internet scenario
     */
    private void handleNoInternet() {
        progressBar.setVisibility(GONE);
        if (gridAdapter == null || gridAdapter.isEmpty()) {
            statusTextView.setVisibility(VISIBLE);
            statusTextView.setText(getString(R.string.no_internet));
        } else {
            ViewUtil.showShortSnackbar(parentLayout, R.string.no_internet);
        }
    }

    /**
     * Logs and handles API error scenario
     * @param throwable
     */
    private void handleError(Throwable throwable) {
        Timber.e(throwable, "Error occurred while loading images inside a category");
        try{
            ViewUtil.showShortSnackbar(parentLayout, R.string.error_loading_images);
            initErrorView();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Handles the UI updates for a error scenario
     */
    private void initErrorView() {
        progressBar.setVisibility(GONE);
        if (gridAdapter == null || gridAdapter.isEmpty()) {
            statusTextView.setVisibility(VISIBLE);
            statusTextView.setText(getString(R.string.no_images_found));
        } else {
            statusTextView.setVisibility(GONE);
        }
    }

    /**
     * Handles the UI updates when there is no bookmarks
     */
    private void initEmptyBookmarkListView() {
        progressBar.setVisibility(GONE);
        if (gridAdapter == null || gridAdapter.isEmpty()) {
            statusTextView.setVisibility(VISIBLE);
            statusTextView.setText(getString(R.string.bookmark_empty));
        } else {
            statusTextView.setVisibility(GONE);
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
                return;
            }
            gridAdapter.addItems(collection);
            try {
                ((BookmarksActivity) getContext()).viewPagerNotifyDataSetChanged();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        progressBar.setVisibility(GONE);
        statusTextView.setVisibility(GONE);
        gridView.setVisibility(VISIBLE);
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
        gridView.setAdapter(gridAdapter);
    }

    /**
     * It return an instance of gridView adapter which helps in extracting media details
     * used by the gridView
     * @return  GridView Adapter
     */
    public ListAdapter getAdapter() {
        return gridView.getAdapter();
    }
}
