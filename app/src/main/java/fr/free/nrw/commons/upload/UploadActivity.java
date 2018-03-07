package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.facebook.drawee.view.SimpleDraweeView;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.pedrogomez.renderers.RVRendererAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.category.CategoriesModel;
import fr.free.nrw.commons.category.CategoryItem;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.utils.AbstractTextWatcher;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static fr.free.nrw.commons.category.CategoriesModel.sortBySimilarity;

public class UploadActivity extends AuthenticatedActivity implements UploadView {
    @Inject
    InputMethodManager inputMethodManager;
    @Inject
    MediaWikiApi mwApi;
    @Inject
    UploadPresenter presenter;
    @Inject
    UploadThumbnailsAdapterFactory adapterFactory;
    @Inject
    CategoriesModel categoriesModel;

    // Main GUI
    @BindView(R.id.backgroundImage)
    SimpleDraweeView background;
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
    @BindView(R.id.bottom_card_expand_button)
    ImageView bottomCardExpandButton;
    @BindView(R.id.bottom_card_title)
    TextView bottomCardTitle;
    @BindView(R.id.bottom_card_content)
    View bottomCardContent;
    @BindView(R.id.bottom_card_next)
    Button next;
    @BindView(R.id.bottom_card_previous)
    Button previous;
    @BindView(R.id.image_title)
    EditText imageTitle;
    @BindView(R.id.image_description)
    EditText imageDescription;

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
    @BindView(R.id.submit)
    Button submit;
    @BindView(R.id.license_previous)
    Button licensePrevious;

    private RVRendererAdapter<CategoryItem> categoriesAdapter;
    private final UploadCategoriesAdapterFactory categoryAdapterFactory = new UploadCategoriesAdapterFactory(item -> {
        if (item.isSelected()) {
            categoriesModel.selectCategory(item);
            categoriesModel.updateCategoryCount(item);
        } else {
            categoriesModel.unselectCategory(item);
        }
    });

    private AbstractTextWatcher titleWatcher;
    private AbstractTextWatcher descriptionWatcher;
    private CompositeDisposable compositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        ButterKnife.bind(this);

