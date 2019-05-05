package fr.free.nrw.commons.upload;

import static fr.free.nrw.commons.contributions.ContributionController.ACTION_INTERNAL_UPLOADS;
import static fr.free.nrw.commons.upload.UploadService.EXTRA_FILES;
import static fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.category.CategoriesModel;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.contributions.ContributionController;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.upload.categories.UploadCategoriesFragment;
import fr.free.nrw.commons.upload.license.MediaLicenseFragment;
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment;
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.UploadMediaDetailFragmentCallback;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.disposables.CompositeDisposable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class UploadActivity extends BaseActivity implements UploadContract.View ,UploadBaseFragment.Callback{
    @Inject
    ContributionController contributionController;
    @Inject @Named("direct_nearby_upload_prefs") JsonKvStore directKvStore;
    @Inject UploadContract.UserActionListener presenter;
    @Inject CategoriesModel categoriesModel;
    @Inject SessionManager sessionManager;

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

    @BindView(R.id.vp_upload) ViewPager vpUpload;

    private boolean isTitleExpanded=true;

    private CompositeDisposable compositeDisposable;
    private ProgressDialog progressDialog;
    private UploadImageAdapter uploadImagesAdapter;
    private List<Fragment> fragments;
    private UploadCategoriesFragment uploadCategoriesFragment;
    private MediaLicenseFragment mediaLicenseFragment;
    private ThumbnailsAdapter thumbnailsAdapter;


    private String source;
    private Place place;
    private List<UploadableFile> uploadableFiles= Collections.emptyList();
    private int currentSelectedPosition=0;

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_upload);

        ButterKnife.bind(this);
        compositeDisposable = new CompositeDisposable();
        init();

        PermissionUtils.checkPermissionsAndPerformAction(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                this::receiveSharedItems,
                R.string.storage_permission_title,
                R.string.write_storage_permission_rationale_for_image_share);
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
    }

    private void initThumbnailsRecyclerView() {
        rvThumbnails.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false));
        thumbnailsAdapter=new ThumbnailsAdapter(() -> currentSelectedPosition);
        rvThumbnails.setAdapter(thumbnailsAdapter);

    }

    private void initViewPager() {
        vpUpload.setOffscreenPageLimit(0);
        uploadImagesAdapter=new UploadImageAdapter(getSupportFragmentManager());
        vpUpload.setAdapter(uploadImagesAdapter);
        vpUpload.addOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                    int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                currentSelectedPosition=position;
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
        checkStoragePermissions();
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
        presenter.onDetachView();
        compositeDisposable.clear();
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
    public void showMessage(int messageResourceId) {
        ViewUtil.showLongToast(this, messageResourceId);
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
                    .getQuantityString(R.plurals.upload_count_title, uploadableFiles.size(),uploadableFiles.size()));

            fragments = new ArrayList<>();
            int indexOfChild=0;
            for (UploadableFile uploadableFile : uploadableFiles) {
                UploadMediaDetailFragment uploadMediaDetailFragment = new UploadMediaDetailFragment();
                uploadMediaDetailFragment.setImageTobeUploaded(uploadableFile, source, place);
                uploadMediaDetailFragment.setCallback(new UploadMediaDetailFragmentCallback(){
                    @Override
                    public void deletePictureAtIndex(int index) {
                        if(uploadableFiles.size()==1){
                            showMessage(R.string.upload_cancelled);
                            finish();
                        }else {
                            fragments.remove(index);
                            uploadableFiles.remove(index);
                            thumbnailsAdapter.notifyItemRemoved(index);
                            uploadImagesAdapter.notifyDataSetChanged();
                        }
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
                });
                uploadMediaDetailFragment.setIndexInViewFlipper(indexOfChild++);
                uploadMediaDetailFragment.totalNumberOfSteps=uploadableFiles.size()+2;
                fragments.add(uploadMediaDetailFragment);
            }

            uploadCategoriesFragment = new UploadCategoriesFragment();
            uploadCategoriesFragment.setMediaTitleList(presenter.getImageTitleList());
            uploadCategoriesFragment.setIndexInViewFlipper(indexOfChild++);
            uploadCategoriesFragment.totalNumberOfSteps=uploadableFiles.size()+2;
            uploadCategoriesFragment.setCallback(this);

            mediaLicenseFragment = new MediaLicenseFragment();
            mediaLicenseFragment.setIndexInViewFlipper(indexOfChild);
            mediaLicenseFragment.setCallback(this);
            mediaLicenseFragment.totalNumberOfSteps=uploadableFiles.size()+2;


            fragments.add(uploadCategoriesFragment);
            fragments.add(mediaLicenseFragment);

            uploadImagesAdapter.setFragments(fragments);
        }
    }

    private void receiveExternalSharedItems() {
        uploadableFiles = contributionController.handleExternalImagesPicked(this, getIntent());
    }

    private void receiveInternalSharedItems() {
        Intent intent = getIntent();

        if (intent.hasExtra(UploadService.EXTRA_SOURCE)) {
            source = intent.getStringExtra(UploadService.EXTRA_SOURCE);
        } else {
            source = Contribution.SOURCE_EXTERNAL;
        }

        Timber.d("Received intent %s with action %s and from source %s",
                intent.toString(),
                intent.getAction(),
                source);

        uploadableFiles = intent.getParcelableArrayListExtra(EXTRA_FILES);
        Timber.i("Received multiple upload %s", uploadableFiles.size());

        place = intent.getParcelableExtra(PLACE_OBJECT);
        resetDirectPrefs();
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

    private void showInfoAlert(int titleStringID, int messageStringId, String... formatArgs) {
        new AlertDialog.Builder(this)
                .setTitle(titleStringID)
                .setMessage(getString(messageStringId, (Object[]) formatArgs))
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.cancel())
                .create()
                .show();
    }

    @Override
    public void onNextButtonClicked(int index) {
        if (index < vpUpload.getChildCount()) {
            vpUpload.setCurrentItem(index + 1, false);
        } else {
            presenter.handleSubmit();
        }
    }

    @Override
    public void onPreviousButtonClicked(int index) {
        if (index != 0) {
            vpUpload.setCurrentItem(index - 1, true);
        }
    }

    /**
     * The adapter used to show image upload intermediate fragments
     */

    private class UploadImageAdapter extends FragmentStatePagerAdapter {
        List<Fragment> fragments;

        public UploadImageAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
            this.fragments = new ArrayList<>();
        }

        public void setFragments(List<Fragment> fragments) {
            this.fragments = fragments;
            notifyDataSetChanged();
        }

        @Override public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override public int getCount() {
            return fragments.size();
        }
    }


    @OnClick(R.id.rl_container_title)
    public void onRlContainerTitleClicked(){
        rvThumbnails.setVisibility(isTitleExpanded ? View.GONE : View.VISIBLE);
        isTitleExpanded = !isTitleExpanded;
        ibToggleTopCard.setRotation(ibToggleTopCard.getRotation() + 180);
    }
}
