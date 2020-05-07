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
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.media.MediaClient;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

/**
 * Created by root on 01.06.2018.
 */

public class ContributionsListFragment extends CommonsDaggerSupportFragment implements
    ContributionsListContract.View, ContributionsListAdapter.Callback {

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

    @Inject ContributionController controller;
  @Inject
  MediaClient mediaClient;

    @Inject
    ContributionsListPresenter contributionsListPresenter;

    private MediaDetailPagerFragment mediaDetailPagerFragment;

    private Animation fab_close;
    private Animation fab_open;
    private Animation rotate_forward;
    private Animation rotate_backward;


    private boolean isFabOpen;

    private ContributionsListAdapter adapter;

    private Callback callback;

  private int SPAN_COUNT_LANDSCAPE =3;
  private int SPAN_COUNT_PORTRAIT =1;

    ContributionsListFragment(Callback callback) {
        this.callback = callback;
    }

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contributions_list, container, false);
        ButterKnife.bind(this, view);
        contributionsListPresenter.onAttachView(this);
        contributionsListPresenter.setLifeCycleOwner(getViewLifecycleOwner());
        initAdapter();
        return view;
    }

    private void initAdapter() {
      adapter = new ContributionsListAdapter(this, mediaClient);
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
      GridLayoutManager layoutManager = new GridLayoutManager(getContext(),
          getSpanCount(getResources().getConfiguration().orientation));
      rvContributionsList.setLayoutManager(layoutManager);

        rvContributionsList.setAdapter(adapter);
        rvContributionsList.addOnScrollListener(contributionsListPresenter
            .getScrollListener(layoutManager, getContext()));

      contributionsListPresenter.setupLiveData();
      contributionsListPresenter.fetchContributions(getContext());
    }

    private int getSpanCount(int orientation) {
      return orientation == Configuration.ORIENTATION_LANDSCAPE?
          SPAN_COUNT_LANDSCAPE: SPAN_COUNT_PORTRAIT;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
      // check orientation
      fab_layout.setOrientation(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE?
          LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
      rvContributionsList
          .setLayoutManager(new GridLayoutManager(getContext(), getSpanCount(newConfig.orientation)));
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
    public void showProgress(final boolean shouldShow) {
        progressBar.setVisibility(shouldShow ? VISIBLE : GONE);
    }

    public void showNoContributionsUI(boolean shouldShow) {
        noContributionsYet.setVisibility(shouldShow ? VISIBLE : GONE);
    }

    @Override
    public void showContributions(final List<Contribution> contributionList) {
        adapter.setContributions(contributionList);
    }

    @Override
    public void retryUpload(Contribution contribution) {
        callback.retryUpload(contribution);
    }

    @Override
    public void deleteUpload(Contribution contribution) {
        contributionsListPresenter.deleteUpload(contribution);
    }

    @Override
    public void openMediaDetail(int position) {
        callback.showDetail(position);
    }

    public Media getMediaAtPosition(int i) {
        return adapter.getContributionForPosition(i);
    }

    public int getTotalMediaCount() {
        return adapter.getItemCount();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        LayoutManager layoutManager = rvContributionsList.getLayoutManager();
        int lastVisibleItemPosition= ((GridLayoutManager)layoutManager).findLastCompletelyVisibleItemPosition();;
        String idOfItemWithPosition = findIdOfItemWithPosition(lastVisibleItemPosition);
        if (null != idOfItemWithPosition) {
            outState.putString(VISIBLE_ITEM_ID, idOfItemWithPosition);
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if(null!=savedInstanceState){
        }
    }

    /**
     * Gets the id of the contribution from the db
     * @param position
     * @return
     */
    @Nullable
    private String findIdOfItemWithPosition(int position) {
        Contribution contributionForPosition = adapter.getContributionForPosition(position);
        if (null != contributionForPosition) {
            return contributionForPosition.getFilename();
        }
        return null;
    }

    public interface Callback {
        void retryUpload(Contribution contribution);
        void showDetail(int position);
    }
}
