package fr.free.nrw.commons.media;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static fr.free.nrw.commons.category.CategoryClientKt.CATEGORY_NEEDING_CATEGORIES;
import static fr.free.nrw.commons.category.CategoryClientKt.CATEGORY_UNCATEGORISED;
import static fr.free.nrw.commons.description.EditDescriptionConstants.LIST_OF_DESCRIPTION_AND_CAPTION;
import static fr.free.nrw.commons.description.EditDescriptionConstants.UPDATED_WIKITEXT;
import static fr.free.nrw.commons.description.EditDescriptionConstants.WIKITEXT;
import static fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.LAST_LOCATION;
import static fr.free.nrw.commons.utils.LangCodeUtils.getLocalizedResources;
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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
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
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import fr.free.nrw.commons.LocationPicker.LocationPicker;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.MediaDataExtractor;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.AccountUtil;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.category.CategoryClient;
import fr.free.nrw.commons.category.CategoryDetailsActivity;
import fr.free.nrw.commons.category.CategoryEditHelper;
import fr.free.nrw.commons.category.CategoryEditSearchRecyclerViewAdapter;
import fr.free.nrw.commons.category.CategoryEditSearchRecyclerViewAdapter.Callback;
import fr.free.nrw.commons.contributions.ContributionsFragment;
import fr.free.nrw.commons.coordinates.CoordinateEditHelper;
import fr.free.nrw.commons.delete.DeleteHelper;
import fr.free.nrw.commons.delete.ReasonBuilder;
import fr.free.nrw.commons.description.DescriptionEditActivity;
import fr.free.nrw.commons.description.DescriptionEditHelper;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.depictions.WikidataItemDetailsActivity;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.nearby.Label;
import fr.free.nrw.commons.profile.ProfileActivity;
import fr.free.nrw.commons.ui.widget.HtmlTextView;
import fr.free.nrw.commons.upload.depicts.DepictsFragment;
import fr.free.nrw.commons.upload.UploadMediaDetail;
import fr.free.nrw.commons.utils.ViewUtilWrapper;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.wikipedia.language.AppLanguageLookUpTable;
import org.wikipedia.util.DateUtil;
import timber.log.Timber;

