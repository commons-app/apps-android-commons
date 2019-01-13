package fr.free.nrw.commons.contributions;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.utils.ImageUtils;
import fr.free.nrw.commons.utils.IntentUtils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static fr.free.nrw.commons.contributions.ContributionController.CAMERA_UPLOAD_REQUEST_CODE;
import static fr.free.nrw.commons.contributions.ContributionController.GALLERY_UPLOAD_REQUEST_CODE;
import static fr.free.nrw.commons.contributions.ContributionController.MULTIPLE_UPLOAD_IMAGE_LIMIT;

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

    @Inject @Named("default_preferences") SharedPreferences defaultPrefs;
    @Inject @Named("direct_nearby_upload_prefs") SharedPreferences directPrefs;
    @Inject ContributionController controller;

    private Animation fab_close;
    private Animation fab_open;
    private Animation rotate_forward;
    private Animation rotate_backward;


    private boolean isFabOpen = false;

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contributions_list, container, false);
        ButterKnife.bind(this, view);

        contributionsList.setOnItemClickListener((AdapterView.OnItemClickListener) getParentFragment());

        changeEmptyScreen(true);
        changeProgressBarVisibility(true);
        return view;
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
        fabCamera.setOnClickListener(view -> controller.initiateCameraPick(this, CAMERA_UPLOAD_REQUEST_CODE));
        fabGallery.setOnClickListener(view -> controller.initiateGalleryPick(this, MULTIPLE_UPLOAD_IMAGE_LIMIT, GALLERY_UPLOAD_REQUEST_CODE));
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (IntentUtils.shouldContributionsListHandle(requestCode, resultCode, data)) {
            List<Image> images = ImagePicker.getImages(data);
            Intent shareIntent = controller.handleImagesPicked(ImageUtils.getUriListFromImages(images), requestCode);
            startActivity(shareIntent);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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
