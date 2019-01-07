package fr.free.nrw.commons.contributions;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
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

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.utils.ConfigUtils;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static android.view.View.*;
import static android.view.View.GONE;
import static fr.free.nrw.commons.contributions.ContributionController.SELECT_FROM_GALLERY;

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
    @BindView(R.id.fab_gallery)
    FloatingActionButton fabGallery;
    @BindView(R.id.noDataYet)
    TextView noDataYet;

    @Inject
    @Named("default_preferences")
    SharedPreferences defaultPrefs;

    private Animation fab_close;
    private Animation fab_open;
    private Animation rotate_forward;
    private Animation rotate_backward;


    private boolean isFabOpen = false;
    public ContributionController controller;

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contributions_list, container, false);
        ButterKnife.bind(this, view);

        contributionsList.setOnItemClickListener((AdapterView.OnItemClickListener) getParentFragment());

        changeEmptyScreen(true);
        changeProgressBarVisibility(true);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (controller == null) {
            controller = new ContributionController(this, defaultPrefs);
        }
        controller.loadState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (controller != null) {
            controller.saveState(outState);
        } else {
            controller = new ContributionController(this, defaultPrefs);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeAnimations();
        setListeners();
    }

    public void changeEmptyScreen(boolean isEmpty){
        this.noDataYet.setVisibility(isEmpty ? VISIBLE : GONE);
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
        });
        fabGallery.setOnClickListener(view -> controller.initiateGalleryPick(getActivity()));
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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ContributionsFragment parentFragment = (ContributionsFragment)getParentFragment();
        parentFragment.waitForContributionsListFragment.countDown();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Timber.d("OnActivityResult() parameters: Req code: %d Result code: %d Data: %s",
                    requestCode, resultCode, data);
            if (requestCode == ContributionController.SELECT_FROM_CAMERA) {
                // If coming from camera, pass null as uri. Because camera photos get saved to a
                // fixed directory
                controller.handleImagePicked(requestCode, null, false, null, null);
            } else if (requestCode == ContributionController.PICK_IMAGE_MULTIPLE) {
                handleMultipleImages(requestCode, data);
            } else if (requestCode == ContributionController.SELECT_FROM_GALLERY){
                controller.handleImagePicked(requestCode, data.getData(), false, null, null);
            }
        } else {
            Timber.e("OnActivityResult() parameters: Req code: %d Result code: %d Data: %s",
                    requestCode, resultCode, data);
        }
    }

    private void handleMultipleImages(int requestCode, Intent data) {
        if (getContext() == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && data.getClipData() != null) {
            ClipData mClipData = data.getClipData();
            ArrayList<Uri> mArrayUri = new ArrayList<>();
            for (int i = 0; i < mClipData.getItemCount(); i++) {

                ClipData.Item item = mClipData.getItemAt(i);
                Uri uri = item.getUri();
                mArrayUri.add(uri);
            }
            Log.v("LOG_TAG", "Selected Images" + mArrayUri.size());
            controller.handleImagesPicked(requestCode, mArrayUri);
        } else if(data.getData() != null) {
            controller.handleImagePicked(SELECT_FROM_GALLERY, data.getData(), false, null, null);
        }
    }


    /**
     * Responsible to set progress bar invisible and visible
     * @param isVisible True when contributions list should be hidden.
     */
    public void changeProgressBarVisibility(boolean isVisible) {
        this.progressBar.setVisibility(isVisible ? VISIBLE : GONE);
    }

    /**
     * Clears sync message displayed with progress bar before contributions list became visible
     */
    protected void clearSyncMessage() {
        waitingMessage.setVisibility(GONE);
        noDataYet.setVisibility(GONE);
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

        if (ConfigUtils.isBetaFlavour()) {
            //TODO: add betaSetUploadCount method
            ((ContributionsFragment) getParentFragment()).betaSetUploadCount(adapter.getCount());
        }
    }

    public interface SourceRefresher {
        void refreshSource();
    }
}
