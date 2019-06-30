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
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionsListAdapter.Callback;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by root on 01.06.2018.
 */

public class ContributionsListFragment extends CommonsDaggerSupportFragment {

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

    private Animation fab_close;
    private Animation fab_open;
    private Animation rotate_forward;
    private Animation rotate_backward;


    private boolean isFabOpen = false;

    private LinearLayoutManager linearLayoutManager;
    private GridLayoutManager gridLayoutManager;

    private ContributionsListAdapter adapter;

    private Callback callback;

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contributions_list, container, false);
        ButterKnife.bind(this, view);
        linearLayoutManager=new LinearLayoutManager(getContext());
        gridLayoutManager=new GridLayoutManager(getContext(),3);
        initAdapter();
        return view;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    private void initAdapter() {
        adapter = new ContributionsListAdapter(callback);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initRecyclerView();
        initializeAnimations();
        setListeners();
    }

    private void initRecyclerView() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            rvContributionsList.setLayoutManager(gridLayoutManager);
        } else {
            rvContributionsList.setLayoutManager(linearLayoutManager);
        }

        rvContributionsList.setAdapter(adapter);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // check orientation
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            fab_layout.setOrientation(LinearLayout.HORIZONTAL);
            rvContributionsList.setLayoutManager(gridLayoutManager);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            fab_layout.setOrientation(LinearLayout.VERTICAL);
            rvContributionsList.setLayoutManager(linearLayoutManager);
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
     * @param shouldShow True when contributions list should be hidden.
     */
    public void showProgress(boolean shouldShow) {
        progressBar.setVisibility(shouldShow ? VISIBLE : GONE);
    }

    public void showNoContributionsUI(boolean shouldShow) {
        noContributionsYet.setVisibility(shouldShow?VISIBLE:GONE);
    }

    public void onDataSetChanged() {
        if(null!=adapter) {
            adapter.notifyDataSetChanged();
        }
    }

    public void onDataSetInvalidated() {
        //Doing nothing as of now
    }

    public interface SourceRefresher {
        void refreshSource();
    }
}
