package fr.free.nrw.commons.upload;

import android.Manifest;
import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.github.chrisbanes.photoview.PhotoView;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.pedrogomez.renderers.RVRendererAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.category.CategoriesModel;
import fr.free.nrw.commons.category.CategoryItem;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.StringUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static fr.free.nrw.commons.utils.ImageUtils.Result;
import static fr.free.nrw.commons.utils.ImageUtils.getErrorMessageForResult;
import static fr.free.nrw.commons.wikidata.WikidataConstants.IS_DIRECT_UPLOAD;
import static fr.free.nrw.commons.wikidata.WikidataConstants.WIKIDATA_ENTITY_ID_PREF;
import static fr.free.nrw.commons.wikidata.WikidataConstants.WIKIDATA_ITEM_LOCATION;

public class UploadActivity extends AuthenticatedActivity implements UploadView, SimilarImageInterface {
    @Inject MediaWikiApi mwApi;
    @Inject @Named("direct_nearby_upload_prefs") SharedPreferences directPrefs;
    @Inject UploadPresenter presenter;
    @Inject CategoriesModel categoriesModel;

    // Main GUI
    @BindView(R.id.backgroundImage) PhotoView background;
    @BindView(R.id.activity_upload_cards) ConstraintLayout cardLayout;
    @BindView(R.id.view_flipper) ViewFlipper viewFlipper;

    // Top Card
    @BindView(R.id.top_card) CardView topCard;
    @BindView(R.id.top_card_expand_button) ImageView topCardExpandButton;
    @BindView(R.id.top_card_title) TextView topCardTitle;
    @BindView(R.id.top_card_thumbnails) RecyclerView topCardThumbnails;

    // Bottom Card
    @BindView(R.id.bottom_card) CardView bottomCard;
    @BindView(R.id.bottom_card_expand_button) ImageView bottomCardExpandButton;
    @BindView(R.id.bottom_card_title) TextView bottomCardTitle;
    @BindView(R.id.bottom_card_subtitle) TextView bottomCardSubtitle;
    @BindView(R.id.bottom_card_next) Button next;
    @BindView(R.id.bottom_card_previous) Button previous;
    @BindView(R.id.bottom_card_add_desc) Button bottomCardAddDescription;
    @BindView(R.id.categories_subtitle) TextView categoriesSubtitle;
    @BindView(R.id.license_subtitle) TextView licenseSubtitle;
    @BindView(R.id.please_wait_text_view) TextView pleaseWaitTextView;

    //Right Card
    @BindView(R.id.right_card) CardView rightCard;
    @BindView(R.id.right_card_expand_button) ImageView rightCardExpandButton;
    @BindView(R.id.right_card_map_button) View rightCardMapButton;

    // Category Search
    @BindView(R.id.categories_title) TextView categoryTitle;
    @BindView(R.id.category_next) Button categoryNext;
    @BindView(R.id.category_previous) Button categoryPrevious;
    @BindView(R.id.categoriesSearchInProgress) ProgressBar categoriesSearchInProgress;
    @BindView(R.id.category_search) EditText categoriesSearch;
    @BindView(R.id.category_search_container) TextInputLayout categoriesSearchContainer;
    @BindView(R.id.categories) RecyclerView categoriesList;

    // Final Submission
    @BindView(R.id.license_title) TextView licenseTitle;
    @BindView(R.id.share_license_summary) TextView licenseSummary;
    @BindView(R.id.media_upload_policy) TextView licensePolicy;
    @BindView(R.id.license_list) Spinner licenseSpinner;
    @BindView(R.id.submit) Button submit;
    @BindView(R.id.license_previous) Button licensePrevious;
    @BindView(R.id.rv_descriptions) RecyclerView rvDescriptions;

