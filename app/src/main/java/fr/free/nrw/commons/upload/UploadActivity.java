package fr.free.nrw.commons.upload;

import static fr.free.nrw.commons.contributions.ContributionController.ACTION_INTERNAL_UPLOADS;
import static fr.free.nrw.commons.upload.UploadService.EXTRA_FILES;
import static fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
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
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.pedrogomez.renderers.RVRendererAdapter;
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
import fr.free.nrw.commons.upload.UploadModel.UploadItem;
import fr.free.nrw.commons.upload.categories.UploadCategoriesFragment;
import fr.free.nrw.commons.upload.license.MediaLicenseFragment;
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.disposables.CompositeDisposable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class UploadActivity extends BaseActivity implements SimilarImageInterface, IUpload.View ,UploadBaseFragment.Callback{
    @Inject
    ContributionController contributionController;
    @Inject @Named("direct_nearby_upload_prefs") JsonKvStore directKvStore;
    @Inject IUpload.UserActionListener presenter;
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
    private List<UploadItem> uploadItems;
    private RVRendererAdapter<UploadableFile> thumbnailsAdapter;


    private String source;
    private Place place;
    private List<UploadableFile> uploadableFiles= Collections.emptyList();
    private int baseHeight;

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_upload);

        ButterKnife.bind(this);
        compositeDisposable = new CompositeDisposable();
        baseHeight=llContainerTopCard.getMeasuredHeight();
        init();

        PermissionUtils.checkPermissionsAndPerformAction(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                this::receiveSharedItems,
                R.string.storage_permission_title,
                R.string.write_storage_permission_rationale_for_image_share);
    }

    private void init() {
        uploadItems=new ArrayList<>();
        initViewPager();
        initThumbnailsRecyclerView();
        //And init other things you need to
    }

    private void initThumbnailsRecyclerView() {
        rvThumbnails.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false));
        thumbnailsAdapter=new UploadThumbnailsAdapterFactory(content -> {

        }).create(uploadableFiles);
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
                if (position >= uploadableFiles.size()) {
                    cvContainerTopCard.setVisibility(View.GONE);
                } else {
                    cvContainerTopCard.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public boolean checkIfLoggedIn() {
        if (!sessionManager.isUserLoggedIn()) {
            Timber.d("Current account is null");
            ViewUtil.showLongToast(this, getString(R.string.user_not_logged_in));
            Intent loginIntent = new Intent(UploadActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            return false;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIfLoggedIn();
        presenter.onAttachView(this);
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

    @Override
    public void updateRightCardContent(boolean gpsPresent) {
        /*if(gpsPresent){
            rightCardMapButton.setVisibility(View.VISIBLE);
        }else{
            rightCardMapButton.setVisibility(View.GONE);
        }*/
        //The card should be disabled if it has no buttons.
        setRightCardVisibility(gpsPresent);
    }

    @Override
    public void updateBottomCardContent(int currentStep,
                                        int stepCount,
                                        UploadModel.UploadItem uploadItem,
                                        boolean isShowingItem) {
        String cardTitle = getResources().getString(R.string.step_count, currentStep, stepCount);
        String cardSubTitle = getResources().getString(R.string.image_in_set_label, currentStep);
        /*bottomCardTitle.setText(cardTitle);
        bottomCardSubtitle.setText(cardSubTitle);
        categoryTitle.setText(cardTitle);
        licenseTitle.setText(cardTitle);*/
        if (currentStep == stepCount) {
            dismissKeyboard();
        }
    }

    @Override
    public void updateLicenses(List<String> licenses, String selectedLicense) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, licenses);
        /*licenseSpinner.setAdapter(adapter);

        int position = licenses.indexOf(getString(Utils.licenseNameFor(selectedLicense)));

        // Check position is valid
        if (position < 0) {
            Timber.d("Invalid position: %d. Using default license", position);
            position = licenses.size() - 1;
        }

        Timber.d("Position: %d %s", position, getString(Utils.licenseNameFor(selectedLicense)));
        licenseSpinner.setSelection(position);*/
    }

    @SuppressLint("StringFormatInvalid")
    @Override
    public void updateLicenseSummary(String selectedLicense, int imageCount) {
        String licenseHyperLink = "<a href='" + Utils.licenseUrlFor(selectedLicense) + "'>" +
                getString(Utils.licenseNameFor(selectedLicense)) + "</a><br>";

    }

    @Override
    public void updateTopCardContent() {
        tvTopCardTitle.setText(getResources().getQuantityString(R.plurals.upload_count_title,uploadItems.size()));
    }

    @Override
    public void setNextEnabled(boolean available) {
       /* next.setEnabled(available);
        categoryNext.setEnabled(available);*/
    }

    @Override
    public void setSubmitEnabled(boolean available) {
//        submit.setEnabled(available);
    }

    @Override
    public void setPreviousEnabled(boolean available) {
       /* previous.setEnabled(available);
        categoryPrevious.setEnabled(available);
        licensePrevious.setEnabled(available);*/
    }

    @Override
    public void setTopCardState(boolean state) {
//        updateCardState(state, topCardExpandButton, topCardThumbnails);
    }

    @Override
    public void setTopCardVisibility(boolean visible) {
//        topCard.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setRightCardVisibility(boolean visible) {
//        rightCard.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setBottomCardVisibility(@UploadPage int page, int uploadCount) {
        /*if (page == TITLE_CARD) {
            viewFlipper.setDisplayedChild(0);
        } else if (page == CATEGORIES) {
            viewFlipper.setDisplayedChild(1);
        } else if (page == LICENSE) {
            viewFlipper.setDisplayedChild(2);
            dismissKeyboard();
        } else if (page == PLEASE_WAIT) {
            viewFlipper.setDisplayedChild(3);
            pleaseWaitTextView.setText(getResources().getQuantityText(R.plurals.receiving_shared_content, uploadCount));
        }*/
    }

    /**
     * Only show the subtitle ("For all images in set") if multiple images being uploaded
     * @param imageCount Number of images being uploaded
     */
    @Override
    public void updateSubtitleVisibility(int imageCount) {
        /*categoriesSubtitle.setVisibility(imageCount > 1 ? View.VISIBLE : View.GONE);
        licenseSubtitle.setVisibility(imageCount > 1 ? View.VISIBLE : View.GONE);*/
    }

    @Override
    public void setBottomCardState(boolean state) {
//        updateCardState(state, bottomCardExpandButton, rvDescriptions, previous, next, bottomCardAddDescription);
    }

    @Override
    public void setRightCardState(boolean state) {
       /* rightCardExpandButton.animate().rotation(rightCardExpandButton.getRotation() + (state ? -180 : 180)).start();
        //Add all items in rightCard here
        rightCardMapButton.setVisibility(state ? View.VISIBLE : View.GONE);*/
    }

    @Override
    public void setBackground(Uri mediaUri) {
        /*background.setImageURI(mediaUri);*/
    }


    @Override
    public void dismissKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        // verify if the soft keyboard is open
        if (imm != null && imm.isAcceptingText() && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public void showBadPicturePopup(String errorMessage) {
        /*DialogUtil.showAlertDialog(this,
                getString(R.string.warning),
                errorMessage,
                () -> presenter.deletePicture(),
                () -> presenter.keepPicture());*/
    }

    @Override
    public void showDuplicatePicturePopup() {
        /*DialogUtil.showAlertDialog(this,
                getString(R.string.warning),
                String.format(getString(R.string.upload_title_duplicate), presenter.getCurrentImageFileName()),
                null,
                () -> {
                    presenter.keepPicture();
                });*/
    }

    @Override
    public void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setMessage(getString(R.string.please_wait));
        progressDialog.show();
    }

    @Override
    public void hideProgressDialog() {
        if (progressDialog != null && !isFinishing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void askUserToLogIn() {
        //TODO, perform login
    }

    @Override
    public void launchMapActivity(String decCoords) {
        Utils.handleGeoCoordinates(this, decCoords);
    }

    @Override
    public void showErrorMessage(int resourceId) {
        ViewUtil.showShortToast(this, resourceId);
    }

    @Override
    public void initDefaultCategories() {
        updateCategoryList("");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CommonsApplication.OPEN_APPLICATION_DETAIL_SETTINGS) {
            //TODO: Confirm if handling manual permission enabled is required
        }
    }

    /**
     * Parses links from HTML string, and makes the links clickable in the specified TextView.<br>
     * Uses {@link #makeLinkClickable(SpannableStringBuilder, URLSpan)}.
     * @see <a href="https://stackoverflow.com/questions/12418279/android-textview-with-clickable-links-how-to-capture-clicks">Source</a>
     */
    private void setTextViewHTML(TextView text, String html)
    {
        CharSequence sequence = Html.fromHtml(html);
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
        for (URLSpan span : urls) {
            makeLinkClickable(strBuilder, span);
        }
        text.setText(strBuilder);
        text.setMovementMethod(LinkMovementMethod.getInstance());
    }

    /**
     * Sets onClick handler to launch browser for the specified URLSpan.
     * @see <a href="https://stackoverflow.com/questions/12418279/android-textview-with-clickable-links-how-to-capture-clicks">Source</a>
     */
    private void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span)
    {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);
        ClickableSpan clickable = new ClickableSpan() {
            public void onClick(View view) {
                // Handle hyperlink click
                String hyperLink = span.getURL();
                launchBrowser(hyperLink);
            }
        };
        strBuilder.setSpan(clickable, start, end, flags);
        strBuilder.removeSpan(span);
    }

    private void launchBrowser(String hyperLink) {
        Utils.handleWebUrl(this, Uri.parse(hyperLink));
    }

    private void configureLicenses() {
        /*licenseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String licenseName = parent.getItemAtPosition(position).toString();
                presenter.selectLicense(licenseName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                presenter.selectLicense(null);
            }
        });*/
    }

    private void configureLayout() {
       /* background.setScaleType(ImageView.ScaleType.CENTER_CROP);
        background.setOnScaleChangeListener((scaleFactor, x, y) -> presenter.closeAllCards());*/
    }

    private void configureTopCard() {
       /* topCardExpandButton.setOnClickListener(v -> presenter.toggleTopCardState());
        topCardThumbnails.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false));*/
    }

    private void configureBottomCard() {
//        bottomCardExpandButton.setOnClickListener(v -> presenter.toggleBottomCardState());
    }

    private void configureRightCard() {
       /* rightCardExpandButton.setOnClickListener(v -> presenter.toggleRightCardState());
        rightCardMapButton.setOnClickListener(v -> presenter.openCoordinateMap());*/
    }

    private void configureNavigationButtons() {
        // Navigation next / previous for each image as we're collecting title + description
        /*next.setOnClickListener(v -> {
            if (!NetworkUtils.isInternetConnectionEstablished(this)) {
                ViewUtil.showShortSnackbar(rootLayout, R.string.no_internet);
                return;
            }
            setTitleAndDescriptions();
        });
        previous.setOnClickListener(v -> presenter.handlePrevious());

        // Next / previous for the category selection currentPage
        categoryNext.setOnClickListener(v -> presenter.handleCategoryNext(categoriesModel, false));
        categoryPrevious.setOnClickListener(v -> presenter.handlePrevious());

        // Finally, the previous / submit buttons on the final currentPage of the wizard
        licensePrevious.setOnClickListener(v -> presenter.handlePrevious());
        submit.setOnClickListener(v -> {
            Toast.makeText(this, R.string.uploading_started, Toast.LENGTH_LONG).show();
            presenter.handleSubmit(categoriesModel);
            finish();
        });*/

    }

    private void setTitleAndDescriptions() {
    }

    private void configureCategories() {
    }

    private void configurePolicy() {
//        setTextViewHTML(licensePolicy, getString(R.string.media_upload_policy));
    }

    @SuppressLint("CheckResult")
    private void updateCategoryList(String filter) {

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
            thumbnailsAdapter.clear();
            thumbnailsAdapter.addAll(uploadableFiles);
            thumbnailsAdapter.notifyDataSetChanged();

            tvTopCardTitle.setText(getResources()
                    .getQuantityString(R.plurals.upload_count_title, uploadableFiles.size()));

            fragments = new ArrayList<>();
            int indexOfChild=0;
            for (UploadableFile uploadableFile : uploadableFiles) {
                UploadMediaDetailFragment uploadMediaDetailFragment = new UploadMediaDetailFragment();
                uploadMediaDetailFragment.setImageTobeUploaded(uploadableFile, source, place);
                uploadMediaDetailFragment.setCallback(this);
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

    private void updateCardState(boolean state, ImageView button, View... content) {
        button.animate().rotation(button.getRotation() + (state ? 180 : -180)).start();
        if (content != null) {
            for (View view : content) {
                view.setVisibility(state ? View.VISIBLE : View.GONE);
            }
        }
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
    public void showSimilarImageFragment(String originalFilePath, String possibleFilePath) {
        /*SimilarImageDialogFragment newFragment = new SimilarImageDialogFragment();
        Bundle args = new Bundle();
        args.putString("originalImagePath", originalFilePath);
        args.putString("possibleImagePath", possibleFilePath);
        newFragment.setArguments(args);
        newFragment.show(getSupportFragmentManager(), "dialog");*/
    }

    @Override public void setUploadItems(List<UploadModel.UploadItem> uploadItems) {
        this.uploadItems=uploadItems;
    }

    private void setUpTopCardView() {
        if(null!=uploadableFiles){
            tvTopCardTitle.setText(getResources().getQuantityString(R.plurals.upload_count_title,uploadableFiles.size(),uploadableFiles.size()));
        }
    }

    @OnClick(R.id.ll_container_top_card)
    public void onLLContainerTopCardClicked(){

    }

    @OnClick(R.id.ib_toggle_top_card)
    public void onImageButtonTopCardClicked(){

    }

    @Override
    public void onNextButtonClicked(int index) {
        if (index != vpUpload.getChildCount()) {
            vpUpload.setCurrentItem(index + 1, true);
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

    @Override
    public int getMarginTop(int index) {
        if (index < uploadableFiles.size()) {
            return 0;
        } else {
            return baseHeight;
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
        rvThumbnails.setVisibility(isTitleExpanded?View.GONE:View.VISIBLE);
        isTitleExpanded=!isTitleExpanded;
    }
}
