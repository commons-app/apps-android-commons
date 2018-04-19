package fr.free.nrw.commons.featured;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class FeaturedImagesListFragment extends DaggerFragment {
    private GridViewAdapter gridAdapter;

    @BindView(R.id.loadingFeaturedImagesProgressBar)
    ProgressBar progressBar;
    @BindView(R.id.featuredImagesList)
    GridView gridView;

    @Inject
    FeaturedImageController controller;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_featured_images, container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        gridView.setOnItemClickListener((AdapterView.OnItemClickListener) getActivity());
        initList();
    }

    @SuppressLint("CheckResult")
    private void initList() {
        Observable.fromCallable(() -> {
            progressBar.setVisibility(View.VISIBLE);
            return controller.getFeaturedImages();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(featuredImageList -> {
                    Timber.d("Number of featured images is %d", featuredImageList.size());
                    setAdapter(featuredImageList);
                    progressBar.setVisibility(View.GONE);
                }, throwable -> {
                    Timber.e(throwable, "Error occurred while loading featured images");
                    ViewUtil.showSnackbar(gridView, R.string.error_featured_images);
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void setAdapter(List<FeaturedImage> featuredImageList) {
        if (featuredImageList == null || featuredImageList.isEmpty()) {
            ViewUtil.showSnackbar(gridView, R.string.no_featured_images);
            return;
        }

        gridAdapter = new GridViewAdapter(this.getContext(), R.layout.layout_featured_images, featuredImageList);
        gridView.setAdapter(gridAdapter);
    }

    public ListAdapter getAdapter() {
        return gridView.getAdapter();
    }
}
