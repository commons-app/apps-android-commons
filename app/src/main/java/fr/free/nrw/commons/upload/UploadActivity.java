package fr.free.nrw.commons.upload;

import android.Manifest;
import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
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
import android.text.TextUtils;
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
import java.util.HashMap;
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
import fr.free.nrw.commons.utils.ImageUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static fr.free.nrw.commons.wikidata.WikidataConstants.WIKIDATA_ENTITY_ID_PREF;

public class UploadActivity extends AuthenticatedActivity implements UploadView {
    @Inject
    InputMethodManager inputMethodManager;
    @Inject
    MediaWikiApi mwApi;


    @Inject
    @Named("direct_nearby_upload_prefs")
    SharedPreferences directPrefs;

    @Inject
    UploadPresenter presenter;
    @Inject
    CategoriesModel categoriesModel;

    // Main GUI
    @BindView(R.id.backgroundImage)
    PhotoView background;
    @BindView(R.id.activity_upload_cards)
    ConstraintLayout cardLayout;
    @BindView(R.id.view_flipper)
    ViewFlipper viewFlipper;

    // Top Card
    @BindView(R.id.top_card)
    CardView topCard;
    @BindView(R.id.top_card_expand_button)
    ImageView topCardExpandButton;
    @BindView(R.id.top_card_title)
    TextView topCardTitle;
    @BindView(R.id.top_card_thumbnails)
    RecyclerView topCardThumbnails;

    // Bottom Card
    @BindView(R.id.bottom_card)
    CardView bottomCard;
    @BindView(R.id.bottom_card_expand_button)
    ImageView bottomCardExpandButton;
    @BindView(R.id.bottom_card_title)
    TextView bottomCardTitle;
    @BindView(R.id.bottom_card_next)
    Button next;
    @BindView(R.id.bottom_card_previous)
    Button previous;
    @BindView(R.id.bottom_card_add_desc)
    Button bottomCardAddDescription;

    //Right Card
    @BindView(R.id.right_card)
    CardView rightCard;
    @BindView(R.id.right_card_expand_button)
    ImageView rightCardExpandButton;
    @BindView(R.id.right_card_map_button)
    View rightCardMapButton;

    // Category Search
    @BindView(R.id.categories_title)
    TextView categoryTitle;
    @BindView(R.id.category_next)
    Button categoryNext;
    @BindView(R.id.category_previous)
    Button categoryPrevious;
    @BindView(R.id.categoriesSearchInProgress)
    ProgressBar categoriesSearchInProgress;
    @BindView(R.id.category_search)
    EditText categoriesSearch;
    @BindView(R.id.category_search_container)
    TextInputLayout categoriesSearchContainer;
    @BindView(R.id.categories)
    RecyclerView categoriesList;

    // Final Submission
    @BindView(R.id.license_title)
    TextView licenseTitle;
    @BindView(R.id.share_license_summary)
    TextView licenseSummary;
    @BindView(R.id.media_upload_policy)
    TextView licensePolicy;
    @BindView(R.id.license_list)
    Spinner licenseSpinner;
    @BindView(R.id.submit)
    Button submit;
    @BindView(R.id.license_previous)
    Button licensePrevious;
    @BindView(R.id.rv_descriptions)
    RecyclerView rvDescriptions;

    private DescriptionsAdapter descriptionsAdapter;
    private RVRendererAdapter<CategoryItem> categoriesAdapter;
    private CompositeDisposable compositeDisposable;

