package fr.free.nrw.commons.category;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class CategoryImagesListFragment extends DaggerFragment {

    private GridViewAdapter gridAdapter;

    @BindView(R.id.loadingImagesProgressBar) ProgressBar progressBar;
    @BindView(R.id.categoryImagesList) GridView gridView;

    private boolean isLoading;
    private String categoryName = null;

    @Inject CategoryImageController controller;
    @Inject @Named("category_prefs") SharedPreferences categoryPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_category_images, container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        gridView.setOnItemClickListener((AdapterView.OnItemClickListener) getActivity());
        initViews();
    }

    private void initViews() {
        String categoryName = getArguments().getString("categoryName");
        if (getArguments() != null && categoryName != null) {
            this.categoryName = categoryName;
            resetQueryContinueValues(categoryName);
            initList();
            setScrollListener();
        }
    }

    private void resetQueryContinueValues(String keyword) {
        SharedPreferences.Editor editor = categoryPreferences.edit();
        editor.remove(keyword);
        editor.apply();
    }

    @SuppressLint("CheckResult")
    private void initList() {
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        Observable.fromCallable(() -> controller.getCategoryImages(categoryName))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(featuredImageList -> {
                    isLoading = false;
                    setAdapter(featuredImageList);
                    progressBar.setVisibility(View.GONE);
                }, throwable -> {
                    isLoading = false;
                    Timber.e(throwable, "Error occurred while loading featured images");
                    ViewUtil.showSnackbar(gridView, R.string.error_featured_images);
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void setAdapter(List<Media> mediaList) {
        if (mediaList == null || mediaList.isEmpty()) {
            ViewUtil.showSnackbar(gridView, R.string.no_featured_images);
            return;
        }

        gridAdapter = new GridViewAdapter(this.getContext(), R.layout.layout_category_images, mediaList);
        gridView.setAdapter(gridAdapter);
    }

    private void setScrollListener() {
        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (!isLoading && (firstVisibleItem + visibleItemCount + 1 >= totalItemCount)) {
                    isLoading = true;
                    fetchMoreImages();
                }
            }
        });
    }

    @SuppressLint("CheckResult")
    private void fetchMoreImages() {
        progressBar.setVisibility(View.VISIBLE);
        Observable.fromCallable(() -> controller.getCategoryImages(categoryName))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(collection -> {
                            gridAdapter.addItems(collection);
                            isLoading = false;
                        },
                        throwable -> {
                            isLoading = false;
                            Timber.e(throwable, "Error occurred while loading featured images");
                            ViewUtil.showSnackbar(gridView, R.string.error_featured_images);
                            progressBar.setVisibility(View.GONE);
                        });
    }

    public ListAdapter getAdapter() {
        return gridView.getAdapter();
    }
}
