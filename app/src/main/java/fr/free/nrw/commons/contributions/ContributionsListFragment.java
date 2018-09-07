package fr.free.nrw.commons.contributions;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;

import static android.view.View.GONE;

/**
 * Created by root on 01.06.2018.
 */

public class ContributionsListFragment extends CommonsDaggerSupportFragment {

    @BindView(R.id.contributionsList)
    GridView contributionsList;
    @BindView(R.id.waitingMessage)
    TextView waitingMessage;
    @BindView(R.id.loadingContributionsProgressBar)
    ProgressBar progressBar;
    @BindView(R.id.fab_plus)
    FloatingActionButton fabPlus;
    @BindView(R.id.fab_camera)
    FloatingActionButton fabCamera;
    @BindView(R.id.fab_galery)
    FloatingActionButton fabGalery;

    private Animation fab_close;
    private Animation fab_open;
    private Animation rotate_forward;
    private Animation rotate_backward;


    private boolean isFabOpen = false;

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contributions_list, container, false);
        ButterKnife.bind(this, view);

        contributionsList.setOnItemClickListener((AdapterView.OnItemClickListener) getParentFragment());

        changeProgressBarVisibility(true);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeAnimations();
        setListeners();
    }

    private void initializeAnimations() {
        fab_open = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_backward);
    }

    private void setListeners() {
        fabPlus.setOnClickListener(view -> animateFAB(isFabOpen));
    }

    private void animateFAB(boolean isFabOpen) {
        this.isFabOpen = !isFabOpen;
        if (fabPlus.isShown()){
            if (isFabOpen) {
                fabPlus.startAnimation(rotate_backward);
                fabCamera.startAnimation(fab_close);
                fabGalery.startAnimation(fab_close);
                fabCamera.hide();
                fabGalery.hide();
            } else {
                fabPlus.startAnimation(rotate_forward);
                fabCamera.startAnimation(fab_open);
                fabGalery.startAnimation(fab_open);
                fabCamera.show();
                fabGalery.show();
            }
            this.isFabOpen=!isFabOpen;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ContributionsFragment parentFragment = (ContributionsFragment)getParentFragment();
        parentFragment.waitForContributionsListFragment.countDown();
    }

    /**
     * Responsible to set progress bar invisible and visible
     * @param isVisible True when contributions list should be hidden.
     */
    public void changeProgressBarVisibility(boolean isVisible) {
        this.progressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    /**
     * Clears sync message displayed with progress bar before contributions list became visible
     */
    protected void clearSyncMessage() {
        waitingMessage.setVisibility(GONE);
    }

    public ListAdapter getAdapter() {
        return contributionsList.getAdapter();
    }

    /**
     * Sets adapter to contributions list. If beta mode, sets upload count for beta explicitly.
     * @param adapter List adapter for uploads of contributor
     */
    public void setAdapter(ListAdapter adapter) {
        this.contributionsList.setAdapter(adapter);

        if(BuildConfig.FLAVOR.equalsIgnoreCase("beta")){
            //TODO: add betaSetUploadCount method
            ((ContributionsFragment) getParentFragment()).betaSetUploadCount(adapter.getCount());
        }
    }

    public interface SourceRefresher {
        void refreshSource();
    }
}
