package fr.free.nrw.commons.explore.depictions;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pedrogomez.renderers.RVRendererAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.recentsearches.RecentSearch;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.upload.depicts.DepictsInterface;
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class SearchDepictionsFragment  extends CommonsDaggerSupportFragment {
    private static int TIMEOUT_SECONDS = 15;
    @BindView(R.id.imagesListBox)
    RecyclerView depictionsRecyclerView;
    @BindView(R.id.imageSearchInProgress)
    ProgressBar progressBar;
    @BindView(R.id.imagesNotFound)
    TextView depictionNotFound;
    String query;
    @BindView(R.id.bottomProgressBar)
    ProgressBar bottomProgressBar;
    boolean isLoadingDepictions;

    @Inject
    RecentSearchesDao recentSearchesDao;
    @Inject
    MediaWikiApi mwApi;
    @Inject
    DepictsClient depictsClient;

    @Inject
    @Named("default_preferences")
    JsonKvStore basicKvStore;

    private RVRendererAdapter<DepictedItem> depictionsAdapter;
    private List<DepictedItem> queryList = new ArrayList<>();

    private final SearchDepictionsAdapterFactory adapterFactory = new SearchDepictionsAdapterFactory(item -> {
        //todo GSoC'19 this should open url of the depiction
    });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_browse_image, container, false);
        ButterKnife.bind(this, rootView);
        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            depictionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        else{
            depictionsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        }
        ArrayList<DepictedItem> items = new ArrayList<>();
        depictionsAdapter = adapterFactory.create(items);
        depictionsRecyclerView.setAdapter(depictionsAdapter);
        depictionsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // check if end of recycler view is reached, if yes then add more results to existing results
                if (!recyclerView.canScrollVertically(1)) {
                    addDepictionsToList(query);
                }
            }
        });
        return rootView;
    }

    private void addDepictionsToList(String query) {
        if(isLoadingDepictions) return;
        isLoadingDepictions=true;
        this.query = query;
        bottomProgressBar.setVisibility(View.VISIBLE);
        progressBar.setVisibility(GONE);
        compositeDisposable.add(depictsClient.searchForDepictions(query,25)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .collect(ArrayList<DepictedItem>::new, ArrayList::add)
                .subscribe(this::handlePaginationSuccess, this::handleError));
    }

    public void updateDepictionList(String query) {
        this.query = query;
        depictionNotFound.setVisibility(GONE);
        if (!NetworkUtils.isInternetConnectionEstablished(getContext())) {
            handleNoInternet();
            return;
        }
        bottomProgressBar.setVisibility(View.VISIBLE);
        progressBar.setVisibility(GONE);
        queryList.clear();
        depictionsAdapter.clear();
        saveQuery(query);
        compositeDisposable.add(depictsClient.searchForDepictions(query, 25)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .doOnSubscribe(disposable -> saveQuery(query))
                .collect(ArrayList<DepictedItem>::new, ArrayList::add)
                .subscribe(this::handleSuccess, this::handleError));
    }

    private void handleSuccess(List<DepictedItem> mediaList) {
        queryList = mediaList;
        if (mediaList == null || mediaList.isEmpty()) {
            initErrorView();
        }
        else {
            bottomProgressBar.setVisibility(View.GONE);
            progressBar.setVisibility(GONE);
            depictionsAdapter.addAll(mediaList);
            depictionsAdapter.notifyDataSetChanged();
        }
    }

    private void handlePaginationSuccess(List<DepictedItem> mediaList) {
        queryList.addAll(mediaList);
        progressBar.setVisibility(View.GONE);
        bottomProgressBar.setVisibility(GONE);
        depictionsAdapter.addAll(mediaList);
        depictionsAdapter.notifyDataSetChanged();
        isLoadingDepictions=false;
    }

    private void handleError(Throwable throwable) {
        Timber.e(throwable, "Error occurred while loading queried depictions");
        try {
            initErrorView();
            ViewUtil.showShortSnackbar(depictionsRecyclerView, R.string.error_loading_depictions);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void initErrorView() {
        progressBar.setVisibility(GONE);
        depictionNotFound.setVisibility(VISIBLE);
        depictionNotFound.setText(getString(R.string.depictions_not_found));
    }

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

    private void handleNoInternet() {
        progressBar.setVisibility(GONE);
        ViewUtil.showShortSnackbar(depictionsRecyclerView, R.string.no_internet);
    }
}
