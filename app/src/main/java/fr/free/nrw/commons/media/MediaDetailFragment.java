package fr.free.nrw.commons.media;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static fr.free.nrw.commons.category.CategoryClientKt.CATEGORY_NEEDING_CATEGORIES;
import static fr.free.nrw.commons.category.CategoryClientKt.CATEGORY_PREFIX;
import static fr.free.nrw.commons.category.CategoryClientKt.CATEGORY_UNCATEGORISED;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxSearchView;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.MediaDataExtractor;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.AccountUtil;
import fr.free.nrw.commons.category.CategoryClient;
import fr.free.nrw.commons.category.CategoryDetailsActivity;
import fr.free.nrw.commons.category.CategoryEditHelper;
import fr.free.nrw.commons.category.CategoryEditSearchRecyclerViewAdapter;
import fr.free.nrw.commons.category.CategoryEditSearchRecyclerViewAdapter.Callback;
import fr.free.nrw.commons.contributions.ContributionsFragment;
import fr.free.nrw.commons.delete.DeleteHelper;
import fr.free.nrw.commons.delete.ReasonBuilder;
import fr.free.nrw.commons.explore.depictions.WikidataItemDetailsActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.nearby.Label;
import fr.free.nrw.commons.ui.widget.HtmlTextView;
import fr.free.nrw.commons.utils.ViewUtilWrapper;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.wikipedia.util.DateUtil;
import timber.log.Timber;

