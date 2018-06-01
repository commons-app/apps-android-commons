package fr.free.nrw.commons.explore.images;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.pedrogomez.renderers.RVRendererAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.SearchActivity;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Displays the image search screen.
 */

public class SearchImageFragment extends CommonsDaggerSupportFragment {

    private static int TIMEOUT_SECONDS = 15;

    @BindView(R.id.imagesListBox)
    RecyclerView imagesRecyclerView;
    @BindView(R.id.imageSearchInProgress)
    ProgressBar progressBar;
    @BindView(R.id.imagesNotFound)
    TextView imagesNotFoundView;
    String query;

    @Inject
    MediaWikiApi mwApi;
    @Inject @Named("default_preferences") SharedPreferences prefs;

    private RVRendererAdapter<Media> imagesAdapter;
    private List<Media> queryList = new ArrayList<>();

    private final SearchImagesAdapterFactory adapterFactory = new SearchImagesAdapterFactory(item -> {
        int index = queryList.indexOf(item);
        ((SearchActivity)getContext()).onSearchImageClicked(index);
        //TODO : Add images to recently searched images db table
    });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_browse_image, container, false);
        ButterKnife.bind(this, rootView);
        imagesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ArrayList<Media> items = new ArrayList<>();
        imagesAdapter = adapterFactory.create(items);
        imagesRecyclerView.setAdapter(imagesAdapter);
        return rootView;
    }

    /**
     * Checks for internet connection and then initializes the recycler view with 25 images of the searched query
     * Clearing imageAdapter every time new keyword is searched so that user can see only new results
     */
    public void updateImageList(String query) {
        this.query = query;
        if(!NetworkUtils.isInternetConnectionEstablished(getContext())) {
            handleNoInternet();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        queryList.clear();
        imagesAdapter.clear();
        Observable.fromCallable(() -> mwApi.searchImages(query))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .subscribe(this::handleSuccess, this::handleError);
    }

    /**
     * Handles the success scenario
     * it initializes the recycler view by adding items to the adapter
     * @param mediaList
     */
    private void handleSuccess(List<Media> mediaList) {
        queryList = mediaList;
        if(mediaList == null || mediaList.isEmpty()) {
            initErrorView();
        }else {

            progressBar.setVisibility(View.GONE);
            imagesAdapter.addAll(mediaList);
            imagesAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Logs and handles API error scenario
     * @param throwable
     */
    private void handleError(Throwable throwable) {
        Timber.e(throwable, "Error occurred while loading queried images");
        initErrorView();
    }

    /**
     * Handles the UI updates for a error scenario
     */
    private void initErrorView() {
        ViewUtil.showSnackbar(imagesRecyclerView, R.string.error_loading_images);
        progressBar.setVisibility(GONE);
        imagesNotFoundView.setVisibility(VISIBLE);
        imagesNotFoundView.setText(getString(R.string.images_not_found, query));
    }

    /**
     * Handles the UI updates for no internet scenario
     */
    private void handleNoInternet() {
        progressBar.setVisibility(GONE);
        ViewUtil.showSnackbar(imagesRecyclerView, R.string.no_internet);
    }

    /**
    * returns total number of images present in the recyclerview adapter.
    */
    public int getTotalImagesCount(){
        if (imagesAdapter == null) {
            return 0;
        }else {
            return imagesAdapter.getItemCount();
        }
    }

    /**
     * returns Media Object at position
     * @param i position of Media in the recyclerview adapter.
     */
    public Media getImageAtPosition(int i) {
        if (imagesAdapter.getItem(i).getFilename() == null) {
            // not yet ready to return data
            return null;
        } else {
            return new Media(imagesAdapter.getItem(i).getFilename());
        }
    }
}
