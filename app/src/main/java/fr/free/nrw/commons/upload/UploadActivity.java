package fr.free.nrw.commons.upload;

import static fr.free.nrw.commons.contributions.ContributionController.ACTION_INTERNAL_UPLOADS;
import static fr.free.nrw.commons.upload.UploadPresenter.COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES;
import static fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.ContributionController;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.mwapi.UserClient;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.upload.categories.UploadCategoriesFragment;
import fr.free.nrw.commons.upload.depicts.DepictsFragment;
import fr.free.nrw.commons.upload.license.MediaLicenseFragment;
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment;
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.UploadMediaDetailFragmentCallback;
import fr.free.nrw.commons.upload.worker.UploadWorker;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class UploadActivity extends BaseActivity implements UploadContract.View, UploadBaseFragment.Callback {

    @Inject
    ContributionController contributionController;
    @Inject
    @Named("default_preferences")
    JsonKvStore directKvStore;
    @Inject
    UploadContract.UserActionListener presenter;
    @Inject
    SessionManager sessionManager;
    @Inject
    UserClient userClient;


    @BindView(R.id.cv_container_top_card)
    CardView cvContainerTopCard;

    @BindView(R.id.ll_container_top_card)
    LinearLayout llContainerTopCard;

    @BindView(R.id.rl_container_title)
    RelativeLayout rlContainerTitle;

    @BindView(R.id.tv_top_card_title)
    TextView tvTopCardTitle;

    @BindView(R.id.ib_toggle_top_card)
    ImageButton ibToggleTopCard;

    @BindView(R.id.rv_thumbnails)
    RecyclerView rvThumbnails;

    @BindView(R.id.vp_upload)
    ViewPager vpUpload;

    private boolean isTitleExpanded = true;

    private CompositeDisposable compositeDisposable;
    private ProgressDialog progressDialog;
    private UploadImageAdapter uploadImagesAdapter;
    private List<UploadBaseFragment> fragments;
    private UploadCategoriesFragment uploadCategoriesFragment;
    private DepictsFragment depictsFragment;
    private MediaLicenseFragment mediaLicenseFragment;
    private ThumbnailsAdapter thumbnailsAdapter;

    private Place place;
    private List<UploadableFile> uploadableFiles = Collections.emptyList();
    private int currentSelectedPosition = 0;
    /*
     Checks for if multiple files selected
     */
    private boolean isMultipleFilesSelected = false;

    public static final String EXTRA_FILES = "commons_image_exta";

    /**
     * Stores all nearby places found and related users response for
     * each place while uploading media
     */
    public static HashMap<Place,Boolean> nearbyPopupAnswers;

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_upload);

        ButterKnife.bind(this);
        compositeDisposable = new CompositeDisposable();
        init();
        nearbyPopupAnswers = new HashMap<>();

        PermissionUtils.checkPermissionsAndPerformAction(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                this::receiveSharedItems,
                R.string.storage_permission_title,
                R.string.write_storage_permission_rationale_for_image_share);
        //getting the current dpi of the device and if it is less than 320dp i.e. overlapping
        //threshold, thumbnails automatically minimizes
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float dpi = (metrics.widthPixels)/(metrics.density);
        if (dpi<=321) {
            onRlContainerTitleClicked();
        }
    }

    private void init() {
        initProgressDialog();
        initViewPager();
        initThumbnailsRecyclerView();
        //And init other things you need to
    }

    private void initProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.please_wait));
        progressDialog.setCancelable(false);
    }

    private void initThumbnailsRecyclerView() {
        rvThumbnails.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false));
        thumbnailsAdapter = new ThumbnailsAdapter(() -> currentSelectedPosition);
        rvThumbnails.setAdapter(thumbnailsAdapter);

    }

    private void initViewPager() {
        uploadImagesAdapter = new UploadImageAdapter(getSupportFragmentManager());
        vpUpload.setAdapter(uploadImagesAdapter);
        vpUpload.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                currentSelectedPosition = position;
                if (position >= uploadableFiles.size()) {
                    cvContainerTopCard.setVisibility(View.GONE);
                } else {
                    thumbnailsAdapter.notifyDataSetChanged();
                    cvContainerTopCard.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public boolean isLoggedIn() {
        return sessionManager.isUserLoggedIn();
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.onAttachView(this);
        if (!isLoggedIn()) {
            askUserToLogIn();
        }
        checkBlockStatus();
        checkStoragePermissions();
    }

    /**
     * Makes API call to check if user is blocked from Commons. If the user is blocked, a snackbar
     * is created to notify the user
     */
    protected void checkBlockStatus() {
        compositeDisposable.add(userClient.isUserBlockedFromCommons()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .filter(result -> result)
                .subscribe(result -> showInfoAlert(R.string.block_notification_title,
                        R.string.block_notification, UploadActivity.this::finish)
                ));
    }

    private void checkStoragePermissions() {
        PermissionUtils.checkPermissionsAndPerformAction(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                () -> {
                    //TODO handle this
                },
                R.string.storage_permission_title,
                R.string.write_storage_permission_rationale_for_image_share);
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void returnToMainActivity() {
        finish();
    }

    /**
     * Show/Hide the progress dialog
     */
    @Override
    public void showProgress(boolean shouldShow) {
        if (shouldShow) {
            if (!progressDialog.isShowing()) {
                progressDialog.show();
            }
        } else {
            if (progressDialog != null && !isFinishing()) {
                progressDialog.dismiss();
            }
        }
    }

    @Override
    public int getIndexInViewFlipper(UploadBaseFragment fragment) {
        return fragments.indexOf(fragment);
    }

    @Override
    public int getTotalNumberOfSteps() {
        return fragments.size();
    }

    @Override
    public boolean isWLMUpload() {
        return place!=null && place.isMonument();
    }

    @Override
    public void showMessage(int messageResourceId) {
        ViewUtil.showLongToast(this, messageResourceId);
    }

    @Override
    public List<UploadableFile> getUploadableFiles() {
        return uploadableFiles;
    }

    @Override
    public void showHideTopCard(boolean shouldShow) {
        llContainerTopCard.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onUploadMediaDeleted(int index) {
        fragments.remove(index);//Remove the corresponding fragment
        uploadableFiles.remove(index);//Remove the files from the list
        thumbnailsAdapter.notifyItemRemoved(index); //Notify the thumbnails adapter
        uploadImagesAdapter.notifyDataSetChanged(); //Notify the ViewPager
    }

    @Override
    public void updateTopCardTitle() {
        tvTopCardTitle.setText(getResources()
                .getQuantityString(R.plurals.upload_count_title, uploadableFiles.size(), uploadableFiles.size()));
    }

    @Override
    public void makeUploadRequest() {
        WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(
            UploadWorker.class.getSimpleName(),
            ExistingWorkPolicy.APPEND_OR_REPLACE, OneTimeWorkRequest.from(UploadWorker.class));
    }

    @Override
    public void askUserToLogIn() {
        Timber.d("current session is null, asking user to login");
        ViewUtil.showLongToast(this, getString(R.string.user_not_logged_in));
        Intent loginIntent = new Intent(UploadActivity.this, LoginActivity.class);
        startActivity(loginIntent);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CommonsApplication.OPEN_APPLICATION_DETAIL_SETTINGS) {
            //TODO: Confirm if handling manual permission enabled is required
        }
    }

    private void receiveSharedItems() {
        thumbnailsAdapter.context=this;
        Intent intent = getIntent();
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            receiveExternalSharedItems();
        } else if (ACTION_INTERNAL_UPLOADS.equals(action)) {
            receiveInternalSharedItems();
        }

        if (uploadableFiles == null || uploadableFiles.isEmpty()) {
            handleNullMedia();
        } else {
            //Show thumbnails
            if (uploadableFiles.size()
                    > 1) {//If there is only file, no need to show the image thumbnails
                thumbnailsAdapter.setUploadableFiles(uploadableFiles);
            } else {
                llContainerTopCard.setVisibility(View.GONE);
            }
            tvTopCardTitle.setText(getResources()
                    .getQuantityString(R.plurals.upload_count_title, uploadableFiles.size(), uploadableFiles.size()));

            fragments = new ArrayList<>();
            for (UploadableFile uploadableFile : uploadableFiles) {
                UploadMediaDetailFragment uploadMediaDetailFragment = new UploadMediaDetailFragment();
                uploadMediaDetailFragment.setImageTobeUploaded(uploadableFile, place);
                uploadMediaDetailFragment.setCallback(new UploadMediaDetailFragmentCallback() {
                    @Override
                    public void deletePictureAtIndex(int index) {
                        presenter.deletePictureAtIndex(index);
                    }

                    @Override
                    public void onNextButtonClicked(int index) {
                        UploadActivity.this.onNextButtonClicked(index);
                    }

                    @Override
                    public void onPreviousButtonClicked(int index) {
                        UploadActivity.this.onPreviousButtonClicked(index);
                    }

                    @Override
                    public void showProgress(boolean shouldShow) {
                        UploadActivity.this.showProgress(shouldShow);
                    }

                    @Override
                    public int getIndexInViewFlipper(UploadBaseFragment fragment) {
                        return fragments.indexOf(fragment);
                    }

                    @Override
                    public int getTotalNumberOfSteps() {
                        return fragments.size();
                    }

                    @Override
                    public boolean isWLMUpload() {
                        return place!=null && place.isMonument();
                    }
                });
                fragments.add(uploadMediaDetailFragment);
            }

            uploadCategoriesFragment = new UploadCategoriesFragment();
            uploadCategoriesFragment.setCallback(this);

            depictsFragment = new DepictsFragment();
            depictsFragment.setCallback(this);

            mediaLicenseFragment = new MediaLicenseFragment();
            mediaLicenseFragment.setCallback(this);

            fragments.add(depictsFragment);
            fragments.add(uploadCategoriesFragment);
            fragments.add(mediaLicenseFragment);

            uploadImagesAdapter.setFragments(fragments);
            vpUpload.setOffscreenPageLimit(fragments.size());
        }
    }

    private void receiveExternalSharedItems() {
        uploadableFiles = contributionController.handleExternalImagesPicked(this, getIntent());
    }

    private void receiveInternalSharedItems() {
        Intent intent = getIntent();

        Timber.d("Received intent %s with action %s", intent.toString(), intent.getAction());

        uploadableFiles = intent.getParcelableArrayListExtra(EXTRA_FILES);
        isMultipleFilesSelected = uploadableFiles.size() > 1;
        Timber.i("Received multiple upload %s", uploadableFiles.size());

        place = intent.getParcelableExtra(PLACE_OBJECT);
        resetDirectPrefs();
    }

    /**
     * Returns if multiple files selected or not.
     */
    public boolean getIsMultipleFilesSelected() {
        return isMultipleFilesSelected;
    }

    public void resetDirectPrefs() {
        directKvStore.remove(PLACE_OBJECT);
    }

    /**
     * Handle null URI from the received intent.
     * Current implementation will simply show a toast and finish the upload activity.
     */
    private void handleNullMedia() {
        ViewUtil.showLongToast(this, R.string.error_processing_image);
        finish();
    }

    private void showInfoAlert(int titleStringID, int messageStringId, Runnable positive, String... formatArgs) {
        new AlertDialog.Builder(this)
                .setTitle(titleStringID)
                .setMessage(getString(messageStringId, (Object[]) formatArgs))
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    positive.run();
                    dialog.cancel();
                })
                .create()
                .show();
    }

    @Override
    public void showAlertDialog(int messageResourceId, Runnable onPositiveClick) {
        DialogUtil.showAlertDialog(this,
            "",
            getString(messageResourceId),
            getString(R.string.ok),
            onPositiveClick,
            false);
    }

    @Override
    public void onNextButtonClicked(int index) {
        if (index < fragments.size() - 1) {
            vpUpload.setCurrentItem(index + 1, false);
            fragments.get(index + 1).onBecameVisible();
            ((LinearLayoutManager) rvThumbnails.getLayoutManager())
                .scrollToPositionWithOffset((index > 0) ? index-1 : 0, 0);
        } else {
            if(defaultKvStore.getInt(COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES, 0) >= 10){
                DialogUtil.showAlertDialog(this,
                    "",
                    getString(R.string.location_message),
                    getString(R.string.ok),
                    () -> {
                        defaultKvStore.putInt(COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES, 0);
                        presenter.handleSubmit();
                    }, false);
            } else {
                presenter.handleSubmit();
            }
        }
    }

    @Override
    public void onPreviousButtonClicked(int index) {
        if (index != 0) {
            vpUpload.setCurrentItem(index - 1, true);
            fragments.get(index - 1).onBecameVisible();
            ((LinearLayoutManager) rvThumbnails.getLayoutManager())
                .scrollToPositionWithOffset((index > 3) ? index-2 : 0, 0);
        }
    }

    /**
     * The adapter used to show image upload intermediate fragments
     */

    private class UploadImageAdapter extends FragmentStatePagerAdapter {
        List<UploadBaseFragment> fragments;

        public UploadImageAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
            this.fragments = new ArrayList<>();
        }

        public void setFragments(List<UploadBaseFragment> fragments) {
            this.fragments = fragments;
            notifyDataSetChanged();
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public int getItemPosition(Object object) {
            return PagerAdapter.POSITION_NONE;
        }
    }


    @OnClick(R.id.rl_container_title)
    public void onRlContainerTitleClicked() {
        rvThumbnails.setVisibility(isTitleExpanded ? View.GONE : View.VISIBLE);
        isTitleExpanded = !isTitleExpanded;
        ibToggleTopCard.setRotation(ibToggleTopCard.getRotation() + 180);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onDetachView();
        compositeDisposable.clear();
        if (mediaLicenseFragment != null) {
            mediaLicenseFragment.setCallback(null);
        }
        if (uploadCategoriesFragment != null) {
            uploadCategoriesFragment.setCallback(null);
        }
    }

    /**
     * Overrides the back button to make sure the user is prepared to lose their progress
     */
    @Override
    public void onBackPressed() {
        DialogUtil.showAlertDialog(this,
            getString(R.string.back_button_warning),
            getString(R.string.back_button_warning_desc),
            getString(R.string.back_button_continue),
            getString(R.string.back_button_warning),
            null,
            this::finish
        );
    }

}