public class MediaDetailFragment extends CommonsDaggerSupportFragment implements Callback,
    CategoryEditHelper.Callback {

    private boolean editable;
    private boolean isCategoryImage;
    private MediaDetailPagerFragment.MediaDetailProvider detailProvider;
    private int index;
    private boolean isDeleted = false;
    private boolean isWikipediaButtonDisplayed;


    public static MediaDetailFragment forMedia(int index, boolean editable, boolean isCategoryImage, boolean isWikipediaButtonDisplayed) {
        MediaDetailFragment mf = new MediaDetailFragment();

        Bundle state = new Bundle();
        state.putBoolean("editable", editable);
        state.putBoolean("isCategoryImage", isCategoryImage);
        state.putInt("index", index);
        state.putInt("listIndex", 0);
        state.putInt("listTop", 0);
        state.putBoolean("isWikipediaButtonDisplayed", isWikipediaButtonDisplayed);

        mf.setArguments(state);

        return mf;
    }

    @Inject
    MediaDataExtractor mediaDataExtractor;
    @Inject
    ReasonBuilder reasonBuilder;
    @Inject
    DeleteHelper deleteHelper;
    @Inject
    CategoryEditHelper categoryEditHelper;
    @Inject
    ViewUtilWrapper viewUtil;
    @Inject
    CategoryClient categoryClient;

    private int initialListTop = 0;

    @BindView(R.id.mediaDetailImageView)
    SimpleDraweeView image;
    @BindView(R.id.mediaDetailImageViewLandscape)
    SimpleDraweeView imageLandscape;
    @BindView(R.id.mediaDetailImageViewSpacer)
    LinearLayout imageSpacer;
    @BindView(R.id.mediaDetailTitle)
    TextView title;
    @BindView(R.id.caption_layout)
    LinearLayout captionLayout;
    @BindView(R.id.depicts_layout)
    LinearLayout depictsLayout;
    @BindView(R.id.media_detail_caption)
    TextView mediaCaption;
    @BindView(R.id.mediaDetailDesc)
    HtmlTextView desc;
    @BindView(R.id.mediaDetailAuthor)
    TextView author;
    @BindView(R.id.mediaDetailLicense)
    TextView license;
    @BindView(R.id.mediaDetailCoordinates)
    TextView coordinates;
    @BindView(R.id.mediaDetailuploadeddate)
    TextView uploadedDate;
    @BindView(R.id.mediaDetailDisc)
    TextView mediaDiscussion;
    @BindView(R.id.seeMore)
    TextView seeMore;
    @BindView(R.id.nominatedDeletionBanner)
    LinearLayout nominatedForDeletion;
    @BindView(R.id.mediaDetailCategoryContainer)
    LinearLayout categoryContainer;
    @BindView(R.id.categoryEditButton)
    Button categoryEditButton;
    @BindView(R.id.media_detail_depiction_container)
    LinearLayout depictionContainer;
    @BindView(R.id.authorLinearLayout)
    LinearLayout authorLayout;
    @BindView(R.id.nominateDeletion)
    Button delete;
    @BindView(R.id.mediaDetailScrollView)
    ScrollView scrollView;
    @BindView(R.id.toDoLayout)
    LinearLayout toDoLayout;
    @BindView(R.id.toDoReason)
    TextView toDoReason;
    @BindView(R.id.category_edit_layout)
    LinearLayout categoryEditLayout;
    @BindView(R.id.et_search)
    SearchView categorySearchView;
    @BindView(R.id.rv_categories)
    RecyclerView categoryRecyclerView;
    @BindView(R.id.update_categories_button)
    Button updateCategoriesButton;
    @BindView(R.id.dummy_category_edit_container)
    LinearLayout dummyCategoryEditContainer;
    @BindView(R.id.pb_categories)
    ProgressBar progressbarCategories;
    @BindView(R.id.existing_categories)
    TextView existingCategories;
    @BindView(R.id.no_results_found)
    TextView noResultsFound;

    private ArrayList<String> categoryNames = new ArrayList<>();
    private String categorySearchQuery;

    /**
     * Depicts is a feature part of Structured data. Multiple Depictions can be added for an image just like categories.
     * However unlike categories depictions is multi-lingual
     * Ex: key: en value: monument
     */
    private ImageInfo imageInfoCache;
    private int oldWidthOfImageView;
    private int newWidthOfImageView;
    private boolean heightVerifyingBoolean = true; // helps in maintaining aspect ratio
    private ViewTreeObserver.OnGlobalLayoutListener layoutListener; // for layout stuff, only used once!
    private CategoryEditSearchRecyclerViewAdapter categoryEditSearchRecyclerViewAdapter;

    //Had to make this class variable, to implement various onClicks, which access the media, also I fell why make separate variables when one can serve the purpose
    private Media media;
    private ArrayList<String> reasonList;


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("index", index);
        outState.putBoolean("editable", editable);
        outState.putBoolean("isCategoryImage", isCategoryImage);
        outState.putBoolean("isWikipediaButtonDisplayed", isWikipediaButtonDisplayed);

        getScrollPosition();
        outState.putInt("listTop", initialListTop);
    }

    private void getScrollPosition() {
        initialListTop = scrollView.getScrollY();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getParentFragment() != null
            && getParentFragment() instanceof MediaDetailPagerFragment) {
            detailProvider =
                ((MediaDetailPagerFragment) getParentFragment()).getMediaDetailProvider();
        }
        if (savedInstanceState != null) {
            editable = savedInstanceState.getBoolean("editable");
            isCategoryImage = savedInstanceState.getBoolean("isCategoryImage");
            isWikipediaButtonDisplayed = savedInstanceState.getBoolean("isWikipediaButtonDisplayed");
            index = savedInstanceState.getInt("index");
            initialListTop = savedInstanceState.getInt("listTop");
        } else {
            editable = getArguments().getBoolean("editable");
            isCategoryImage = getArguments().getBoolean("isCategoryImage");
            isWikipediaButtonDisplayed = getArguments().getBoolean("isWikipediaButtonDisplayed");
            index = getArguments().getInt("index");
            initialListTop = 0;
        }

        reasonList = new ArrayList<>();
        reasonList.add(getString(R.string.deletion_reason_uploaded_by_mistake));
        reasonList.add(getString(R.string.deletion_reason_publicly_visible));
        reasonList.add(getString(R.string.deletion_reason_not_interesting));
        reasonList.add(getString(R.string.deletion_reason_no_longer_want_public));
        reasonList.add(getString(R.string.deletion_reason_bad_for_my_privacy));

        final View view = inflater.inflate(R.layout.fragment_media_detail, container, false);

        ButterKnife.bind(this,view);
        Utils.setUnderlinedText(seeMore, R.string.nominated_see_more, container.getContext());

        if (isCategoryImage){
            authorLayout.setVisibility(VISIBLE);
        } else {
            authorLayout.setVisibility(GONE);
        }

        return view;
    }

    @OnClick(R.id.mediaDetailImageViewSpacer)
    public void launchZoomActivity(View view) {
        if (media.getImageUrl() != null) {
            Context ctx = view.getContext();
            ctx.startActivity(
                new Intent(ctx, ZoomableActivity.class).setData(Uri.parse(media.getImageUrl()))
            );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getParentFragment() != null && getParentFragment().getParentFragment() != null) {
            //Added a check because, not necessarily, the parent fragment will have a parent fragment, say
            // in the case when MediaDetailPagerFragment is directly started by the CategoryImagesActivity
            if (getParentFragment() instanceof ContributionsFragment) {
                ((ContributionsFragment) (getParentFragment()
                    .getParentFragment())).nearbyNotificationCardView
                    .setVisibility(View.GONE);
            }
        }
        categoryEditSearchRecyclerViewAdapter =
            new CategoryEditSearchRecyclerViewAdapter(getContext(), new ArrayList<>(
                Label.valuesAsList()), categoryRecyclerView, categoryClient, this);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        categoryRecyclerView.setAdapter(categoryEditSearchRecyclerViewAdapter);

        media = detailProvider.getMediaAtPosition(index);
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
            new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        imageLandscape.setVisibility(VISIBLE);
                    }
                    oldWidthOfImageView = scrollView.getWidth();
                    displayMediaDetails();
                }
            }
        );
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
            new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (scrollView.getWidth() != oldWidthOfImageView) {
                        if (newWidthOfImageView == 0) {
                            newWidthOfImageView = scrollView.getWidth();
                            updateAspectRatio(newWidthOfImageView);
                        }
                        scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            }
        );
        // check orientation
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            imageLandscape.setVisibility(VISIBLE);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            imageLandscape.setVisibility(GONE);
        }
        // ensuring correct aspect ratio for landscape mode
        if (heightVerifyingBoolean) {
            updateAspectRatio(newWidthOfImageView);
            heightVerifyingBoolean = false;
        } else {
            updateAspectRatio(oldWidthOfImageView);
            heightVerifyingBoolean = true;
        }
    }

    private void displayMediaDetails() {
        setTextFields(media);
        compositeDisposable.addAll(
            mediaDataExtractor.fetchDepictionIdsAndLabels(media)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onDepictionsLoaded, Timber::e),
            mediaDataExtractor.checkDeletionRequestExists(media)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onDeletionPageExists, Timber::e),
            mediaDataExtractor.fetchDiscussion(media)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onDiscussionLoaded, Timber::e),
            mediaDataExtractor.refresh(media)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onMediaRefreshed, Timber::e)
        );
    }

    private void onMediaRefreshed(Media media) {
        setTextFields(media);
        compositeDisposable.addAll(
            mediaDataExtractor.fetchDepictionIdsAndLabels(media)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onDepictionsLoaded, Timber::e)
        );
        // compositeDisposable.add(disposable);
        setupToDo();
    }

    private void onDiscussionLoaded(String discussion) {
        mediaDiscussion.setText(prettyDiscussion(discussion.trim()));
    }

    private void onDeletionPageExists(Boolean deletionPageExists) {
        if (deletionPageExists){
            delete.setVisibility(GONE);
            nominatedForDeletion.setVisibility(VISIBLE);
        } else if (!isCategoryImage) {
            delete.setVisibility(VISIBLE);
            nominatedForDeletion.setVisibility(GONE);
        }
    }

    private void onDepictionsLoaded(List<IdAndCaptions> idAndCaptions){
      depictsLayout.setVisibility(idAndCaptions.isEmpty() ? GONE : VISIBLE);
      buildDepictionList(idAndCaptions);
    }
    /**
     * The imageSpacer is Basically a transparent overlay for the SimpleDraweeView
     * which holds the image to be displayed( moreover this image is out of
     * the scroll view )
     * @param scrollWidth the current width of the scrollView
     */
    private void updateAspectRatio(int scrollWidth) {
        if (imageInfoCache != null) {
            int finalHeight = (scrollWidth*imageInfoCache.getHeight()) / imageInfoCache.getWidth();
            ViewGroup.LayoutParams params = image.getLayoutParams();
            ViewGroup.LayoutParams spacerParams = imageSpacer.getLayoutParams();
            params.height = finalHeight;
            spacerParams.height = finalHeight;
            image.setLayoutParams(params);
            imageSpacer.setLayoutParams(spacerParams);
            imageLandscape.setLayoutParams(params);
        }
    }

    private final ControllerListener aspectRatioListener = new BaseControllerListener<ImageInfo>() {
        @Override
        public void onIntermediateImageSet(String id, @Nullable ImageInfo imageInfo) {
            imageInfoCache = imageInfo;
            updateAspectRatio(scrollView.getWidth());
        }
        @Override
        public void onFinalImageSet(String id, @Nullable ImageInfo imageInfo, @Nullable Animatable animatable) {
            imageInfoCache = imageInfo;
            updateAspectRatio(scrollView.getWidth());
        }
    };

    /**
     * Uses two image sources.
     * - low resolution thumbnail is shown initially
     * - when the high resolution image is available, it replaces the low resolution image
     */
    private void setupImageView() {

        image.getHierarchy().setPlaceholderImage(R.drawable.image_placeholder);
        image.getHierarchy().setFailureImage(R.drawable.image_placeholder);

        imageLandscape.getHierarchy().setPlaceholderImage(R.drawable.image_placeholder);
        imageLandscape.getHierarchy().setFailureImage(R.drawable.image_placeholder);

        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setLowResImageRequest(ImageRequest.fromUri(media.getThumbUrl()))
                .setImageRequest(ImageRequest.fromUri(media.getImageUrl()))
                .setControllerListener(aspectRatioListener)
                .setOldController(image.getController())
                .build();
        DraweeController controllerLandscape = Fresco.newDraweeControllerBuilder()
            .setLowResImageRequest(ImageRequest.fromUri(media.getThumbUrl()))
            .setImageRequest(ImageRequest.fromUri(media.getImageUrl()))
            .setControllerListener(aspectRatioListener)
            .setOldController(imageLandscape.getController())
            .build();
        image.setController(controller);
        imageLandscape.setController(controllerLandscape);
    }

    /**
     * Displays layout about missing actions to inform user
     * - Images that they uploaded with no categories/descriptions, so that they can add them
     * - Images that can be added to associated Wikipedia articles that have no pictures
     */
    private void setupToDo() {
        updateToDoWarning();
        compositeDisposable.add(RxSearchView.queryTextChanges(categorySearchView)
            .takeUntil(RxView.detaches(categorySearchView))
            .debounce(500, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(query -> {
                    this.categorySearchQuery = query.toString();
                    //update image list
                    if (!TextUtils.isEmpty(query)) {
                        if (categoryEditLayout.getVisibility() == VISIBLE) {
                            ((CategoryEditSearchRecyclerViewAdapter) categoryRecyclerView.getAdapter()).
                                getFilter().filter(query.toString());
                        }
                    }
                }, Timber::e
            ));
    }

    private void updateToDoWarning() {
        String toDoMessage = "";
        boolean toDoNeeded = false;
        boolean categoriesPresent = media.getCategories() == null ? false : (media.getCategories().size() == 0 ? false : true);

        // Check if the presented category is about need of category
        if (categoriesPresent) {
            for (String category : media.getCategories()) {
                if (category.toLowerCase().contains(CATEGORY_NEEDING_CATEGORIES) ||
                    category.toLowerCase().contains(CATEGORY_UNCATEGORISED)) {
                    categoriesPresent = false;
                }
                break;
            }
        }
        if (!categoriesPresent) {
            toDoNeeded = true;
            toDoMessage += getString(R.string.missing_category);
        }
        if (isWikipediaButtonDisplayed) {
            toDoNeeded = true;
            toDoMessage += (toDoMessage.isEmpty()) ? "" : "\n" + getString(R.string.missing_article);
        }

        if (toDoNeeded) {
            toDoMessage = getString(R.string.todo_improve) + "\n" + toDoMessage;
            toDoLayout.setVisibility(VISIBLE);
            toDoReason.setText(toDoMessage);
        } else {
            toDoLayout.setVisibility(GONE);
        }
    }

    @Override
    public void onDestroyView() {
        if (layoutListener != null && getView() != null) {
            getView().getViewTreeObserver().removeGlobalOnLayoutListener(layoutListener); // old Android was on crack. CRACK IS WHACK
            layoutListener = null;
        }

        compositeDisposable.clear();
        super.onDestroyView();
    }

    private void setTextFields(Media media) {
        setupImageView();
        title.setText(media.getDisplayTitle());
        desc.setHtmlText(prettyDescription(media));
        license.setText(prettyLicense(media));
        coordinates.setText(prettyCoordinates(media));
        uploadedDate.setText(prettyUploadedDate(media));
        if (prettyCaption(media).equals(getContext().getString(R.string.detail_caption_empty))) {
            captionLayout.setVisibility(GONE);
        } else {
            mediaCaption.setText(prettyCaption(media));
        }

        categoryNames.clear();
        categoryNames.addAll(media.getCategories());
        categoryEditSearchRecyclerViewAdapter.addToCategories(media.getCategories());
        updateSelectedCategoriesTextView(categoryEditSearchRecyclerViewAdapter.getCategories());

        updateCategoryList();

        if (media.getAuthor() == null || media.getAuthor().equals("")) {
            authorLayout.setVisibility(GONE);
        } else {
            author.setText(media.getAuthor());
        }
    }

    private void updateCategoryList() {
        List<String> allCategories = new ArrayList<String>( media.getCategories());
        if (media.getAddedCategories() != null) {
            // TODO this added categories logic should be removed.
            //  It is just a short term hack. Categories should be fetch everytime they are updated.
            // if media.getCategories contains addedCategory, then do not re-add them
            for (String addedCategory : media.getAddedCategories()) {
                if (allCategories.contains(addedCategory)) {
                    media.setAddedCategories(null);
                    break;
                }
            }
            allCategories.addAll(media.getAddedCategories());
        }
        if (allCategories.isEmpty()) {
            // Stick in a filler element.
            allCategories.add(getString(R.string.detail_panel_cats_none));
        }

        rebuildCatList(allCategories);
    }

    @Override
    public void updateSelectedCategoriesTextView(List<String> selectedCategories) {
        if (selectedCategories == null || selectedCategories.size() == 0) {
            updateCategoriesButton.setClickable(false);
        }
        if (selectedCategories != null) {
            existingCategories.setText(StringUtils.join(selectedCategories,", "));
            updateCategoriesButton.setClickable(true);
        }
    }

    @Override
    public void noResultsFound() {
        noResultsFound.setVisibility(VISIBLE);
    }

    @Override
    public void someResultsFound() {
        noResultsFound.setVisibility(GONE);
    }

    /**
     * Populates media details fragment with depiction list
     * @param idAndCaptions
     */
    private void buildDepictionList(List<IdAndCaptions> idAndCaptions) {
        depictionContainer.removeAllViews();
        for (IdAndCaptions idAndCaption : idAndCaptions) {
                depictionContainer.addView(buildDepictLabel(
                    idAndCaption.getCaptions().values().iterator().next(),
                    idAndCaption.getId(),
                    depictionContainer
                ));
        }
    }

    @OnClick(R.id.mediaDetailLicense)
    public void onMediaDetailLicenceClicked(){
        String url = media.getLicenseUrl();
        if (!StringUtils.isBlank(url) && getActivity() != null) {
            Utils.handleWebUrl(getActivity(), Uri.parse(url));
        } else {
            viewUtil.showShortToast(getActivity(), getString(R.string.null_url));
        }
    }

    @OnClick(R.id.mediaDetailCoordinates)
    public void onMediaDetailCoordinatesClicked(){
        if (media.getCoordinates() != null && getActivity() != null) {
            Utils.handleGeoCoordinates(getActivity(), media.getCoordinates());
        }
    }

    @OnClick(R.id.copyWikicode)
    public void onCopyWikicodeClicked(){
        String data = "[[" + media.getFilename() + "|thumb|" + media.getFallbackDescription() + "]]";
        Utils.copy("wikiCode",data,getContext());
        Timber.d("Generated wikidata copy code: %s", data);

        Toast.makeText(getContext(), getString(R.string.wikicode_copied), Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.dummy_category_edit_container)
    public void onOutsideOfCategoryEditClicked() {
        if (dummyCategoryEditContainer.getVisibility() == VISIBLE) {
            dummyCategoryEditContainer.setVisibility(GONE);
        }
    }

    @OnClick(R.id.categoryEditButton)
    public void onCategoryEditButtonClicked(){
        displayHideCategorySearch();
    }

    public void displayHideCategorySearch() {
        if (dummyCategoryEditContainer.getVisibility() != VISIBLE) {
            dummyCategoryEditContainer.setVisibility(VISIBLE);
        } else {
            dummyCategoryEditContainer.setVisibility(GONE);
        }
    }

    @OnClick(R.id.update_categories_button)
    public void onUpdateCategoriesClicked() {
        updateCategories(categoryEditSearchRecyclerViewAdapter.getNewCategories());
        displayHideCategorySearch();
    }

    @OnClick(R.id.cancel_categories_button)
    public void onCancelCategoriesClicked() {
        displayHideCategorySearch();
    }

    public void updateCategories(List<String> selectedCategories) {
        compositeDisposable.add(categoryEditHelper.makeCategoryEdit(getContext(), media, selectedCategories, this)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(s -> {
                Timber.d("Categories are added.");
                onOutsideOfCategoryEditClicked();
                media.setAddedCategories(selectedCategories);
                updateCategoryList();
            }));
    }

    @SuppressLint("StringFormatInvalid")
    @OnClick(R.id.nominateDeletion)
    public void onDeleteButtonClicked(){
            if (AccountUtil.getUserName(getContext()) != null && AccountUtil.getUserName(getContext()).equals(media.getAuthor())) {
                final ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(getActivity(),
                    R.layout.simple_spinner_dropdown_list, reasonList);
                final Spinner spinner = new Spinner(getActivity());
                spinner.setLayoutParams(
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                spinner.setAdapter(languageAdapter);
                spinner.setGravity(17);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setView(spinner);
                builder.setTitle(R.string.nominate_delete)
                    .setPositiveButton(R.string.about_translate_proceed,
                        (dialog, which) -> onDeleteClicked(spinner));
                builder.setNegativeButton(R.string.about_translate_cancel,
                    (dialog, which) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialog.show();
                if (isDeleted) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
            //Reviewer correct me if i have misunderstood something over here
            //But how does this  if (delete.getVisibility() == View.VISIBLE) {
            //            enableDeleteButton(true);   makes sense ?
            else {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setMessage(
                    getString(R.string.dialog_box_text_nomination, media.getDisplayTitle()));
                final EditText input = new EditText(getActivity());
                alert.setView(input);
                input.requestFocus();
                alert.setPositiveButton(R.string.ok, (dialog1, whichButton) -> {
                    String reason = input.getText().toString();
                    onDeleteClickeddialogtext(reason);
                });
                alert.setNegativeButton(R.string.cancel, (dialog12, whichButton) -> {
                });
                AlertDialog d = alert.create();
                input.addTextChangedListener(new TextWatcher() {
                    private void handleText() {
                        final Button okButton = d.getButton(AlertDialog.BUTTON_POSITIVE);
                        if (input.getText().length() == 0) {
                            okButton.setEnabled(false);
                        } else {
                            okButton.setEnabled(true);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable arg0) {
                        handleText();
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }
                });
                d.show();
                d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        }

    @SuppressLint("CheckResult")
    private void onDeleteClicked(Spinner spinner) {
        String reason = spinner.getSelectedItem().toString();
        Single<Boolean> resultSingle = reasonBuilder.getReason(media, reason)
                .flatMap(reasonString -> deleteHelper.makeDeletion(getContext(), media, reason));
        compositeDisposable.add(resultSingle
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                    if (getActivity() != null) {
                        isDeleted = true;
                        enableDeleteButton(false);
                    }
                }));

    }

    @SuppressLint("CheckResult")
    private void onDeleteClickeddialogtext(String reason) {
        Single<Boolean> resultSingletext = reasonBuilder.getReason(media, reason)
                .flatMap(reasonString -> deleteHelper.makeDeletion(getContext(), media, reason));
        compositeDisposable.add(resultSingletext
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                    if (getActivity() != null) {
                        isDeleted = true;
                        enableDeleteButton(false);
                    }
                }));

    }

    @OnClick(R.id.seeMore)
    public void onSeeMoreClicked(){
        if (nominatedForDeletion.getVisibility() == VISIBLE && getActivity() != null) {
            Utils.handleWebUrl(getActivity(), Uri.parse(media.getPageTitle().getMobileUri()));
        }
    }

    private void enableDeleteButton(boolean visibility) {
        delete.setEnabled(visibility);
        if (visibility) {
            delete.setTextColor(getResources().getColor(R.color.primaryTextColor));
        } else {
            delete.setTextColor(getResources().getColor(R.color.deleteButtonLight));
        }
    }

    private void rebuildCatList(List<String> categories) {
        categoryContainer.removeAllViews();
        for (String category : categories) {
            categoryContainer.addView(buildCatLabel(sanitise(category), categoryContainer));
        }
    }

    //As per issue #1826(see https://github.com/commons-app/apps-android-commons/issues/1826), some categories come suffixed with strings prefixed with |. As per the discussion
    //that was meant for alphabetical sorting of the categories and can be safely removed.
    private String sanitise(String category) {
        int indexOfPipe = category.indexOf('|');
        if (indexOfPipe != -1) {
            //Removed everything after '|'
            return category.substring(0, indexOfPipe);
        }
        return category;
    }

    /**
     * Add view to depictions obtained also tapping on depictions should open the url
     */
    private View buildDepictLabel(String depictionName, String entityId, LinearLayout depictionContainer) {
        final View item = LayoutInflater.from(getContext()).inflate(R.layout.detail_category_item, depictionContainer,false);
        final TextView textView = item.findViewById(R.id.mediaDetailCategoryItemText);
        textView.setText(depictionName);
        item.setOnClickListener(view -> {
            Intent intent = new Intent(getContext(), WikidataItemDetailsActivity.class);
            intent.putExtra("wikidataItemName", depictionName);
            intent.putExtra("entityId", entityId);
            getContext().startActivity(intent);
        });
        return item;
    }

    private View buildCatLabel(final String catName, ViewGroup categoryContainer) {
        final View item = LayoutInflater.from(getContext()).inflate(R.layout.detail_category_item, categoryContainer, false);
        final TextView textView = item.findViewById(R.id.mediaDetailCategoryItemText);

        textView.setText(catName);
        if(!getString(R.string.detail_panel_cats_none).equals(catName)) {
            textView.setOnClickListener(view -> {
                // Open Category Details page
                String selectedCategoryTitle = CATEGORY_PREFIX + catName;
                Intent intent = new Intent(getContext(), CategoryDetailsActivity.class);
                intent.putExtra("categoryName", selectedCategoryTitle);
                getContext().startActivity(intent);
            });
        }
        return item;
    }

    /**
    * Returns captions for media details
     *
     * @param media object of class media
     * @return caption as string
     */
    private String prettyCaption(Media media) {
        for (String caption : media.getCaptions().values()) {
            if (caption.equals("")) {
                return getString(R.string.detail_caption_empty);
            } else {
                return caption;
            }
        }
        return getString(R.string.detail_caption_empty);
    }

    private String prettyDescription(Media media) {
        final String description = chooseDescription(media);
        return description.isEmpty() ? getString(R.string.detail_description_empty)
            : description;
    }

    private String chooseDescription(Media media) {
        final Map<String, String> descriptions = media.getDescriptions();
        final String multilingualDesc = descriptions.get(Locale.getDefault().getLanguage());
        if (multilingualDesc != null) {
            return multilingualDesc;
        }
        for (String description : descriptions.values()) {
            return description;
        }
        return media.getFallbackDescription();
    }

    private String prettyDiscussion(String discussion) {
        return discussion.isEmpty() ? getString(R.string.detail_discussion_empty) : discussion;
    }

    private String prettyLicense(Media media) {
        String licenseKey = media.getLicense();
        Timber.d("Media license is: %s", licenseKey);
        if (licenseKey == null || licenseKey.equals("")) {
            return getString(R.string.detail_license_empty);
        }
        return licenseKey;
    }

    private String prettyUploadedDate(Media media) {
        Date date = media.getDateUploaded();
        if (date == null || date.toString() == null || date.toString().isEmpty()) {
            return "Uploaded date not available";
        }
        return DateUtil.getDateStringWithSkeletonPattern(date, "dd MMM yyyy");
    }

    /**
     * Returns the coordinates nicely formatted.
     *
     * @return Coordinates as text.
     */
    private String prettyCoordinates(Media media) {
        if (media.getCoordinates() == null) {
            return getString(R.string.media_detail_coordinates_empty);
        }
        return media.getCoordinates().getPrettyCoordinateString();
    }

    @Override
    public boolean updateCategoryDisplay(List<String> categories) {
        if (categories == null) {
            return false;
        } else {
            rebuildCatList(categories);
            return true;
        }
    }
}
