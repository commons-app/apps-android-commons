package fr.free.nrw.commons.contributions;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionsListAdapter.Callback;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

/**
 * Created by root on 01.06.2018.
 */

public class ContributionsListFragment extends CommonsDaggerSupportFragment implements
    ContributionsListContract.View {

    private static final int PAGE_SIZE = 10;

    private static final String VISIBLE_ITEM_ID = "visible_item_id";
    @BindView(R.id.contributionsList)
    RecyclerView rvContributionsList;
    @BindView(R.id.loadingContributionsProgressBar)
    ProgressBar progressBar;
    @BindView(R.id.fab_plus)
    FloatingActionButton fabPlus;
    @BindView(R.id.fab_camera)
    FloatingActionButton fabCamera;
    @BindView(R.id.fab_gallery)
    FloatingActionButton fabGallery;
    @BindView(R.id.noContributionsYet)
    TextView noContributionsYet;
    @BindView(R.id.fab_layout)
    LinearLayout fab_layout;

    @Inject @Named("default_preferences") JsonKvStore kvStore;
    @Inject ContributionController controller;

    @Inject
    ContributionsListPresenter contributionsListPresenter;

    private Animation fab_close;
    private Animation fab_open;
    private Animation rotate_forward;
    private Animation rotate_backward;


    private boolean isFabOpen = false;

    private boolean isLoading = false;
    private boolean isLastPage = false;

    private ContributionsListAdapter adapter;

    private Callback callback;
    private String lastVisibleItemID;

    private int SPAN_COUNT=3;
    private List<Contribution> contributions=new ArrayList<>();

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contributions_list, container, false);
        ButterKnife.bind(this, view);
        Timber.d("RecyclerList Inside onCreateView.");
        contributionsListPresenter.onAttachView(this);
        contributionsListPresenter.setLifeCycleOwner(this.getViewLifecycleOwner());
        initAdapter();
        return view;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    private void initAdapter() {
        adapter = new ContributionsListAdapter(callback);
        adapter.setHasStableIds(true);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initRecyclerView();
        initializeAnimations();
        setListeners();
    }

    private void initRecyclerView() {
        Timber.d("RecyclerList Recycler view Init.");
        LinearLayoutManager layoutManager;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            layoutManager = new GridLayoutManager(getContext(), SPAN_COUNT);
            rvContributionsList.setLayoutManager(layoutManager);
        } else {
            layoutManager = new LinearLayoutManager(getContext());
            rvContributionsList.setLayoutManager(layoutManager);
        }

        rvContributionsList.setAdapter(adapter);
        adapter.setContributions(contributions);
        rvContributionsList.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= PAGE_SIZE) {
                        loadMoreItems();
                    }
                }
            }
        });

      contributionsListPresenter.setupLiveData();
      contributionsListPresenter.fetchContributions();
    }

    private void loadMoreItems() {
        Timber.d("RecyclerList Inside load more items.");
        isLoading = true;
        contributionsListPresenter.fetchContributions();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // check orientation
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            fab_layout.setOrientation(LinearLayout.HORIZONTAL);
            rvContributionsList.setLayoutManager(new GridLayoutManager(getContext(),SPAN_COUNT));
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            fab_layout.setOrientation(LinearLayout.VERTICAL);
            rvContributionsList.setLayoutManager(new LinearLayoutManager(getContext()));
        }
    }

    private void initializeAnimations() {
        fab_open = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_backward);
    }

    private void setListeners() {
        fabPlus.setOnClickListener(view -> animateFAB(isFabOpen));
        fabCamera.setOnClickListener(view -> {
            controller.initiateCameraPick(getActivity());
            animateFAB(isFabOpen);
        });
        fabGallery.setOnClickListener(view -> {
            controller.initiateGalleryPick(getActivity(), true);
            animateFAB(isFabOpen);
        });
    }

    private void animateFAB(boolean isFabOpen) {
        this.isFabOpen = !isFabOpen;
        if (fabPlus.isShown()){
            if (isFabOpen) {
                fabPlus.startAnimation(rotate_backward);
                fabCamera.startAnimation(fab_close);
                fabGallery.startAnimation(fab_close);
                fabCamera.hide();
                fabGallery.hide();
            } else {
                fabPlus.startAnimation(rotate_forward);
                fabCamera.startAnimation(fab_open);
                fabGallery.startAnimation(fab_open);
                fabCamera.show();
                fabGallery.show();
            }
            this.isFabOpen=!isFabOpen;
        }
    }

    /**
     * Shows welcome message if user has no contributions yet i.e. new user.
     */
    public void showWelcomeTip(boolean shouldShow) {
        noContributionsYet.setVisibility(shouldShow ? VISIBLE : GONE);
    }

    /**
     * Responsible to set progress bar invisible and visible
     *
     * @param shouldShow True when contributions list should be hidden.
     */
    public void showProgress(boolean shouldShow) {
        progressBar.setVisibility(shouldShow ? VISIBLE : GONE);
    }

    public void showNoContributionsUI(boolean shouldShow) {
        noContributionsYet.setVisibility(shouldShow ? VISIBLE : GONE);
    }

    @Override
    public void setUploadCount(int count) {

    }

    @Override
    public void showContributions(List<Contribution> contributionList) {

    }

    @Override
    public void showMessage(String localizedMessage) {

    }

    public void setContributions(List<Contribution> contributionList) {
        this.contributions.clear();
        this.contributions.addAll(contributionList);
        adapter.setContributions(contributions);
    }

    public interface SourceRefresher {
        void refreshSource();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        LayoutManager layoutManager = rvContributionsList.getLayoutManager();
        int lastVisibleItemPosition=0;
        if(layoutManager instanceof  LinearLayoutManager){
            lastVisibleItemPosition= ((LinearLayoutManager) layoutManager).findLastCompletelyVisibleItemPosition();
        }else if(layoutManager instanceof GridLayoutManager){
            lastVisibleItemPosition=((GridLayoutManager)layoutManager).findLastCompletelyVisibleItemPosition();
        }
        String idOfItemWithPosition = findIdOfItemWithPosition(lastVisibleItemPosition);
        if (null != idOfItemWithPosition) {
            outState.putString(VISIBLE_ITEM_ID, idOfItemWithPosition);
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if(null!=savedInstanceState){
            lastVisibleItemID =savedInstanceState.getString(VISIBLE_ITEM_ID, null);
        }
    }


    /**
     * Gets the id of the contribution from the db
     * @param position
     * @return
     */
    @Nullable
    private String findIdOfItemWithPosition(int position) {
        Contribution contributionForPosition = callback.getContributionForPosition(position);
        if (null != contributionForPosition) {
            return contributionForPosition.getFilename();
        }
        return null;
    }

}