public class MediaDetailFragment extends CommonsDaggerSupportFragment implements Callback,
    CategoryEditHelper.Callback {

    private static final int REQUEST_CODE = 1001 ;
    private static final int REQUEST_CODE_EDIT_DESCRIPTION = 1002 ;
    private boolean editable;
    private boolean isCategoryImage;
    private MediaDetailPagerFragment.MediaDetailProvider detailProvider;
    private int index;
    private boolean isDeleted = false;
    private boolean isWikipediaButtonDisplayed;
    private Callback callback;

    @Inject
    LocationServiceManager locationManager;


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
    SessionManager sessionManager;

    @Inject
    MediaDataExtractor mediaDataExtractor;
    @Inject
    ReasonBuilder reasonBuilder;
    @Inject
    DeleteHelper deleteHelper;
    @Inject
    CategoryEditHelper categoryEditHelper;
    @Inject
    CoordinateEditHelper coordinateEditHelper;
    @Inject
    DescriptionEditHelper descriptionEditHelper;
    @Inject
    ViewUtilWrapper viewUtil;
    @Inject
    CategoryClient categoryClient;
    @Inject
    @Named("default_preferences")
    JsonKvStore applicationKvStore;

    private int initialListTop = 0;
    @BindView(R.id.description_webview)
    WebView descriptionWebView;
    @BindView(R.id.mediaDetailFrameLayout)
    FrameLayout frameLayout;
    @BindView(R.id.mediaDetailImageView)
    SimpleDraweeView image;
    @BindView(R.id.mediaDetailImageViewSpacer)
    LinearLayout imageSpacer;
    @BindView(R.id.mediaDetailTitle)
    TextView title;
    @BindView(R.id.caption_layout)
    LinearLayout captionLayout;
    @BindView(R.id.depicts_layout)
    LinearLayout depictsLayout;
    @BindView(R.id.depictionsEditButton)
    Button depictEditButton;
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
    @BindView(R.id.coordinate_edit)
    Button coordinateEditButton;
    @BindView(R.id.dummy_category_edit_container)
    LinearLayout dummyCategoryEditContainer;
    @BindView(R.id.pb_categories)
    ProgressBar progressbarCategories;
    @BindView(R.id.existing_categories)
    TextView existingCategories;
    @BindView(R.id.no_results_found)
    TextView noResultsFound;
    @BindView(R.id.dummy_caption_description_container)
    LinearLayout showCaptionAndDescriptionContainer;
    @BindView(R.id.show_caption_description_textview)
    TextView showCaptionDescriptionTextView;
    @BindView(R.id.caption_listview)
    ListView captionsListView;
    @BindView(R.id.caption_label)
    TextView captionLabel;
    @BindView(R.id.description_label)
    TextView descriptionLabel;
    @BindView(R.id.pb_circular)
    ProgressBar progressBar;
    String descriptionHtmlCode;
    @BindView(R.id.progressBarDeletion)
    ProgressBar progressBarDeletion;
    @BindView(R.id.progressBarEdit)
    ProgressBar progressBarEditDescription;
    @BindView(R.id.description_edit)
    Button editDescription;

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
    private ArrayList<String> reasonListEnglishMappings;

    /**
     * Height stores the height of the frame layout as soon as it is initialised and updates itself on
     * configuration changes.
     * Used to adjust aspect ratio of image when length of the image is too large.
     */
    private int frameLayoutHeight;

    /**
     * Minimum height of the metadata, in pixels.
     * Images with a very narrow aspect ratio will be reduced so that the metadata information panel always has at least this height.
     */
    private int minimumHeightOfMetadata = 200;

    final static String NOMINATING_FOR_DELETION_MEDIA = "Nominating for deletion %s";

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

        // Add corresponding mappings in english locale so that we can upload it in deletion request
        reasonListEnglishMappings = new ArrayList<>();
        reasonListEnglishMappings.add(getLocalizedResources(getContext(), Locale.ENGLISH).getString(R.string.deletion_reason_uploaded_by_mistake));
        reasonListEnglishMappings.add(getLocalizedResources(getContext(), Locale.ENGLISH).getString(R.string.deletion_reason_publicly_visible));
        reasonListEnglishMappings.add(getLocalizedResources(getContext(), Locale.ENGLISH).getString(R.string.deletion_reason_not_interesting));
        reasonListEnglishMappings.add(getLocalizedResources(getContext(), Locale.ENGLISH).getString(R.string.deletion_reason_no_longer_want_public));
        reasonListEnglishMappings.add(getLocalizedResources(getContext(), Locale.ENGLISH).getString(R.string.deletion_reason_bad_for_my_privacy));

        final View view = inflater.inflate(R.layout.fragment_media_detail, container, false);

        ButterKnife.bind(this,view);
        Utils.setUnderlinedText(seeMore, R.string.nominated_see_more, requireContext());

        if (isCategoryImage){
            authorLayout.setVisibility(VISIBLE);
        } else {
            authorLayout.setVisibility(GONE);
        }

        if (!sessionManager.isUserLoggedIn()) {
            categoryEditButton.setVisibility(GONE);
        }

        if(applicationKvStore.getBoolean("login_skipped")){
            delete.setVisibility(GONE);
            coordinateEditButton.setVisibility(GONE);
        }

        handleBackEvent(view);

        /**
         * Gets the height of the frame layout as soon as the view is ready and updates aspect ratio
         * of the picture.
         */
        view.post(new Runnable() {
            @Override
            public void run() {
                frameLayoutHeight = frameLayout.getMeasuredHeight();
                updateAspectRatio(scrollView.getWidth());
            }
        });

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
        // detail provider is null when fragment is shown in review activity
        if (detailProvider != null) {
            media = detailProvider.getMediaAtPosition(index);
        } else {
            media = getArguments().getParcelable("media");
        }

        if(media != null && applicationKvStore.getBoolean(String.format(NOMINATING_FOR_DELETION_MEDIA, media.getImageUrl()), false)) {
            enableProgressBar();
        }

        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
            new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (getContext() == null) {
                        return;
                    }
                    scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    oldWidthOfImageView = scrollView.getWidth();
                    if(media != null) {
                        displayMediaDetails();
                    }
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
                    /**
                     * We update the height of the frame layout as the configuration changes.
                     */
                    frameLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            frameLayoutHeight = frameLayout.getMeasuredHeight();
                            updateAspectRatio(scrollView.getWidth());
                        }
                    });
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
        // Ensuring correct aspect ratio for landscape mode
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
            mediaDataExtractor.refresh(media)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onMediaRefreshed, Timber::e),
            mediaDataExtractor.checkDeletionRequestExists(media)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onDeletionPageExists, Timber::e),
            mediaDataExtractor.fetchDiscussion(media)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onDiscussionLoaded, Timber::e)
        );
    }

    private void onMediaRefreshed(Media media) {
        this.media = media;
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
            if(applicationKvStore.getBoolean(String.format(NOMINATING_FOR_DELETION_MEDIA, media.getImageUrl()), false)) {
                applicationKvStore.remove(String.format(NOMINATING_FOR_DELETION_MEDIA, media.getImageUrl()));
                progressBarDeletion.setVisibility(GONE);
            }
            delete.setVisibility(GONE);
            nominatedForDeletion.setVisibility(VISIBLE);
        } else if (!isCategoryImage) {
            delete.setVisibility(VISIBLE);
            nominatedForDeletion.setVisibility(GONE);
        }
    }

    private void onDepictionsLoaded(List<IdAndCaptions> idAndCaptions){
        depictsLayout.setVisibility(idAndCaptions.isEmpty() ? GONE : VISIBLE);
        depictEditButton.setVisibility(idAndCaptions.isEmpty() ? GONE : VISIBLE);
        buildDepictionList(idAndCaptions);
    }

    /**
     * By clicking on the edit depictions button, it will send user to depict fragment
     */
    @OnClick(R.id.depictionsEditButton)
    public void onDepictionsEditButtonClicked() {
        depictionContainer.removeAllViews();
        depictEditButton.setVisibility(GONE);
        final Fragment depictsFragment = new DepictsFragment();
        final Bundle bundle = new Bundle();
        bundle.putParcelable("Existing_Depicts", media);
        depictsFragment.setArguments(bundle);
        final FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.mediaDetailFrameLayout, depictsFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
    /**
     * The imageSpacer is Basically a transparent overlay for the SimpleDraweeView
     * which holds the image to be displayed( moreover this image is out of
     * the scroll view )
     *
     *
     * If the image is sufficiently large i.e. the image height extends the view height, we reduce
     * the height and change the width to maintain the aspect ratio, otherwise image takes up the
     * total possible width and height is adjusted accordingly.
     *
     * @param scrollWidth the current width of the scrollView
     */
    private void updateAspectRatio(int scrollWidth) {
        if (imageInfoCache != null) {
            int finalHeight = (scrollWidth*imageInfoCache.getHeight()) / imageInfoCache.getWidth();
            ViewGroup.LayoutParams params = image.getLayoutParams();
            ViewGroup.LayoutParams spacerParams = imageSpacer.getLayoutParams();
            params.width = scrollWidth;
            if(finalHeight > frameLayoutHeight - minimumHeightOfMetadata) {

                // Adjust the height and width of image.
                int temp = frameLayoutHeight - minimumHeightOfMetadata;
                params.width = (scrollWidth*temp) / finalHeight;
                finalHeight = temp;

            }
            params.height = finalHeight;
            spacerParams.height = finalHeight;
            image.setLayoutParams(params);
            imageSpacer.setLayoutParams(spacerParams);
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

        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setLowResImageRequest(ImageRequest.fromUri(media != null ? media.getThumbUrl() : null))
                .setImageRequest(ImageRequest.fromUri(media != null ? media.getImageUrl() : null))
                .setControllerListener(aspectRatioListener)
                .setOldController(image.getController())
                .build();
        image.setController(controller);
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

        categoryRecyclerView.setVisibility(GONE);
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
            updateCategoriesButton.setAlpha(.5f);
        } else {
            existingCategories.setText(StringUtils.join(selectedCategories,", "));
            if (selectedCategories.equals(media.getCategories())) {
                updateCategoriesButton.setClickable(false);
                updateCategoriesButton.setAlpha(.5f);
            } else {
                updateCategoriesButton.setClickable(true);
                updateCategoriesButton.setAlpha(1f);
            }
        }
    }

    @Override
    public void noResultsFound() {
        categoryRecyclerView.setVisibility(GONE);
        noResultsFound.setVisibility(VISIBLE);
    }

    @Override
    public void someResultsFound() {
        categoryRecyclerView.setVisibility(VISIBLE);
        noResultsFound.setVisibility(GONE);
    }

    /**
     * Populates media details fragment with depiction list
     * @param idAndCaptions
     */
    private void buildDepictionList(List<IdAndCaptions> idAndCaptions) {
        depictionContainer.removeAllViews();
        String locale = Locale.getDefault().getLanguage();
        for (IdAndCaptions idAndCaption : idAndCaptions) {
                depictionContainer.addView(buildDepictLabel(
                    getDepictionCaption(idAndCaption, locale),
                    idAndCaption.getId(),
                    depictionContainer
                ));
        }
    }

    private String getDepictionCaption(IdAndCaptions idAndCaption, String locale) {
        //Check if the Depiction Caption is available in user's locale if not then check for english, else show any available.
        if(idAndCaption.getCaptions().get(locale) != null) {
            return idAndCaption.getCaptions().get(locale);
        }
        if(idAndCaption.getCaptions().get("en") != null) {
            return idAndCaption.getCaptions().get("en");
        }
        return idAndCaption.getCaptions().values().iterator().next();
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

    /**
     * Hides the categoryEditContainer.
     * returns true after closing the categoryEditContainer if open, implying that event was handled.
     * else returns false
     * @return
     */
    public boolean hideCategoryEditContainerIfOpen(){
        if (dummyCategoryEditContainer.getVisibility() == VISIBLE) {
            // editCategory is open, close it and return true as the event was handled.
            dummyCategoryEditContainer.setVisibility(GONE);
            return true;
        }
        // Event was not handled.
        return false;
    }

    public void displayHideCategorySearch() {
        showCaptionAndDescriptionContainer.setVisibility(GONE);
        if (dummyCategoryEditContainer.getVisibility() != VISIBLE) {
            dummyCategoryEditContainer.setVisibility(VISIBLE);
        } else {
            dummyCategoryEditContainer.setVisibility(GONE);
        }
    }

    @OnClick(R.id.coordinate_edit)
    public void onUpdateCoordinatesClicked(){
        goToLocationPickerActivity();
    }

    /**
     * Start location picker activity with a request code and get the coordinates from the activity.
     */
    private void goToLocationPickerActivity() {
        /*
        If location is not provided in media this coordinates will act as a placeholder in
        location picker activity
         */
        double defaultLatitude = 37.773972;
        double defaultLongitude = -122.431297;
        if (media.getCoordinates() != null) {
            defaultLatitude = media.getCoordinates().getLatitude();
            defaultLongitude = media.getCoordinates().getLongitude();
        } else {
            if(locationManager.getLastLocation()!=null) {
                defaultLatitude = locationManager.getLastLocation().getLatitude();
                defaultLongitude = locationManager.getLastLocation().getLongitude();
            } else {
                String[] lastLocation = applicationKvStore.getString(LAST_LOCATION,(defaultLatitude + "," + defaultLongitude)).split(",");
                defaultLatitude = Double.parseDouble(lastLocation[0]);
                defaultLongitude = Double.parseDouble(lastLocation[1]);
            }
        }

        startActivityForResult(new LocationPicker.IntentBuilder()
            .defaultLocation(new CameraPosition.Builder()
                .target(new LatLng(defaultLatitude, defaultLongitude))
                .zoom(16).build())
            .activityKey("MediaActivity")
            .build(getActivity()), REQUEST_CODE);
    }

    @OnClick(R.id.description_edit)
    public void onDescriptionEditClicked() {
        progressBarEditDescription.setVisibility(VISIBLE);
        editDescription.setVisibility(GONE);
        getDescriptionList();
    }

    /**
     * Gets descriptions from wikitext
     */
    private void getDescriptionList() {
        compositeDisposable.add(mediaDataExtractor.getCurrentWikiText(
            Objects.requireNonNull(media.getFilename()))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::extractCaptionDescription, Timber::e));
    }

    /**
     * Gets captions and descriptions and merge them according to language code and arranges it in a
     * single list.
     * Send the list to DescriptionEditActivity
     * @param s wikitext
     */
    private void extractCaptionDescription(final String s) {
        final LinkedHashMap<String,String> descriptions = getDescriptions(s);
        final LinkedHashMap<String,String> captions = getCaptionsList();

        final ArrayList<UploadMediaDetail> descriptionAndCaptions = new ArrayList<>();

        if(captions.size() >= descriptions.size()) {
            for (final Map.Entry mapElement : captions.entrySet()) {

                final String language = (String) mapElement.getKey();
                if (descriptions.containsKey(language)) {
                    descriptionAndCaptions.add(
                        new UploadMediaDetail(language,
                            Objects.requireNonNull(descriptions.get(language)),
                            (String) mapElement.getValue())
                    );
                } else {
                    descriptionAndCaptions.add(
                        new UploadMediaDetail(language, "",
                            (String) mapElement.getValue())
                    );
                }
            }
            for (final Map.Entry mapElement : descriptions.entrySet()) {

                final String language = (String) mapElement.getKey();
                if (!captions.containsKey(language)) {
                    descriptionAndCaptions.add(
                        new UploadMediaDetail(language,
                            Objects.requireNonNull(descriptions.get(language)),
                            "")
                    );
                }
            }
        } else {
            for (final Map.Entry mapElement : descriptions.entrySet()) {

                final String language = (String) mapElement.getKey();
                if (captions.containsKey(language)) {
                    descriptionAndCaptions.add(
                        new UploadMediaDetail(language, (String) mapElement.getValue(),
                            Objects.requireNonNull(captions.get(language)))
                    );
                } else {
                    descriptionAndCaptions.add(
                        new UploadMediaDetail(language, (String) mapElement.getValue(),
                            "")
                    );
                }
            }
            for (final Map.Entry mapElement : captions.entrySet()) {

                final String language = (String) mapElement.getKey();
                if (!descriptions.containsKey(language)) {
                    descriptionAndCaptions.add(
                        new UploadMediaDetail(language,
                            "",
                            Objects.requireNonNull(descriptions.get(language)))
                    );
                }
            }
        }
        final Intent intent = new Intent(requireContext(), DescriptionEditActivity.class);
        final Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(LIST_OF_DESCRIPTION_AND_CAPTION, descriptionAndCaptions);
        bundle.putString(WIKITEXT, s);
        intent.putExtras(bundle);
        startActivityForResult(intent, REQUEST_CODE_EDIT_DESCRIPTION);
    }

    /**
     * Filters descriptions from current wikiText and arranges it in LinkedHashmap according to the
     * language code
     * @param s wikitext
     * @return LinkedHashMap<LanguageCode,Description>
     */
    private LinkedHashMap<String,String> getDescriptions(String s) {
        int descriptionIndex = s.indexOf("description=");
        if(descriptionIndex == -1){
            descriptionIndex = s.indexOf("Description=");
        }

        if( descriptionIndex == -1 ){
            return new LinkedHashMap<>();
        }
        final String descriptionToEnd = s.substring(descriptionIndex+12);
        final int descriptionEndIndex = descriptionToEnd.indexOf("\n");
        final String description = s.substring(descriptionIndex+12, descriptionIndex+12+descriptionEndIndex);

        final String[] arr = description.trim().split(",");
        final LinkedHashMap<String,String> descriptionList = new LinkedHashMap<>();

        if (!description.equals("")) {
            for (final String string :
                arr) {
                final int startCode = string.indexOf("{{");
                final int endCode = string.indexOf("|");
                final String languageCode = string.substring(startCode + 2, endCode).trim();
                final int startDescription = string.indexOf("=");
                final int endDescription = string.indexOf("}}");
                final String languageDescription = string
                    .substring(startDescription + 1, endDescription);
                descriptionList.put(languageCode, languageDescription);
            }
        }
        return descriptionList;
    }

    /**
     * Gets list of caption and arranges it in a LinkedHashmap according to the language code
     * @return LinkedHashMap<LanguageCode,Caption>
     */
    private LinkedHashMap<String,String> getCaptionsList() {
        final LinkedHashMap<String, String> captionList = new LinkedHashMap<>();
        final Map<String, String> captions = media.getCaptions();
        for (final Map.Entry<String, String> map : captions.entrySet()) {
            final String language = map.getKey();
            final String languageCaption = map.getValue();
            captionList.put(language, languageCaption);
        }
        return captionList;
    }

    /**
     * Get the result from another activity and act accordingly.
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(final int requestCode, final int resultCode,
        @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {

            assert data != null;
            final CameraPosition cameraPosition = LocationPicker.getCameraPosition(data);

            if (cameraPosition != null) {

                final String latitude = String.valueOf(cameraPosition.target.getLatitude());
                final String longitude = String.valueOf(cameraPosition.target.getLongitude());
                final String accuracy = String.valueOf(cameraPosition.target.getAltitude());
                String currentLatitude = null;
                String currentLongitude = null;

                if (media.getCoordinates() != null) {
                    currentLatitude = String.valueOf(media.getCoordinates().getLatitude());
                    currentLongitude = String.valueOf(media.getCoordinates().getLongitude());
                }

                if (!latitude.equals(currentLatitude) || !longitude.equals(currentLongitude)) {
                    updateCoordinates(latitude, longitude, accuracy);
                } else if (media.getCoordinates() == null) {
                    updateCoordinates(latitude, longitude, accuracy);
                }
            }

        } else if (requestCode == REQUEST_CODE_EDIT_DESCRIPTION && resultCode == RESULT_OK) {
            final String updatedWikiText = data.getStringExtra(UPDATED_WIKITEXT);
            compositeDisposable.add(descriptionEditHelper.addDescription(getContext(), media,
                updatedWikiText)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                    Timber.d("Descriptions are added.");
                }));

            final ArrayList<UploadMediaDetail> uploadMediaDetails
                = data.getParcelableArrayListExtra(LIST_OF_DESCRIPTION_AND_CAPTION);

            LinkedHashMap<String, String> updatedCaptions = new LinkedHashMap<>();
            for (UploadMediaDetail mediaDetail:
            uploadMediaDetails) {
                compositeDisposable.add(descriptionEditHelper.addCaption(getContext(), media,
                    mediaDetail.getLanguageCode(), mediaDetail.getCaptionText())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(s -> {
                        updateCaptions(mediaDetail, updatedCaptions);
                        Timber.d("Caption is added.");
                    }));
            }
            progressBarEditDescription.setVisibility(GONE);
            editDescription.setVisibility(VISIBLE);

        } else if (requestCode == REQUEST_CODE && resultCode == RESULT_CANCELED) {
            viewUtil.showShortToast(getContext(),
                Objects.requireNonNull(getContext())
                    .getString(R.string.coordinates_picking_unsuccessful));

        } else if (requestCode == REQUEST_CODE_EDIT_DESCRIPTION && resultCode == RESULT_CANCELED) {
            progressBarEditDescription.setVisibility(GONE);
            editDescription.setVisibility(VISIBLE);

            viewUtil.showShortToast(getContext(),
                Objects.requireNonNull(getContext())
                    .getString(R.string.descriptions_picking_unsuccessful));
        }
    }

    /**
     * Adds caption to the map and updates captions
     * @param mediaDetail UploadMediaDetail
     * @param updatedCaptions updated captionds
     */
    private void updateCaptions(UploadMediaDetail mediaDetail,
        LinkedHashMap<String, String> updatedCaptions) {
        updatedCaptions.put(mediaDetail.getLanguageCode(), mediaDetail.getCaptionText());
        media.setCaptions(updatedCaptions);
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

    /**
     * Fetched coordinates are replaced with existing coordinates by a POST API call.
     * @param Latitude to be added
     * @param Longitude to be added
     * @param Accuracy to be added
     */
    public void updateCoordinates(final String Latitude, final String Longitude,
        final String Accuracy) {
        compositeDisposable.add(coordinateEditHelper.makeCoordinatesEdit(getContext(), media,
            Latitude, Longitude, Accuracy)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(s -> {
                Timber.d("Coordinates are added.");
                coordinates.setText(prettyCoordinates(media));
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
                        if (input.getText().length() == 0 || isDeleted) {
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
        applicationKvStore.putBoolean(String.format(NOMINATING_FOR_DELETION_MEDIA, media.getImageUrl()), true);
        enableProgressBar();
        String reason = reasonListEnglishMappings.get(spinner.getSelectedItemPosition());
        String finalReason = reason;
        Single<Boolean> resultSingle = reasonBuilder.getReason(media, reason)
                .flatMap(reasonString -> deleteHelper.makeDeletion(getContext(), media, finalReason));
        resultSingle
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(s -> {
                if(applicationKvStore.getBoolean(String.format(NOMINATING_FOR_DELETION_MEDIA, media.getImageUrl()), false)) {
                    applicationKvStore.remove(String.format(NOMINATING_FOR_DELETION_MEDIA, media.getImageUrl()));
                    callback.nominatingForDeletion(index);
                }
            });
    }

    @SuppressLint("CheckResult")
    private void onDeleteClickeddialogtext(String reason) {
        applicationKvStore.putBoolean(String.format(NOMINATING_FOR_DELETION_MEDIA, media.getImageUrl()), true);
        enableProgressBar();
        Single<Boolean> resultSingletext = reasonBuilder.getReason(media, reason)
                .flatMap(reasonString -> deleteHelper.makeDeletion(getContext(), media, reason));
        resultSingletext
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(s -> {
                if(applicationKvStore.getBoolean(String.format(NOMINATING_FOR_DELETION_MEDIA, media.getImageUrl()), false)) {
                    applicationKvStore.remove(String.format(NOMINATING_FOR_DELETION_MEDIA, media.getImageUrl()));
                    callback.nominatingForDeletion(index);
                }
            });
    }

    @OnClick(R.id.seeMore)
    public void onSeeMoreClicked(){
        if (nominatedForDeletion.getVisibility() == VISIBLE && getActivity() != null) {
            Utils.handleWebUrl(getActivity(), Uri.parse(media.getPageTitle().getMobileUri()));
        }
    }

    @OnClick(R.id.mediaDetailAuthor)
    public void onAuthorViewClicked() {
        if (media == null || media.getUser() == null) {
            return;
        }
        ProfileActivity.startYourself(getActivity(), media.getUser(), !Objects
            .equals(sessionManager.getUserName(), media.getUser()));
    }

    /**
     * Enable Progress Bar and Update delete button text.
     */
    private void enableProgressBar() {
        progressBarDeletion.setVisibility(VISIBLE);
        delete.setText("Nominating for Deletion");
        isDeleted = true;
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
            intent.putExtra("fragment", "MediaDetailFragment");
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
                Intent intent = new Intent(getContext(), CategoryDetailsActivity.class);
                intent.putExtra("categoryName", catName);
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

    @OnClick(R.id.show_caption_description_textview)
    void showCaptionAndDescription() {
        dummyCategoryEditContainer.setVisibility(GONE);
        if (showCaptionAndDescriptionContainer.getVisibility() == GONE) {
            showCaptionAndDescriptionContainer.setVisibility(VISIBLE);
            setUpCaptionAndDescriptionLayout();
        } else {
            showCaptionAndDescriptionContainer.setVisibility(GONE);
        }
    }

    /**
     * setUp Caption And Description Layout
     */
    private void setUpCaptionAndDescriptionLayout() {
        List<Caption> captions = getCaptions();

        if (descriptionHtmlCode == null) {
            progressBar.setVisibility(VISIBLE);
        }

        getDescription();
        CaptionListViewAdapter adapter = new CaptionListViewAdapter(captions);
        captionsListView.setAdapter(adapter);
    }

    /**
     * Generate the caption with language
     */
    private List<Caption> getCaptions() {
        List<Caption> captionList = new ArrayList<>();
        Map<String, String> captions = media.getCaptions();
        AppLanguageLookUpTable appLanguageLookUpTable = new AppLanguageLookUpTable(getContext());
        for (Map.Entry<String, String> map : captions.entrySet()) {
            String language = appLanguageLookUpTable.getLocalizedName(map.getKey());
            String languageCaption = map.getValue();
            captionList.add(new Caption(language, languageCaption));
        }

        if (captionList.size() == 0) {
            captionList.add(new Caption("", "No Caption"));
        }
        return captionList;
    }

    private void getDescription() {
        compositeDisposable.add(mediaDataExtractor.getHtmlOfPage(media.getFilename())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::extractDescription, Timber::e));
    }

    /**
     * extract the description from html of imagepage
     */
    private void extractDescription(String s) {
        String descriptionClassName = "<td class=\"description\">";
        int start = s.indexOf(descriptionClassName) + descriptionClassName.length();
        int end = s.indexOf("</td>", start);
        descriptionHtmlCode = "";
        for (int i = start; i < end; i++) {
            descriptionHtmlCode = descriptionHtmlCode + s.toCharArray()[i];
        }

        descriptionWebView
            .loadDataWithBaseURL(null, descriptionHtmlCode, "text/html", "utf-8", null);
        progressBar.setVisibility(GONE);
    }

    /**
     * Handle back event when fragment when showCaptionAndDescriptionContainer is visible
     */
    private void handleBackEvent(View view) {
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keycode, KeyEvent keyEvent) {
                if (keycode == KeyEvent.KEYCODE_BACK) {
                    if (showCaptionAndDescriptionContainer.getVisibility() == VISIBLE) {
                        showCaptionAndDescriptionContainer.setVisibility(GONE);
                        return true;
                    }
                }
                return false;
            }
        });

    }


    public interface Callback {
        void nominatingForDeletion(int index);
    }
}
