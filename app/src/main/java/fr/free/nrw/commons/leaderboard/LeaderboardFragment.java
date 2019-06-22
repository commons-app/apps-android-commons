package fr.free.nrw.commons.leaderboard;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.leaderboard.Utils.PaginationScrollListener;
import fr.free.nrw.commons.leaderboard.model.GetLeaderboardUser;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class LeaderboardFragment extends DaggerFragment implements
        AdapterView.OnItemSelectedListener{

    private leaderboardAdapter adapter;
    private static final int PAGE_START = 0;
    private boolean isLoading = false;
    private int currentPage = PAGE_START;
    private String[] duration_array = { "All-time", "Monthly", "Weekly", "Daily"};
    private String duration = null;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private String FragmentName = null;
    private LinearLayoutManager linearLayoutManager;

    @BindView(R.id.user_rank)
    TextView user_rank;
    @BindView(R.id.statusMessage)
    TextView statusTextView;
    @BindView(R.id.loadingImagesProgressBar)
    ProgressBar progressBar;
    @BindView(R.id.userList)
    RecyclerView recyclerView;
    @BindView(R.id.parentLayout)
    RelativeLayout parentLayout;
    @Inject
    SessionManager sessionManager;
    @Inject
    OkHttpJsonApiClient okHttpJsonApiClient;
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

        //Getting the instance of Spinner and applying OnItemSelectedListener on it
        //Creating the ArrayAdapter instance having the duration array
        Spinner spin = (Spinner) view.findViewById(R.id.duration_spinner);
        spin.setOnItemSelectedListener(this);
        ArrayAdapter aa = new ArrayAdapter(getContext(),android.R.layout.simple_spinner_item,duration_array);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(aa);

        setUserRank();

        adapter = new leaderboardAdapter(getContext());
        linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new PaginationScrollListener(linearLayoutManager) {
            @Override
            protected void loadMoreItems() {
                isLoading = true;
                currentPage += 1;

                // mocking network delay for API call
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadNextPage();
                    }
                }, 1000);
            }

            @Override
            public boolean isLoading() {
                return isLoading;
            }
        });


        // mocking network delay for API call
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loadFirstPage();
            }
        }, 1000);

    }

    private void loadFirstPage() {
        List<GetLeaderboardUser> getLeaderboardUsers = GetLeaderboardUser.createMovies(adapter.getItemCount());
        progressBar.setVisibility(View.GONE);
        adapter.addAll(getLeaderboardUsers);

        adapter.addLoadingFooter();
    }

    private void loadNextPage() {
        List<GetLeaderboardUser> getLeaderboardUsers = GetLeaderboardUser.createMovies(adapter.getItemCount());
        adapter.removeLoadingFooter();
        isLoading = false;
        adapter.addAll(getLeaderboardUsers);

       adapter.addLoadingFooter();
    }

    @SuppressLint("CheckResult")
    private void setUserRank() {
        String userName = sessionManager.getUserName();
        if (StringUtils.isBlank(userName)) {
            return;
        }
        compositeDisposable.add(okHttpJsonApiClient.getUserRank(userName,FragmentName,duration)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(rank -> user_rank.setText(user_rank.getText()+" : " + String.valueOf(rank)), e -> {
                    Timber.e("Error:" + e);
                }));
    }

    //Performing action onItemSelected and onNothing selected
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
        duration=duration_array[position];
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
//     * Handles the UI updates for no internet scenario
//     */
//    private void handleNoInternet() {
//        progressBar.setVisibility(GONE);
//        if (adapter == null || adapter.isEmpty()) {
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
//        if (adapter == null || adapter.isEmpty()) {
//            statusTextView.setVisibility(VISIBLE);
//            statusTextView.setText(getString(R.string.no_images_found));
//        } else {
//            statusTextView.setVisibility(GONE);
//        }
//    }

//    /**
//     * Handles the success scenario
//     * On first load, it initializes the recycler view. On subsequent loads, it adds items to the adapter
//     * @param collection List of new userList to be displayed
//     */
//    private void handleSuccess(List<GetLeaderboardUser> collection) {
//        if (collection == null || collection.isEmpty()) {
//            initErrorView();
//            hasMoreImages = false;
//            return;
//        }
//        if(adapter==null){
//            progressBar.setVisibility(View.GONE);
//        } else {
//            progressBar.setVisibility(View.GONE);
//            adapter.removeLoadingFooter();
//            isLoading = false;
//        }
//        adapter.addAll(collection);
//    }

}
