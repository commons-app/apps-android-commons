package fr.free.nrw.commons.contributions;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import android.widget.TextView;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import timber.log.Timber;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.app.Activity.RESULT_OK;
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

    @Inject
    @Named("default_preferences")
    SharedPreferences defaultPrefs;

    private Animation fab_close;
    private Animation fab_open;
    private Animation rotate_forward;
    private Animation rotate_backward;


    private boolean isFabOpen = false;
    private ContributionController controller;

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contributions_list, container, false);
        ButterKnife.bind(this, view);

        contributionsList.setOnItemClickListener((AdapterView.OnItemClickListener) getParentFragment());

        changeProgressBarVisibility(true);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (controller == null) {
            controller = new ContributionController(this);
        }
        controller.loadState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (controller != null) {
            controller.saveState(outState);
        } else {
            controller = new ContributionController(this);
        }
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
        fabCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean useExtStorage = defaultPrefs.getBoolean("useExternalStorage", true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && useExtStorage) {
                    // Here, thisActivity is the current activity
                    if (ContextCompat.checkSelfPermission(getActivity(), WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        if (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
                            // Show an explanation to the user *asynchronously* -- don't block
                            // this thread waiting for the user's response! After the user
                            // sees the explanation, try again to request the permission.
                            new AlertDialog.Builder(getActivity())
                                    .setMessage(getString(R.string.write_storage_permission_rationale))
                                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                        requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, 3);
                                        dialog.dismiss();
                                    })
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .create()
                                    .show();
                        } else {
                            // No explanation needed, we can request the permission.
                            requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE},
                                    3);
                            // MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE is an
                            // app-defined int constant. The callback method gets the
                            // result of the request.
                        }
                    } else {
                        controller.startCameraCapture();
                    }
                } else {
                    controller.startCameraCapture();
                }
            }
        });

        fabGalery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            //Gallery crashes before reach ShareActivity screen so must implement permissions check here
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    // Here, thisActivity is the current activity
                    if (ContextCompat.checkSelfPermission(getActivity(),
                            READ_EXTERNAL_STORAGE)
                            != PERMISSION_GRANTED) {

                        // Should we show an explanation?
                        if (shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)) {

                            // Show an explanation to the user *asynchronously* -- don't block
                            // this thread waiting for the user's response! After the user
                            // sees the explanation, try again to request the permission.

                            new AlertDialog.Builder(getActivity())
                                    .setMessage(getString(R.string.read_storage_permission_rationale))
                                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                        requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, 1);
                                        dialog.dismiss();
                                    })
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .create()
                                    .show();

                        } else {

                            // No explanation needed, we can request the permission.

                            requestPermissions(new String[]{READ_EXTERNAL_STORAGE},
                                    1);

                            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                            // app-defined int constant. The callback method gets the
                            // result of the request.
                        }
                    } else {
                        controller.startGalleryPick();
                    }

                } else {
                    controller.startGalleryPick();
                }
            }
        });
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Timber.d("OnActivityResult() parameters: Req code: %d Result code: %d Data: %s",
                    requestCode, resultCode, data);
            if (requestCode == ContributionController.SELECT_FROM_CAMERA) {
                // If coming from camera, pass null as uri. Because camera photos get saved to a
                // fixed directory
                controller.handleImagePicked(requestCode, null, false, null);
            } else if (requestCode == ContributionController.SELECT_FROM_GALLERY){
                controller.handleImagePicked(requestCode, data.getData(), false, null);
            }
        } else {
            Timber.e("OnActivityResult() parameters: Req code: %d Result code: %d Data: %s",
                    requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Timber.d("onRequestPermissionsResult: req code = " + " perm = "
                + Arrays.toString(permissions) + " grant =" + Arrays.toString(grantResults));

        switch (requestCode) {
            // 1 = Storage allowed when gallery selected
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    Timber.d("Call controller.startGalleryPick()");
                    controller.startGalleryPick();
                }
            }
            break;
            // 2 = Location allowed when 'nearby places' selected
            case 2: {
                // TODO: understand and fix
                /*if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    Timber.d("Location permission granted");
                    Intent nearbyIntent = new Intent(getActivity(), MainActivity.class);
                    startActivity(nearbyIntent);
                }*/
            }
            break;
            case 3: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Call controller.startCameraCapture()");
                    controller.startCameraCapture();
                }
            }
        }
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
