package fr.free.nrw.commons.leaderboard;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.category.CategoryImageController;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class LeaderboardFragment extends DaggerFragment implements
        AdapterView.OnItemSelectedListener{

    private static int TIMEOUT_SECONDS = 15;
    private leaderboardAdapter gridAdapter;


    String[] duration_array = { "All-time", "Monthly", "Weekly", "Daily"};
    private String duration = null;
    @BindView(R.id.user_rank)
    TextView user_rank;
    @Inject
    SessionManager sessionManager;
    @Inject
    OkHttpJsonApiClient okHttpJsonApiClient;


    @BindView(R.id.statusMessage)
    TextView statusTextView;
    @BindView(R.id.loadingImagesProgressBar)
    ProgressBar progressBar;
    @BindView(R.id.userList)
    RecyclerView recyclerView;
    @BindView(R.id.parentLayout)
    RelativeLayout parentLayout;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private boolean hasMoreImages = true;
    private boolean isLoading = true;
    private String FragmentName = null;

    @Inject
    CategoryImageController controller;
    @Inject
    @Named("default_preferences")
    JsonKvStore categoryKvStore;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_leaderboard, container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
       // initViews();

        //Getting the instance of Spinner and applying OnItemSelectedListener on it
        Spinner spin = (Spinner) view.findViewById(R.id.duration_spinner);
        spin.setOnItemSelectedListener(this);

        //Creating the ArrayAdapter instance having the duration array
        ArrayAdapter aa = new ArrayAdapter(getContext(),android.R.layout.simple_spinner_item,duration_array);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Setting the ArrayAdapter data on the Spinner
        spin.setAdapter(aa);
    //    setUserRank();

        String[] rank = {"1","2","3","4","5","6","7","8","9"};
        String[] name = {"a","b","c","d","e","f","g","h","i"};
        String[] score = {"11","12","13","14","15","16","17","18","19"};
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new leaderboardAdapter(rank,name,score));
    }
//
//    @SuppressLint("CheckResult")
//    private void setUserRank() {
//        String userName = sessionManager.getUserName();
//        if (StringUtils.isBlank(userName)) {
//            return;
//        }
//        compositeDisposable.add(okHttpJsonApiClient.getUserRank(userName,FragmentName,duration)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(edits -> user_rank.setText(user_rank.getText()+" : " + String.valueOf(edits)), e -> {
//                    Timber.e("Error:" + e);
//                }));
//    }

    //Performing action onItemSelected and onNothing selected
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
        duration=duration_array[position];
        //  Toast.makeText(getApplicationContext(),duration_array[position] , Toast.LENGTH_LONG).show();
    }
    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

//    /**
//     * Initializes the UI elements for the fragment
//     * Setup the grid view to and scroll listener for it
//     */
//    private void initViews() {
//        String FragmentName = getArguments().getString("FragmentName");
//        if (getArguments() != null && FragmentName != null) {
//            this.FragmentName = FragmentName;
//            initList();
//            setScrollListener();
//        }
//    }
//
//    /**
//     * Checks for internet connection and then initializes the grid view with first 10 images of that category
//     */
//    @SuppressLint("CheckResult")
//    private void initList() {
//        if (!NetworkUtils.isInternetConnectionEstablished(getContext())) {
//            handleNoInternet();
//            return;
//        }
//
//        isLoading = true;
//        progressBar.setVisibility(VISIBLE);
//        compositeDisposable.add(controller.getCategoryImages("Category:Uploaded_with_Mobile/Android")
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
//                .subscribe(this::handleSuccess, this::handleError));
//    }
//
//    /**
//     * Handles the UI updates for no internet scenario
//     */
//    private void handleNoInternet() {
//        progressBar.setVisibility(GONE);
//        if (gridAdapter == null || gridAdapter.isEmpty()) {
//            statusTextView.setVisibility(VISIBLE);
//            statusTextView.setText(getString(R.string.no_internet));
//        } else {
//            ViewUtil.showShortSnackbar(parentLayout, R.string.no_internet);
//        }
//    }
//
//    /**
//     * Logs and handles API error scenario
//     * @param throwable
//     */
//    private void handleError(Throwable throwable) {
//        Timber.e(throwable, "Error occurred while loading images inside a category");
//        try{
//            ViewUtil.showShortSnackbar(parentLayout, R.string.error_loading_images);
//            initErrorView();
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//    }
//
//    /**
//     * Handles the UI updates for a error scenario
//     */
//    private void initErrorView() {
//        progressBar.setVisibility(GONE);
//        if (gridAdapter == null || gridAdapter.isEmpty()) {
//            statusTextView.setVisibility(VISIBLE);
//            statusTextView.setText(getString(R.string.no_images_found));
//        } else {
//            statusTextView.setVisibility(GONE);
//        }
//    }
//
//    /**
//     * Initializes the adapter with a list of Media objects
//     * @param mediaList List of new Media to be displayed
//     */
//    private void setAdapter(List<Media> mediaList) {
//        gridAdapter = new leaderboardAdapter(this.getContext(), R.layout.layout_leaderboard_item, mediaList);
//        gridView.setAdapter(gridAdapter);
//    }
//
//    /**
//     * Sets the scroll listener for the grid view so that more images are fetched when the user scrolls down
//     * Checks if the category has more images before loading
//     * Also checks whether images are currently being fetched before triggering another request
//     */
//    private void setScrollListener() {
//        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
//            @Override
//            public void onScrollStateChanged(AbsListView view, int scrollState) {
//            }
//
//            @Override
//            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//                if (hasMoreImages && !isLoading && (firstVisibleItem + visibleItemCount + 1 >= totalItemCount)) {
//                    isLoading = true;
//                    fetchMoreImages();
//                }
//                if (!hasMoreImages){
//                    progressBar.setVisibility(GONE);
//                }
//            }
//        });
//    }
//
//    /**
//     * Fetches more images for the category and adds it to the grid view adapter
//     */
//    @SuppressLint("CheckResult")
//    private void fetchMoreImages() {
//        if (!NetworkUtils.isInternetConnectionEstablished(getContext())) {
//            handleNoInternet();
//            return;
//        }
//
//        progressBar.setVisibility(VISIBLE);
//        compositeDisposable.add(controller.getCategoryImages("Category:Uploaded_with_Mobile/Android")
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
//                .subscribe(this::handleSuccess, this::handleError));
//    }
//
//    /**
//     * Handles the success scenario
//     * On first load, it initializes the grid view. On subsequent loads, it adds items to the adapter
//     * @param collection List of new Media to be displayed
//     */
//    private void handleSuccess(List<Media> collection) {
//        if (collection == null || collection.isEmpty()) {
//            initErrorView();
//            hasMoreImages = false;
//            return;
//        }
//
//        if (gridAdapter == null) {
//            setAdapter(collection);
//        } else {
//            if (gridAdapter.containsAll(collection)) {
//                hasMoreImages = false;
//                return;
//            }
//            gridAdapter.addItems(collection);
//        }
//        progressBar.setVisibility(GONE);
//        isLoading = false;
//        statusTextView.setVisibility(GONE);
//    }
//
//    /**
//     * It return an instance of gridView adapter which helps in extracting media details
//     * used by the gridView
//     * @return  GridView Adapter
//     */
//    public ListAdapter getAdapter() {
//        return gridAdapter;
//    }
}
