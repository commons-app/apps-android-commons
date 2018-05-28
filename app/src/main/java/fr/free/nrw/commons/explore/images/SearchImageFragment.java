package fr.free.nrw.commons.explore.images;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pedrogomez.renderers.RVRendererAdapter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.SearchActivity;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Displays the image search screen.
 */

public class SearchImageFragment extends CommonsDaggerSupportFragment {

    public static final int SEARCH_IMAGES_LIMIT = 25;

    @BindView(R.id.imagesListBox)
    RecyclerView imagesList;
    @BindView(R.id.imageSearchInProgress)
    ProgressBar imageSearchInProgress;
    @BindView(R.id.imagesNotFound)
    TextView imagesNotFoundView;

    @Inject
    MediaWikiApi mwApi;
    @Inject @Named("default_preferences") SharedPreferences prefs;

    private RVRendererAdapter<SearchImageItem> imagesAdapter;
    private List<SearchImageItem> images=new ArrayList<>();
    private List<SearchImageItem> queryList = new ArrayList<>();

    private final SearchImagesAdapterFactory adapterFactory = new SearchImagesAdapterFactory(item -> {
        int index = queryList.indexOf(item);
        ((SearchActivity)getContext()).onSearchImageClicked(index);
        //TODO : Add images to recently searched images db table
    });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_browse_image, container, false);
        ButterKnife.bind(this, rootView);

        imagesList.setLayoutManager(new LinearLayoutManager(getContext()));

        ArrayList<SearchImageItem> items = new ArrayList<>();

        imagesAdapter = adapterFactory.create(items);
        imagesList.setAdapter(imagesAdapter);

        return rootView;
    }

    public void updateImageList(String query) {
        queryList.clear();
        Observable.fromIterable(images)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> {
                    imageSearchInProgress.setVisibility(View.VISIBLE);
                    imagesNotFoundView.setVisibility(View.GONE);
                    imagesAdapter.clear();
                })
                .observeOn(Schedulers.io())
                .concatWith(
                        searchImages(query)
                )
                .distinct()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        s ->{
                            imagesAdapter.add(s);
                            queryList.add(s);
                        },
                        Timber::e,
                        () -> {
                            imagesAdapter.notifyDataSetChanged();
                            imageSearchInProgress.setVisibility(View.GONE);

                            if (imagesAdapter.getItemCount() == images.size()) {
                                if (TextUtils.isEmpty(query)) {
                                    imagesAdapter.clear();

                                } else {
                                    imagesNotFoundView.setText(getString(R.string.images_not_found, query));
                                    imagesNotFoundView.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                );
    }

    private Observable<SearchImageItem> searchImages(String query) {
        return mwApi.searchImages(query, SEARCH_IMAGES_LIMIT)
            .map(s -> new SearchImageItem(s));
    }

    public int getTotalImagesCount(){
        if (imagesAdapter == null) {
            return 0;
        }else {
            return imagesAdapter.getItemCount();
        }
    }

    public Media getImageAtPosition(int i) {
        if (imagesAdapter.getItem(i).getName() == null) {
            // not yet ready to return data
            return null;
        } else {
            return new Media(imagesAdapter.getItem(i).getName());
        }
    }
}