        topCardThumbnails.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false));

        titleWatcher = new AbstractTextWatcher(this::handleImageTitleChanged);
        descriptionWatcher = new AbstractTextWatcher(this::handleImageDescriptionChanged);
        adapterFactory.setListener(presenter::thumbnailClicked);
        topCardExpandButton.setOnClickListener(v -> presenter.toggleTopCardState());
        bottomCardExpandButton.setOnClickListener(v -> presenter.toggleBottomCardState());
        next.setOnClickListener(v -> presenter.handleNext());
        previous.setOnClickListener(v -> presenter.handlePrevious());
        categoryNext.setOnClickListener(v -> presenter.handleNext());
        categoryPrevious.setOnClickListener(v -> presenter.handlePrevious());
        licensePrevious.setOnClickListener(v -> presenter.handlePrevious());
        submit.setOnClickListener(v -> presenter.handleSubmit());

        ArrayList<CategoryItem> items = new ArrayList<>();
        if (savedInstanceState != null) {
            items.addAll(savedInstanceState.getParcelableArrayList("currentCategories"));
            //noinspection unchecked
            categoriesModel.cacheAll((HashMap<String, ArrayList<String>>) savedInstanceState
                    .getSerializable("categoriesCache"));
        }
        categoriesAdapter = categoryAdapterFactory.create(items);
        categoriesList.setLayoutManager(new LinearLayoutManager(this));
        categoriesList.setAdapter(categoriesAdapter);

        presenter.init(savedInstanceState);
        receiveSharedItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        compositeDisposable = new CompositeDisposable();
        presenter.addView(this);
        imageTitle.addTextChangedListener(titleWatcher);
        imageDescription.addTextChangedListener(descriptionWatcher);
        compositeDisposable.add(
                RxTextView.textChanges(categoriesSearch)
                        .doOnEach(v -> categoriesSearchContainer.setError(null))
                        .takeUntil(RxView.detaches(categoriesSearch))
                        .debounce(500, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(filter -> updateCategoryList(filter.toString()))
        );
    }

    @SuppressLint("CheckResult")
    private void updateCategoryList(String filter) {
        Observable.fromIterable(categoriesModel.getSelectedCategories())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> {
                    categoriesSearchInProgress.setVisibility(View.VISIBLE);
                    categoriesSearchContainer.setError(null);
//                    categoriesNotFoundView.setVisibility(View.GONE);
//                    categoriesSkip.setVisibility(View.GONE);
                    categoriesAdapter.clear();
                })
                .observeOn(Schedulers.io())
                .concatWith(
                        categoriesModel.searchAll(filter)
                                .mergeWith(categoriesModel.searchCategories(filter))
                                .concatWith(TextUtils.isEmpty(filter)
                                        ? categoriesModel.defaultCategories() : Observable.empty())
                )
                .filter(categoryItem -> !CategoriesModel.containsYear(categoryItem.getName()))
                .distinct()
                .sorted(sortBySimilarity(filter))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        s -> categoriesAdapter.add(s),
                        Timber::e,
                        () -> {
                            categoriesAdapter.notifyDataSetChanged();
                            categoriesSearchInProgress.setVisibility(View.GONE);

                            if (categoriesAdapter.getItemCount() == categoriesModel.selectedCategoriesCount()
                                    && !categoriesSearch.getText().toString().isEmpty()) {
                                // There are no suggestions
//                                if (TextUtils.isEmpty(filter)) {
//                                    // Allow to send image with no categories
//                                    categoriesSkip.setVisibility(View.VISIBLE);
//                                } else {
//                                    // Inform the user that the searched term matches  no category
//                                    categoriesNotFoundView.setText(getString(R.string.categories_not_found, filter));
//                                    categoriesNotFoundView.setVisibility(View.VISIBLE);
//                                }
                                categoriesSearchContainer.setError("No categories found");
                            }
                        }
                );
    }

    @Override
    protected void onPause() {
        presenter.removeView();
        imageTitle.removeTextChangedListener(titleWatcher);
        imageDescription.removeTextChangedListener(descriptionWatcher);
        compositeDisposable.dispose();
        super.onPause();
    }

    @Override
    public void updateThumbnails(List<UploadModel.UploadItem> uploads) {
        int uploadCount = uploads.size();
        topCardThumbnails.setAdapter(adapterFactory.create(uploads));
        topCardTitle.setText(getResources().getQuantityString(R.plurals.upload_count_title, uploadCount, uploadCount));
    }

    @Override
    public void updateBottomCardContent(int currentStep, int stepCount, UploadModel.UploadItem uploadItem) {
        String title = getResources().getString(R.string.step_count, currentStep, stepCount);
        bottomCardTitle.setText(title);
        categoryTitle.setText(title);
        licenseTitle.setText(title);
        imageTitle.setText(uploadItem.title);
        imageDescription.setText(uploadItem.description);
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
    public void setBottomCardVisibility(@UploadPage int page) {
        if (page == TITLE_CARD) {
            viewFlipper.setDisplayedChild(0);
        } else if (page == CATEGORIES) {
            viewFlipper.setDisplayedChild(1);
        } else if (page == LICENSE) {
            viewFlipper.setDisplayedChild(2);
        }
    }

    @Override
    public void setBottomCardState(boolean state) {
        updateCardState(state, bottomCardExpandButton, bottomCardContent);
    }

    @Override
    public void setBackground(Uri mediaUri) {
        background.setImageURI(mediaUri);
    }

    @Override
    public void dismissKeyboard() {
        inputMethodManager.hideSoftInputFromWindow(imageTitle.getWindowToken(), 0);
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
            presenter.receive(mediaUri, mimeType, source);
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            ArrayList<Uri> urisList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            presenter.receive(urisList, mimeType, source);
        }
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
    protected void onAuthFailure() {
        Toast failureToast = Toast.makeText(this, R.string.authentication_failed, Toast.LENGTH_LONG);
        failureToast.show();
        finish();
    }

    private void handleImageDescriptionChanged() {
        presenter.descriptionChanged(imageDescription.getText().toString());
    }

    private void handleImageTitleChanged() {
        presenter.imageTitleChanged(imageTitle.getText().toString());
    }

    private void updateCardState(boolean state, ImageView button, View... content) {
        button.setImageResource(state
                ? R.drawable.ic_expand_less_black_24dp
                : R.drawable.ic_expand_more_black_24dp);
        if (content != null) {
            for (View view : content) {
                view.setVisibility(state
                        ? View.VISIBLE : View.GONE);
            }
        }
    }
}