    private DescriptionsAdapter descriptionsAdapter;
    private RVRendererAdapter<CategoryItem> categoriesAdapter;
    private CompositeDisposable compositeDisposable;


    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_upload);
        ButterKnife.bind(this);
        compositeDisposable = new CompositeDisposable();

        configureLayout();
        configureTopCard();
        configureBottomCard();
        initRecyclerView();
        configureRightCard();
        configureNavigationButtons();
        configureCategories();
        configureLicenses();
        configurePolicy();

        presenter.init();

        PermissionUtils.checkPermissionsAndPerformAction(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                this::receiveSharedItems,
                R.string.storage_permission_title,
                R.string.write_storage_permission_rationale_for_image_share);
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
    protected void onDestroy() {
        presenter.cleanup();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIfLoggedIn();

        checkStoragePermissions();
        compositeDisposable.add(
                RxTextView.textChanges(categoriesSearch)
                        .doOnEach(v -> categoriesSearchContainer.setError(null))
                        .takeUntil(RxView.detaches(categoriesSearch))
                        .debounce(500, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(filter -> updateCategoryList(filter.toString()), Timber::e)
        );
    }

    private void checkStoragePermissions() {
        PermissionUtils.checkPermissionsAndPerformAction(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                () -> presenter.addView(this),
                R.string.storage_permission_title,
                R.string.write_storage_permission_rationale_for_image_share);
    }

    @Override
    protected void onPause() {
        presenter.removeView();
        compositeDisposable.dispose();
        compositeDisposable = new CompositeDisposable();
        super.onPause();
    }

    @Override
    public void updateThumbnails(List<UploadModel.UploadItem> uploads) {
        int uploadCount = uploads.size();
        topCardThumbnails.setAdapter(new UploadThumbnailsAdapterFactory(presenter::thumbnailClicked).create(uploads));
        topCardTitle.setText(getResources().getQuantityString(R.plurals.upload_count_title, uploadCount, uploadCount));
    }

    @Override
    public void updateRightCardContent(boolean gpsPresent) {
        if(gpsPresent){
            rightCardMapButton.setVisibility(View.VISIBLE);
        }else{
            rightCardMapButton.setVisibility(View.GONE);
        }
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
        bottomCardTitle.setText(cardTitle);
        bottomCardSubtitle.setText(cardSubTitle);
        categoryTitle.setText(cardTitle);
        licenseTitle.setText(cardTitle);
        if (currentStep == stepCount) {
            dismissKeyboard();
        }
        if(isShowingItem) {
            descriptionsAdapter.setItems(uploadItem.title, uploadItem.descriptions);
            rvDescriptions.setAdapter(descriptionsAdapter);
        }
    }

    @Override
    public void updateLicenses(List<String> licenses, String selectedLicense) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, licenses);
        licenseSpinner.setAdapter(adapter);

        int position = licenses.indexOf(getString(Utils.licenseNameFor(selectedLicense)));

        // Check position is valid
        if (position < 0) {
            Timber.d("Invalid position: %d. Using default license", position);
            position = licenses.size() - 1;
        }

        Timber.d("Position: %d %s", position, getString(Utils.licenseNameFor(selectedLicense)));
        licenseSpinner.setSelection(position);
    }

    @SuppressLint("StringFormatInvalid")
    @Override
    public void updateLicenseSummary(String selectedLicense, int imageCount) {
        String licenseHyperLink = "<a href='" + Utils.licenseUrlFor(selectedLicense) + "'>" +
                getString(Utils.licenseNameFor(selectedLicense)) + "</a><br>";

          setTextViewHTML(licenseSummary, getResources().getQuantityString(R.plurals.share_license_summary, imageCount, licenseHyperLink));
    }

    @Override
    public void updateTopCardContent() {
        RecyclerView.Adapter adapter = topCardThumbnails.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void setNextEnabled(boolean available) {
        next.setEnabled(available);
        categoryNext.setEnabled(available);
    }

    @Override
    public void setSubmitEnabled(boolean available) {
        submit.setEnabled(available);
    }

    @Override
    public void setPreviousEnabled(boolean available) {
        previous.setEnabled(available);
        categoryPrevious.setEnabled(available);
        licensePrevious.setEnabled(available);
    }

    @Override
    public void setTopCardState(boolean state) {
        updateCardState(state, topCardExpandButton, topCardThumbnails);
    }

    @Override
    public void setTopCardVisibility(boolean visible) {
        topCard.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setBottomCardVisibility(boolean visible) {
        bottomCard.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setRightCardVisibility(boolean visible) {
        rightCard.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setBottomCardVisibility(@UploadPage int page, int uploadCount) {
        if (page == TITLE_CARD) {
            viewFlipper.setDisplayedChild(0);
        } else if (page == CATEGORIES) {
            viewFlipper.setDisplayedChild(1);
        } else if (page == LICENSE) {
            viewFlipper.setDisplayedChild(2);
            dismissKeyboard();
        } else if (page == PLEASE_WAIT) {
            viewFlipper.setDisplayedChild(3);
            pleaseWaitTextView.setText(getResources().getQuantityText(R.plurals.receiving_shared_content, uploadCount));
        }
    }

    /**
     * Only show the subtitle ("For all images in set") if multiple images being uploaded
     * @param imageCount Number of images being uploaded
     */
    @Override
    public void updateSubtitleVisibility(int imageCount) {
        categoriesSubtitle.setVisibility(imageCount > 1 ? View.VISIBLE : View.GONE);
        licenseSubtitle.setVisibility(imageCount > 1 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setBottomCardState(boolean state) {
        updateCardState(state, bottomCardExpandButton, rvDescriptions, previous, next, bottomCardAddDescription);
    }

    @Override
    public void setRightCardState(boolean state) {
        rightCardExpandButton.animate().rotation(rightCardExpandButton.getRotation() + (state ? -180 : 180)).start();
        //Add all items in rightCard here
        rightCardMapButton.setVisibility(state ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setBackground(Uri mediaUri) {
        background.setImageURI(mediaUri);
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
    public void showBadPicturePopup(@Result int result) {
        if (result >= 8 ) { // If location of image and nearby does not match, then set shared preferences to disable wikidata edits
            directPrefs.edit().putBoolean("Picture_Has_Correct_Location",false);
        }
        String errorMessageForResult = getErrorMessageForResult(this, result);
        if (StringUtils.isNullOrWhiteSpace(errorMessageForResult)) {
            return;
        }

        DialogUtil.showAlertDialog(this,
                getString(R.string.warning),
                errorMessageForResult,
                () -> presenter.deletePicture(),
                () -> presenter.keepPicture());
    }

    @Override
    public void showDuplicatePicturePopup() {
        DialogUtil.showAlertDialog(this,
                getString(R.string.warning),
                String.format(getString(R.string.upload_title_duplicate), presenter.getCurrentImageFileName()),
                null,
                () -> {
                    presenter.keepPicture();
                    presenter.handleNext(descriptionsAdapter.getTitle(), getDescriptions());
                });
    }

    public void showNoCategorySelectedWarning() {
        DialogUtil.showAlertDialog(this,
                getString(R.string.no_categories_selected),
                getString(R.string.no_categories_selected_warning_desc),
                getString(R.string.no_go_back),
                getString(R.string.yes_submit),
                null,
                () -> presenter.handleCategoryNext(categoriesModel, true));
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
    protected void onAuthCookieAcquired(String authCookie) {
        mwApi.setAuthCookie(authCookie);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CommonsApplication.OPEN_APPLICATION_DETAIL_SETTINGS) {
            //TODO: Confirm if handling manual permission enabled is required
        }
    }


    @Override
    protected void onAuthFailure() {
        Toast.makeText(this, R.string.authentication_failed, Toast.LENGTH_LONG).show();
        finish();
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
        licenseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String licenseName = parent.getItemAtPosition(position).toString();
                presenter.selectLicense(licenseName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                presenter.selectLicense(null);
            }
        });
    }

    private void configureLayout() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            cardLayout.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        }
        background.setScaleType(ImageView.ScaleType.CENTER_CROP);
        background.setOnScaleChangeListener((scaleFactor, x, y) -> presenter.closeAllCards());
    }

    private void configureTopCard() {
        topCardExpandButton.setOnClickListener(v -> presenter.toggleTopCardState());
        topCardThumbnails.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false));
    }

    private void configureBottomCard() {
        bottomCardExpandButton.setOnClickListener(v -> presenter.toggleBottomCardState());
        bottomCardAddDescription.setOnClickListener(v -> addNewDescription());
    }

    private void addNewDescription() {
        descriptionsAdapter.addDescription(new Description());
        rvDescriptions.scrollToPosition(descriptionsAdapter.getItemCount() - 1);
    }

    private void configureRightCard() {
        rightCardExpandButton.setOnClickListener(v -> presenter.toggleRightCardState());
        rightCardMapButton.setOnClickListener(v -> presenter.openCoordinateMap());
    }

    private void configureNavigationButtons() {
        // Navigation next / previous for each image as we're collecting title + description
        next.setOnClickListener(v -> {
            setTitleAndDescriptions();
            presenter.handleNext(descriptionsAdapter.getTitle(),
                    descriptionsAdapter.getDescriptions());
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
        });

    }

    private void setTitleAndDescriptions() {
        List<Description> descriptions = descriptionsAdapter.getDescriptions();
        Timber.d("Descriptions size is %d are %s", descriptions.size(), descriptions);
    }

    private void configureCategories() {
        categoriesAdapter = new UploadCategoriesAdapterFactory(categoriesModel).create(new ArrayList<>());
        categoriesList.setLayoutManager(new LinearLayoutManager(this));
        categoriesList.setAdapter(categoriesAdapter);
    }

    private void configurePolicy() {
        setTextViewHTML(licensePolicy, getString(R.string.media_upload_policy));
    }

    @SuppressLint("CheckResult")
    private void updateCategoryList(String filter) {
        List<String> imageTitleList = presenter.getImageTitleList();
        Observable.fromIterable(categoriesModel.getSelectedCategories())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> {
                    categoriesSearchInProgress.setVisibility(View.VISIBLE);
                    categoriesSearchContainer.setError(null);
                    categoriesAdapter.clear();
                })
                .observeOn(Schedulers.io())
                .concatWith(
                        categoriesModel.searchAll(filter, imageTitleList)
                                .mergeWith(categoriesModel.searchCategories(filter, imageTitleList))
                                .concatWith(TextUtils.isEmpty(filter)
                                        ? categoriesModel.defaultCategories(imageTitleList) : Observable.empty())
                )
                .filter(categoryItem -> !categoriesModel.containsYear(categoryItem.getName()))
                .distinct()
                .sorted(categoriesModel.sortBySimilarity(filter))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        s -> categoriesAdapter.add(s),
                        Timber::e,
                        () -> {
                            categoriesAdapter.notifyDataSetChanged();
                            categoriesSearchInProgress.setVisibility(View.GONE);

                            if (categoriesAdapter.getItemCount() == categoriesModel.selectedCategoriesCount()
                                    && !categoriesSearch.getText().toString().isEmpty()) {
                                categoriesSearchContainer.setError("No categories found");
                            }
                        }
                );
    }

    private void receiveSharedItems() {
        Intent intent = getIntent();
        String mimeType = intent.getType();
        String source;

        if (intent.hasExtra(UploadService.EXTRA_SOURCE)) {
            source = intent.getStringExtra(UploadService.EXTRA_SOURCE);
        } else {
            source = Contribution.SOURCE_EXTERNAL;
        }

        Timber.d("Received intent %s with action %s and mimeType %s from source %s",
                intent.toString(),
                intent.getAction(),
                mimeType,
                source);

        ArrayList<Uri> urisList = new ArrayList<>();

        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri mediaUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (mediaUri != null) {
                urisList.add(mediaUri);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            urisList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            Timber.i("Received multiple upload %s", urisList.size());
        }

        if (urisList.isEmpty()) {
            handleNullMedia();
            return;
        }

        if (intent.getBooleanExtra("isDirectUpload", false)) {
            String imageTitle = directPrefs.getString("Title", "");
            String imageDesc = directPrefs.getString("Desc", "");
            Timber.i("Received direct upload with title %s and description %s", imageTitle, imageDesc);
            String wikiDataEntityIdPref = intent.getStringExtra(WIKIDATA_ENTITY_ID_PREF);
            String wikiDataItemLocation = intent.getStringExtra(WIKIDATA_ITEM_LOCATION);
            presenter.receiveDirect(urisList.get(0), mimeType, source, wikiDataEntityIdPref, imageTitle, imageDesc, wikiDataItemLocation);
        } else {
            presenter.receive(urisList, mimeType, source);
        }

        resetDirectPrefs();
    }

    public void resetDirectPrefs() {
        SharedPreferences.Editor editor = directPrefs.edit();
        editor.remove("Title");
        editor.remove("Desc");
        editor.remove("Category");
        editor.remove(WIKIDATA_ENTITY_ID_PREF);
        editor.remove(WIKIDATA_ITEM_LOCATION);
        editor.remove(IS_DIRECT_UPLOAD);
        editor.apply();
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

    @Override
    public List<Description> getDescriptions() {
        return descriptionsAdapter.getDescriptions();
    }

    private void initRecyclerView() {
        descriptionsAdapter = new DescriptionsAdapter(this);
        descriptionsAdapter.setCallback(this::showInfoAlert);
        rvDescriptions.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        rvDescriptions.setAdapter(descriptionsAdapter);
        addNewDescription();
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
        SimilarImageDialogFragment newFragment = new SimilarImageDialogFragment();
        Bundle args = new Bundle();
        args.putString("originalImagePath", originalFilePath);
        args.putString("possibleImagePath", possibleFilePath);
        newFragment.setArguments(args);
        newFragment.show(getSupportFragmentManager(), "dialog");
    }
}