    DexterPermissionObtainer dexterPermissionObtainer;


    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_upload);
        ButterKnife.bind(this);
        compositeDisposable = new CompositeDisposable();

        configureCategories(savedInstanceState);
        configureLicenses();
        configureLayout();
        configureTopCard();
        configureBottomCard();
        initRecyclerView();
        configureRightCard();
        configureNavigationButtons();

        //storagePermissionReady = BehaviorSubject.createDefault(false);
        //storagePermissionReady.subscribe(b -> Timber.i("storagePermissionReady:" + b));
        presenter.initFromSavedState(savedInstanceState);

        dexterPermissionObtainer = new DexterPermissionObtainer(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                getString(R.string.storage_permission),
                getString(R.string.write_storage_permission_rationale_for_image_share));

        dexterPermissionObtainer.confirmStoragePermissions().subscribe(this::receiveSharedItems);
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
        compositeDisposable.add(
                dexterPermissionObtainer.confirmStoragePermissions()
                        .subscribe(() -> presenter.addView(this)));
        compositeDisposable.add(
                RxTextView.textChanges(categoriesSearch)
                        .doOnEach(v -> categoriesSearchContainer.setError(null))
                        .takeUntil(RxView.detaches(categoriesSearch))
                        .debounce(500, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(filter -> updateCategoryList(filter.toString()), Timber::e)
        );
    }

    @Override
    protected void onPause() {
        presenter.removeView();
//        imageTitle.removeTextChangedListener(titleWatcher);
//        imageDescription.removeTextChangedListener(descriptionWatcher);
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
    public void updateBottomCardContent(int currentStep, int stepCount, UploadModel.UploadItem uploadItem) {
        String cardTitle = getResources().getString(R.string.step_count, currentStep, stepCount);
        bottomCardTitle.setText(cardTitle);
        categoryTitle.setText(cardTitle);
        licenseTitle.setText(cardTitle);
        if (!uploadItem.isDummy()) {
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

    @Override
    @SuppressLint("StringFormatInvalid")
    public void updateLicenseSummary(String selectedLicense) {
        licenseSummary.setText(getString(R.string.share_license_summary, getString(Utils.licenseNameFor(selectedLicense))));
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
    public void setBottomCardVisibility(@UploadPage int page) {
        if (page == TITLE_CARD) {
            viewFlipper.setDisplayedChild(0);
        } else if (page == CATEGORIES) {
            viewFlipper.setDisplayedChild(1);
        } else if (page == LICENSE) {
            viewFlipper.setDisplayedChild(2);
        } else if (page == PLEASE_WAIT) {
            viewFlipper.setDisplayedChild(3);
        }
    }

    @Override
    public void setBottomCardState(boolean state) {
        updateCardState(state, bottomCardExpandButton, rvDescriptions, previous, next);
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
//        inputMethodManager.hideSoftInputFromWindow(imageTitle.getWindowToken(), 0);
    }

    @Override
    public void showBadPicturePopup(@ImageUtils.Result int result) {
        int errorMessage;
        if (result == ImageUtils.IMAGE_DARK)
            errorMessage = R.string.upload_image_problem_dark;
        else if (result == ImageUtils.IMAGE_BLURRY)
            errorMessage = R.string.upload_image_problem_blurry;
        else if (result == ImageUtils.IMAGE_DUPLICATE)
            errorMessage = R.string.upload_image_problem_duplicate;
        else if (result == (ImageUtils.IMAGE_DARK|ImageUtils.IMAGE_BLURRY))
            errorMessage = R.string.upload_image_problem_dark_blurry;
        else if (result == (ImageUtils.IMAGE_DARK|ImageUtils.IMAGE_DUPLICATE))
            errorMessage = R.string.upload_image_problem_dark_duplicate;
        else if (result == (ImageUtils.IMAGE_BLURRY|ImageUtils.IMAGE_DUPLICATE))
            errorMessage = R.string.upload_image_problem_blurry_duplicate;
        else if (result == (ImageUtils.IMAGE_DARK|ImageUtils.IMAGE_BLURRY|ImageUtils.IMAGE_DUPLICATE))
            errorMessage = R.string.upload_image_problem_dark_blurry_duplicate;
        else
            return;
        AlertDialog.Builder errorDialogBuilder = new AlertDialog.Builder(this);
        errorDialogBuilder.setMessage(errorMessage);
        errorDialogBuilder.setTitle(R.string.warning);
        //user does not wish to upload the picture, delete it
        errorDialogBuilder.setPositiveButton(R.string.no, (dialogInterface, i) -> {
            presenter.deletePicture();
            dialogInterface.dismiss();
        });
        //user wishes to go ahead with the upload of this picture, just dismiss this dialog
        errorDialogBuilder.setNegativeButton(R.string.yes, (DialogInterface dialogInterface, int i) -> {
            presenter.keepPicture();
            dialogInterface.dismiss();
        });

        AlertDialog errorDialog = errorDialogBuilder.create();
        if (!isFinishing()) {
            errorDialog.show();
        }
    }

    public void showDuplicateTitlePopup(String title) {
        showInfoAlert(R.string.warning, R.string.upload_title_duplicate, title);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle state = presenter.getSavedState();
        outState.putAll(state);
        int itemCount = categoriesAdapter.getItemCount();
        ArrayList<CategoryItem> items = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            items.add(categoriesAdapter.getItem(i));
        }
        outState.putParcelableArrayList("currentCategories", items);
        outState.putSerializable("categoriesCache", categoriesModel.getCategoriesCache());
    }

    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        mwApi.setAuthCookie(authCookie);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CommonsApplication.OPEN_APPLICATION_DETAIL_SETTINGS) {
            dexterPermissionObtainer.onManualPermissionReturned();
        }
    }


    @Override
    protected void onAuthFailure() {
        Toast failureToast = Toast.makeText(this, R.string.authentication_failed, Toast.LENGTH_LONG);
        failureToast.show();
        finish();
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
        bottomCardAddDescription.setOnClickListener(v -> {
            descriptionsAdapter.addDescription(new Description());
            rvDescriptions.scrollToPosition(descriptionsAdapter.getItemCount() - 1);
        });
    }

    private void configureRightCard() {
        rightCardExpandButton.setOnClickListener(v -> presenter.toggleRightCardState());
        rightCardMapButton.setOnClickListener(v -> presenter.openCoordinateMap());
    }

    private void configureNavigationButtons() {
        // Navigation next / previous for each image as we're collecting title + description
        next.setOnClickListener(v -> presenter.handleNext());
        previous.setOnClickListener(v -> presenter.handlePrevious());

        // Next / previous for the category selection page
        categoryNext.setOnClickListener(v -> presenter.handleNext());
        categoryPrevious.setOnClickListener(v -> presenter.handlePrevious());

        // Finally, the previous / submit buttons on the final page of the wizard
        licensePrevious.setOnClickListener(v -> presenter.handlePrevious());
        submit.setOnClickListener(v -> {
            presenter.handleSubmit(categoriesModel);
            finish();
        });

    }

    private void configureCategories(Bundle savedInstanceState) {
        ArrayList<CategoryItem> items = new ArrayList<>();
        if (savedInstanceState != null) {
            items.addAll(savedInstanceState.getParcelableArrayList("currentCategories"));
            //noinspection unchecked
            categoriesModel.cacheAll((HashMap<String, ArrayList<String>>) savedInstanceState
                    .getSerializable("categoriesCache"));
        }
        categoriesAdapter = new UploadCategoriesAdapterFactory(categoriesModel).create(items);
        categoriesList.setLayoutManager(new LinearLayoutManager(this));
        categoriesList.setAdapter(categoriesAdapter);
    }

    @SuppressLint("CheckResult")
    private void updateCategoryList(String filter) {
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
                        categoriesModel.searchAll(filter)
                                .mergeWith(categoriesModel.searchCategories(filter))
                                .concatWith(TextUtils.isEmpty(filter)
                                        ? categoriesModel.defaultCategories() : Observable.empty())
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

        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri mediaUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (intent.getBooleanExtra("isDirectUpload", false)) {
                String imageTitle = directPrefs.getString("Title", "");
                String imageDesc = directPrefs.getString("Desc", "");
                Timber.i("Received direct upload with title" + imageTitle + "and description %s" + imageDesc);
                String wikidataEntityIdPref = intent.getStringExtra(WIKIDATA_ENTITY_ID_PREF);
                presenter.receiveDirect(mediaUri, mimeType, source, wikidataEntityIdPref, imageTitle, imageDesc);
            } else {
                Timber.i("Received single upload");
                presenter.receive(mediaUri, mimeType, source);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            ArrayList<Uri> urisList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            Timber.i("Received multiple upload");
            presenter.receive(urisList, mimeType, source);
        }
    }

    private void updateCardState(boolean state, ImageView button, View... content) {
        button.animate().rotation(button.getRotation() + (state ? 180 : -180)).start();
        if (content != null) {
            for (View view : content) {
                view.setVisibility(state ? View.VISIBLE : View.GONE);
            }
        }
    }

    public void launchMapActivity(String decCoords) {
        try {
            Uri gmmIntentUri = Uri.parse("google.streetview:cbll=" + decCoords);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        } catch (ActivityNotFoundException ex) {
            AlertDialog.Builder errorDialogBuilder = new AlertDialog.Builder(this);
            errorDialogBuilder.setMessage(R.string.map_application_missing);
            errorDialogBuilder.setTitle(R.string.warning);
            //just dismiss the dialog
            errorDialogBuilder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
            });

            AlertDialog errorDialog = errorDialogBuilder.create();
            if (!isFinishing()) {
                errorDialog.show();
            }
        }
    }

    @Override
    public List<Description> getDescriptions() {
        return descriptionsAdapter.getDescriptions();
    }

    private void initRecyclerView() {
        descriptionsAdapter = new DescriptionsAdapter();
        descriptionsAdapter.setCallback(this::showInfoAlert);
        rvDescriptions.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        rvDescriptions.setAdapter(descriptionsAdapter);
        compositeDisposable.add(
                descriptionsAdapter.getTitleChangeObserver()
                        .debounce(1000, TimeUnit.MILLISECONDS)
                        .observeOn(Schedulers.io())
                        .filter(title -> mwApi.fileExistsWithName(title + "." + presenter.getCurrentItem().fileExt))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(title -> showDuplicateTitlePopup(title + "." + presenter.getCurrentItem().fileExt), Timber::e)
        );
    }


    private void showInfoAlert(int titleStringID, int messageStringId, String... formatArgs) {
        new AlertDialog.Builder(this)
                .setTitle(titleStringID)
                .setMessage(getString(messageStringId, (Object[]) formatArgs))
                .setCancelable(true)
                .setNeutralButton(android.R.string.ok, (dialog, id) -> dialog.cancel())
                .create()
                .show();
    }


}
