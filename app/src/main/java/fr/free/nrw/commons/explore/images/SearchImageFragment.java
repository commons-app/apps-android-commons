package fr.free.nrw.commons.explore.images;


import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.pedrogomez.renderers.RVRendererAdapter;
import java.util.ArrayList;
import java.util.Date;
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
import fr.free.nrw.commons.explore.recentsearches.RecentSearch;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao;
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

    @Inject RecentSearchesDao recentSearchesDao;
    @Inject MediaWikiApi mwApi;
    @Inject @Named("default_preferences") SharedPreferences prefs;

    private RVRendererAdapter<Media> imagesAdapter;
    private List<Media> queryList = new ArrayList<>();

    private final SearchImagesAdapterFactory adapterFactory = new SearchImagesAdapterFactory(item -> {
        // Called on Click of a individual media Item
        int index = queryList.indexOf(item);
        ((SearchActivity)getContext()).onSearchImageClicked(index);
        saveQuery(query);
    });

    /**
     * This method saves Search Query in the Recent Searches Database.
     * @param query
     */
    private void saveQuery(String query) {
        RecentSearch recentSearch = recentSearchesDao.find(query);

        // Newly searched query...
        if (recentSearch == null) {
            recentSearch = new RecentSearch(null, query, new Date());
        }
        else {
            recentSearch.setLastSearched(new Date());
        }

        recentSearchesDao.save(recentSearch);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_browse_image, container, false);
        ButterKnife.bind(this, rootView);
        if(getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            imagesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        else{
            imagesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        }
        ArrayList<Media> items = new ArrayList<>();
        imagesAdapter = adapterFactory.create(items);
        imagesRecyclerView.setAdapter(imagesAdapter);
        imagesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // check if end of recycler view is reached, if yes then add more results to existing results
                if (!recyclerView.canScrollVertically(1)) {
                    addImagesToList(query);
                }
            }
        });
        return rootView;
    }

    /**
     * Checks for internet connection and then initializes the recycler view with 25 images of the searched query
     * Clearing imageAdapter every time new keyword is searched so that user can see only new results
     */
    public void updateImageList(String query) {
        this.query = query;
        imagesNotFoundView.setVisibility(GONE);
        if(!NetworkUtils.isInternetConnectionEstablished(getContext())) {
            handleNoInternet();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        queryList.clear();
        imagesAdapter.clear();
        Observable.fromCallable(() -> mwApi.searchImages(query,queryList.size()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .subscribe(this::handleSuccess, this::handleError);
    }


    /**
     * Adds more results to existing search results
     */
    public void addImagesToList(String query) {
        this.query = query;
        progressBar.setVisibility(View.VISIBLE);
        Observable.fromCallable(() -> mwApi.searchImages(query,queryList.size()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .subscribe(this::handlePaginationSuccess, this::handleError);
    }

    /**
     * Handles the success scenario
     * it initializes the recycler view by adding items to the adapter
     * @param mediaList List of media to be added
     */
    private void handlePaginationSuccess(List<Media> mediaList) {
        queryList.addAll(mediaList);
        progressBar.setVisibility(View.GONE);
        imagesAdapter.addAll(mediaList);
        imagesAdapter.notifyDataSetChanged();
    }



    /**
     * Handles the success scenario
     * it initializes the recycler view by adding items to the adapter
     * @param mediaList List of media to be shown
     */
    private void handleSuccess(List<Media> mediaList) {
        queryList = mediaList;
        if(mediaList == null || mediaList.isEmpty()) {
            initErrorView();
        }
        else {

            progressBar.setVisibility(View.GONE);
            imagesAdapter.addAll(mediaList);
            imagesAdapter.notifyDataSetChanged();

            // check if user is waiting for 5 seconds if yes then save search query to history.
            Handler handler = new Handler();
            handler.postDelayed(() -> saveQuery(query), 5000);
        }
    }

    /**
     * Logs and handles API error scenario
     * @param throwable
     */
    private void handleError(Throwable throwable) {
        Timber.e(throwable, "Error occurred while loading queried images");
        try {
            initErrorView();
            ViewUtil.showSnackbar(imagesRecyclerView, R.string.error_loading_images);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Handles the UI updates for a error scenario
     */
    private void initErrorView() {
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
        }
        else {
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
        }
        else {
            return new Media(imagesAdapter.getItem(i).getFilename());
        }
    }
}
