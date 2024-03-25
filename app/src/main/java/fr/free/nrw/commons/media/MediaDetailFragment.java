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
import android.content.SharedPreferences;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CameraPosition;
import fr.free.nrw.commons.LocationPicker.LocationPicker;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.MediaDataExtractor;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.actions.ThanksClient;
import fr.free.nrw.commons.auth.AccountUtil;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.category.CategoryClient;
import fr.free.nrw.commons.category.CategoryDetailsActivity;
import fr.free.nrw.commons.category.CategoryEditHelper;
import fr.free.nrw.commons.contributions.ContributionsFragment;
import fr.free.nrw.commons.coordinates.CoordinateEditHelper;
import fr.free.nrw.commons.databinding.FragmentMediaDetailBinding;
import fr.free.nrw.commons.delete.DeleteHelper;
import fr.free.nrw.commons.delete.ReasonBuilder;
import fr.free.nrw.commons.description.DescriptionEditActivity;
import fr.free.nrw.commons.description.DescriptionEditHelper;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.depictions.WikidataItemDetailsActivity;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.profile.ProfileActivity;
import fr.free.nrw.commons.review.ReviewHelper;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.ui.widget.HtmlTextView;
import fr.free.nrw.commons.upload.categories.UploadCategoriesFragment;
import fr.free.nrw.commons.upload.depicts.DepictsFragment;
import fr.free.nrw.commons.upload.UploadMediaDetail;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import fr.free.nrw.commons.utils.ViewUtilWrapper;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang3.StringUtils;
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage;
import fr.free.nrw.commons.language.AppLanguageLookUpTable;
import fr.free.nrw.commons.utils.DateUtil;
import timber.log.Timber;

public class MediaDetailFragment extends CommonsDaggerSupportFragment implements
    CategoryEditHelper.Callback {

    private static final int REQUEST_CODE = 1001;
    private static final int REQUEST_CODE_EDIT_DESCRIPTION = 1002;
    private static final String IMAGE_BACKGROUND_COLOR = "image_background_color";
    static final int DEFAULT_IMAGE_BACKGROUND_COLOR = 0;
    
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
    ReviewHelper reviewHelper;
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
    ThanksClient thanksClient;
    @Inject
    @Named("default_preferences")
    JsonKvStore applicationKvStore;

    private int initialListTop = 0;
    private FragmentMediaDetailBinding binding;
    String descriptionHtmlCode;




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
        initialListTop = binding.mediaDetailScrollView.getScrollY();
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

        binding = FragmentMediaDetailBinding.inflate(inflater, container, false);
        final View view = binding.getRoot();


        Utils.setUnderlinedText(binding.seeMore, R.string.nominated_see_more, requireContext());

        if (isCategoryImage){
            binding.authorLinearLayout.setVisibility(VISIBLE);
        } else {
            binding.authorLinearLayout.setVisibility(GONE);
        }

        if (!sessionManager.isUserLoggedIn()) {
            binding.categoryEditButton.setVisibility(GONE);
        }

        if(applicationKvStore.getBoolean("login_skipped")){
            binding.nominateDeletion.setVisibility(GONE);
            binding.coordinateEdit.setVisibility(GONE);
        }

        handleBackEvent(view);

        //set onCLick listeners
        binding.mediaDetailLicense.setOnClickListener(v -> onMediaDetailLicenceClicked());
        binding.mediaDetailCoordinates.setOnClickListener(v -> onMediaDetailCoordinatesClicked());
        binding.sendThanks.setOnClickListener(v -> sendThanksToAuthor());
        binding.dummyCaptionDescriptionContainer.setOnClickListener(v -> showCaptionAndDescription());
        binding.mediaDetailImageView.setOnClickListener(v -> launchZoomActivity(binding.mediaDetailImageView));
        binding.categoryEditButton.setOnClickListener(v -> onCategoryEditButtonClicked());
        binding.depictionsEditButton.setOnClickListener(v -> onDepictionsEditButtonClicked());
        binding.seeMore.setOnClickListener(v -> onSeeMoreClicked());
        binding.mediaDetailAuthor.setOnClickListener(v -> onAuthorViewClicked());
        binding.nominateDeletion.setOnClickListener(v -> onDeleteButtonClicked());
        binding.descriptionEdit.setOnClickListener(v -> onDescriptionEditClicked());
        binding.coordinateEdit.setOnClickListener(v -> onUpdateCoordinatesClicked());
        binding.copyWikicode.setOnClickListener(v -> onCopyWikicodeClicked());


        /**
         * Gets the height of the frame layout as soon as the view is ready and updates aspect ratio
         * of the picture.
         */
        view.post(new Runnable() {
            @Override
            public void run() {
                frameLayoutHeight = binding.mediaDetailFrameLayout.getMeasuredHeight();
                updateAspectRatio(binding.mediaDetailScrollView.getWidth());
            }
        });

        return view;
    }

    public void launchZoomActivity(final View view) {
        final boolean hasPermission = PermissionUtils.hasPermission(getActivity(), PermissionUtils.PERMISSIONS_STORAGE);
        if (hasPermission) {
            launchZoomActivityAfterPermissionCheck(view);
        } else {
            PermissionUtils.checkPermissionsAndPerformAction(getActivity(),
                () -> {
                    launchZoomActivityAfterPermissionCheck(view);
                },
                R.string.storage_permission_title,
                R.string.read_storage_permission_rationale,
                PermissionUtils.PERMISSIONS_STORAGE
                );
        }
    }

    /**
     * launch zoom acitivity after permission check
     * @param view as ImageView
     */
    private void launchZoomActivityAfterPermissionCheck(final View view) {
        if (media.getImageUrl() != null) {
            final Context ctx = view.getContext();
            final Intent zoomableIntent = new Intent(ctx, ZoomableActivity.class);
            zoomableIntent.setData(Uri.parse(media.getImageUrl()));
            zoomableIntent.putExtra(
                ZoomableActivity.ZoomableActivityConstants.ORIGIN, "MediaDetails");
            
            int backgroundColor = getImageBackgroundColor();
            if (backgroundColor != DEFAULT_IMAGE_BACKGROUND_COLOR) {
                zoomableIntent.putExtra(
                    ZoomableActivity.ZoomableActivityConstants.PHOTO_BACKGROUND_COLOR,
                    backgroundColor
                );
            }
            
            ctx.startActivity(
                zoomableIntent
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
                    .getParentFragment())).binding.cardViewNearby
                    .setVisibility(View.GONE);
            }
        }
        // detail provider is null when fragment is shown in review activity
        if (detailProvider != null) {
            media = detailProvider.getMediaAtPosition(index);
        } else {
            media = getArguments().getParcelable("media");
        }

        if(media != null && applicationKvStore.getBoolean(String.format(NOMINATING_FOR_DELETION_MEDIA, media.getImageUrl()), false)) {
            enableProgressBar();
        }

        if (AccountUtil.getUserName(getContext()) != null && media != null
            && AccountUtil.getUserName(getContext()).equals(media.getAuthor())) {
            binding.sendThanks.setVisibility(GONE);
        } else {
            binding.sendThanks.setVisibility(VISIBLE);
        }

        binding.mediaDetailScrollView.getViewTreeObserver().addOnGlobalLayoutListener(
            new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (getContext() == null) {
                        return;
                    }
                    binding.mediaDetailScrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    oldWidthOfImageView = binding.mediaDetailScrollView.getWidth();
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
        binding.mediaDetailScrollView.getViewTreeObserver().addOnGlobalLayoutListener(
            new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    /**
                     * We update the height of the frame layout as the configuration changes.
                     */
                    binding.mediaDetailFrameLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            frameLayoutHeight = binding.mediaDetailFrameLayout.getMeasuredHeight();
                            updateAspectRatio(binding.mediaDetailScrollView.getWidth());
                        }
                    });
                    if (binding.mediaDetailScrollView.getWidth() != oldWidthOfImageView) {
                        if (newWidthOfImageView == 0) {
                            newWidthOfImageView = binding.mediaDetailScrollView.getWidth();
                            updateAspectRatio(newWidthOfImageView);
                        }
                        binding.mediaDetailScrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
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
            mediaDataExtractor.getCurrentWikiText(
                Objects.requireNonNull(media.getFilename()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateCategoryList, Timber::e),
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
        media.setCategories(this.media.getCategories());
        this.media = media;
        setTextFields(media);
        compositeDisposable.addAll(
            mediaDataExtractor.fetchDepictionIdsAndLabels(media)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onDepictionsLoaded, Timber::e)
        );
        // compositeDisposable.add(disposable);
    }

    private void onDiscussionLoaded(String discussion) {
        binding.mediaDetailDisc.setText(prettyDiscussion(discussion.trim()));
    }

    private void onDeletionPageExists(Boolean deletionPageExists) {
        if (deletionPageExists){
            if(applicationKvStore.getBoolean(String.format(NOMINATING_FOR_DELETION_MEDIA, media.getImageUrl()), false)) {
                applicationKvStore.remove(String.format(NOMINATING_FOR_DELETION_MEDIA, media.getImageUrl()));
                binding.progressBarDeletion.setVisibility(GONE);
            }
            binding.nominateDeletion.setVisibility(GONE);
            binding.nominatedDeletionBanner.setVisibility(VISIBLE);
        } else if (!isCategoryImage) {
            binding.nominateDeletion.setVisibility(VISIBLE);
            binding.nominatedDeletionBanner.setVisibility(GONE);
        }
    }

    private void onDepictionsLoaded(List<IdAndCaptions> idAndCaptions){
        binding.depictsLayout.setVisibility(idAndCaptions.isEmpty() ? GONE : VISIBLE);
        binding.depictionsEditButton.setVisibility(idAndCaptions.isEmpty() ? GONE : VISIBLE);
        buildDepictionList(idAndCaptions);
    }

    /**
     * By clicking on the edit depictions button, it will send user to depict fragment
     */

    public void onDepictionsEditButtonClicked() {
        binding.mediaDetailDepictionContainer.removeAllViews();
        binding.depictionsEditButton.setVisibility(GONE);
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
            ViewGroup.LayoutParams params = binding.mediaDetailImageView.getLayoutParams();
            ViewGroup.LayoutParams spacerParams = binding.mediaDetailImageViewSpacer.getLayoutParams();
            params.width = scrollWidth;
            if(finalHeight > frameLayoutHeight - minimumHeightOfMetadata) {

                // Adjust the height and width of image.
                int temp = frameLayoutHeight - minimumHeightOfMetadata;
                params.width = (scrollWidth*temp) / finalHeight;
                finalHeight = temp;

            }
            params.height = finalHeight;
            spacerParams.height = finalHeight;
            binding.mediaDetailImageView.setLayoutParams(params);
            binding.mediaDetailImageViewSpacer.setLayoutParams(spacerParams);
        }
    }

    private final ControllerListener aspectRatioListener = new BaseControllerListener<ImageInfo>() {
        @Override
        public void onIntermediateImageSet(String id, @Nullable ImageInfo imageInfo) {
            imageInfoCache = imageInfo;
            updateAspectRatio(binding.mediaDetailScrollView.getWidth());
        }
        @Override
        public void onFinalImageSet(String id, @Nullable ImageInfo imageInfo, @Nullable Animatable animatable) {
            imageInfoCache = imageInfo;
            updateAspectRatio(binding.mediaDetailScrollView.getWidth());
        }
    };

    /**
     * Uses two image sources.
     * - low resolution thumbnail is shown initially
     * - when the high resolution image is available, it replaces the low resolution image
     */
    private void setupImageView() {
        int imageBackgroundColor = getImageBackgroundColor();
        if (imageBackgroundColor != DEFAULT_IMAGE_BACKGROUND_COLOR) {
            binding.mediaDetailImageView.setBackgroundColor(imageBackgroundColor);
        }

        binding.mediaDetailImageView.getHierarchy().setPlaceholderImage(R.drawable.image_placeholder);
        binding.mediaDetailImageView.getHierarchy().setFailureImage(R.drawable.image_placeholder);

        DraweeController controller = Fresco.newDraweeControllerBuilder()
            .setLowResImageRequest(ImageRequest.fromUri(media != null ? media.getThumbUrl() : null))
            .setRetainImageOnFailure(true)
            .setImageRequest(ImageRequest.fromUri(media != null ? media.getImageUrl() : null))
            .setControllerListener(aspectRatioListener)
            .setOldController(binding.mediaDetailImageView.getController())
            .build();
        binding.mediaDetailImageView.setController(controller);
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
            binding.toDoLayout.setVisibility(VISIBLE);
            binding.toDoReason.setText(toDoMessage);
        } else {
            binding.toDoLayout.setVisibility(GONE);
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
        binding.mediaDetailTitle.setText(media.getDisplayTitle());
        binding.mediaDetailDesc.setHtmlText(prettyDescription(media));
        binding.mediaDetailLicense.setText(prettyLicense(media));
        binding.mediaDetailCoordinates.setText(prettyCoordinates(media));
        binding.mediaDetailuploadeddate.setText(prettyUploadedDate(media));
        if (prettyCaption(media).equals(getContext().getString(R.string.detail_caption_empty))) {
            binding.captionLayout.setVisibility(GONE);
        } else {
            binding.mediaDetailCaption.setText(prettyCaption(media));
        }

        categoryNames.clear();
        categoryNames.addAll(media.getCategories());

        if (media.getAuthor() == null || media.getAuthor().equals("")) {
            binding.authorLinearLayout.setVisibility(GONE);
        } else {
            binding.mediaDetailAuthor.setText(media.getAuthor());
        }
    }

    /**
     * Gets new categories from the WikiText and updates it on the UI
     *
     * @param s WikiText
     */
    private void updateCategoryList(final String s) {
        final List<String> allCategories = new ArrayList<String>();
        int i = s.indexOf("[[Category:");
        while(i != -1){
            final String category = s.substring(i+11, s.indexOf("]]", i));
            allCategories.add(category);
            i = s.indexOf("]]", i);
            i = s.indexOf("[[Category:", i);
        }
        media.setCategories(allCategories);
        if (allCategories.isEmpty()) {
            // Stick in a filler element.
            allCategories.add(getString(R.string.detail_panel_cats_none));
        }
        binding.categoryEditButton.setVisibility(VISIBLE);
        rebuildCatList(allCategories);
    }

    /**
     * Updates the categories
     */
    public void updateCategories() {
        List<String> allCategories = new ArrayList<String>(media.getAddedCategories());
        media.setCategories(allCategories);
        if (allCategories.isEmpty()) {
            // Stick in a filler element.
            allCategories.add(getString(R.string.detail_panel_cats_none));
        }

        rebuildCatList(allCategories);
    }

    /**
     * Populates media details fragment with depiction list
     * @param idAndCaptions
     */
    private void buildDepictionList(List<IdAndCaptions> idAndCaptions) {
        binding.mediaDetailDepictionContainer.removeAllViews();
        String locale = Locale.getDefault().getLanguage();
        for (IdAndCaptions idAndCaption : idAndCaptions) {
            binding.mediaDetailDepictionContainer.addView(buildDepictLabel(
                    getDepictionCaption(idAndCaption, locale),
                    idAndCaption.getId(),
                binding.mediaDetailDepictionContainer
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

    public void onMediaDetailLicenceClicked(){
        String url = media.getLicenseUrl();
        if (!StringUtils.isBlank(url) && getActivity() != null) {
            Utils.handleWebUrl(getActivity(), Uri.parse(url));
        } else {
            viewUtil.showShortToast(getActivity(), getString(R.string.null_url));
        }
    }

    public void onMediaDetailCoordinatesClicked(){
        if (media.getCoordinates() != null && getActivity() != null) {
            Utils.handleGeoCoordinates(getActivity(), media.getCoordinates());
        }
    }

    public void onCopyWikicodeClicked() {
        String data =
            "[[" + media.getFilename() + "|thumb|" + media.getFallbackDescription() + "]]";
        Utils.copy("wikiCode", data, getContext());
        Timber.d("Generated wikidata copy code: %s", data);

        Toast.makeText(getContext(), getString(R.string.wikicode_copied), Toast.LENGTH_SHORT)
            .show();
    }

    /**
     * Sends thanks to author if the author is not the user
     */
    public void sendThanksToAuthor() {
        String fileName = media.getFilename();
        if (TextUtils.isEmpty(fileName)) {
            Toast.makeText(getContext(), getString(R.string.error_sending_thanks),
                Toast.LENGTH_SHORT).show();
            return;
        }
        compositeDisposable.add(reviewHelper.getFirstRevisionOfFile(fileName)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(revision -> sendThanks(getContext(), revision)));
    }

    /**
     * Api call for sending thanks to the author when the author is not the user
     * and display toast depending on the result
     * @param context context
     * @param firstRevision the revision id of the image
     */
    @SuppressLint({"CheckResult", "StringFormatInvalid"})
    void sendThanks(Context context, MwQueryPage.Revision firstRevision) {
        ViewUtil.showShortToast(context,
            context.getString(R.string.send_thank_toast, media.getDisplayTitle()));

        if (firstRevision == null) {
            return;
        }

        Observable.defer((Callable<ObservableSource<Boolean>>) () -> thanksClient.thank(
                firstRevision.getRevisionId()))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe((result) -> {
                displayThanksToast(context, result);
            }, Timber::e);
    }

    /**
     * Method to display toast when api call to thank the author is completed
     * @param context context
     * @param result true if success, false otherwise
     */
    @SuppressLint("StringFormatInvalid")
    private void displayThanksToast(final Context context, final boolean result) {
        final String message;
        final String title;
        if (result) {
            title = context.getString(R.string.send_thank_success_title);
            message = context.getString(R.string.send_thank_success_message,
                media.getDisplayTitle());
        } else {
            title = context.getString(R.string.send_thank_failure_title);
            message = context.getString(R.string.send_thank_failure_message,
                media.getDisplayTitle());
        }

        ViewUtil.showShortToast(context, message);
    }

    public void onCategoryEditButtonClicked(){
        binding.progressBarEditCategory.setVisibility(VISIBLE);
        binding.categoryEditButton.setVisibility(GONE);
        getWikiText();
    }

    /**
     * Gets WikiText from the server and send it to catgory editor
     */
    private void getWikiText() {
        compositeDisposable.add(mediaDataExtractor.getCurrentWikiText(
            Objects.requireNonNull(media.getFilename()))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::gotoCategoryEditor, Timber::e));
    }

    /**
     * Opens the category editor
     *
     * @param s WikiText
     */
    private void gotoCategoryEditor(final String s) {
        binding.categoryEditButton.setVisibility(VISIBLE);
        binding.progressBarEditCategory.setVisibility(GONE);
        final Fragment categoriesFragment = new UploadCategoriesFragment();
        final Bundle bundle = new Bundle();
        bundle.putParcelable("Existing_Categories", media);
        bundle.putString("WikiText", s);
        categoriesFragment.setArguments(bundle);
        final FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.mediaDetailFrameLayout, categoriesFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

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


        startActivity(new LocationPicker.IntentBuilder()
            .defaultLocation(new CameraPosition(defaultLatitude,defaultLongitude,16.0))
            .activityKey("MediaActivity")
            .media(media)
            .build(getActivity()));
    }

    public void onDescriptionEditClicked() {
        binding.progressBarEdit.setVisibility(VISIBLE);
        binding.descriptionEdit.setVisibility(GONE);
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
        bundle.putString(Prefs.DESCRIPTION_LANGUAGE, applicationKvStore.getString(Prefs.DESCRIPTION_LANGUAGE, ""));
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
        final Pattern pattern = Pattern.compile("[dD]escription *=(.*?)\n *\\|", Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(s);
        String description = null;
        if (matcher.find()) {
            description = matcher.group();
        }
        if(description == null){
            return new LinkedHashMap<>();
        }

        final LinkedHashMap<String,String> descriptionList = new LinkedHashMap<>();

        int count = 0; // number of "{{"
        int startCode = 0;
        int endCode = 0;
        int startDescription = 0;
        int endDescription = 0;
        final HashSet<String> allLanguageCodes = new HashSet<>(Arrays.asList("en","es","de","ja","fr","ru","pt","it","zh-hans","zh-hant","ar","ko","id","pl","nl","fa","hi","th","vi","sv","uk","cs","simple","hu","ro","fi","el","he","nb","da","sr","hr","ms","bg","ca","tr","sk","sh","bn","tl","mr","ta","kk","lt","az","bs","sl","sq","arz","zh-yue","ka","te","et","lv","ml","hy","uz","kn","af","nn","mk","gl","sw","eu","ur","ky","gu","bh","sco","ast","is","mn","be","an","km","si","ceb","jv","eo","als","ig","su","be-x-old","la","my","cy","ne","bar","azb","mzn","as","am","so","pa","map-bms","scn","tg","ckb","ga","lb","war","zh-min-nan","nds","fy","vec","pnb","zh-classical","lmo","tt","io","ia","br","hif","mg","wuu","gan","ang","or","oc","yi","ps","tk","ba","sah","fo","nap","vls","sa","ce","qu","ku","min","bcl","ilo","ht","li","wa","vo","nds-nl","pam","new","mai","sn","pms","eml","yo","ha","gn","frr","gd","hsb","cv","lo","os","se","cdo","sd","ksh","bat-smg","bo","nah","xmf","ace","roa-tara","hak","bjn","gv","mt","pfl","szl","bpy","rue","co","diq","sc","rw","vep","lij","kw","fur","pcd","lad","tpi","ext","csb","rm","kab","gom","udm","mhr","glk","za","pdc","om","iu","nv","mi","nrm","tcy","frp","myv","kbp","dsb","zu","ln","mwl","fiu-vro","tum","tet","tn","pnt","stq","nov","ny","xh","crh","lfn","st","pap","ay","zea","bxr","kl","sm","ak","ve","pag","nso","kaa","lez","gag","kv","bm","to","lbe","krc","jam","ss","roa-rup","dv","ie","av","cbk-zam","chy","inh","ug","ch","arc","pih","mrj","kg","rmy","dty","na","ts","xal","wo","fj","tyv","olo","ltg","ff","jbo","haw","ki","chr","sg","atj","sat","ady","ty","lrc","ti","din","gor","lg","rn","bi","cu","kbd","pi","cr","koi","ik","mdf","bug","ee","shn","tw","dz","srn","ks","test","en-x-piglatin","ab"));
        for (int i = 0; i < description.length() - 1; i++) {
            if (description.startsWith("{{", i)) {
                if (count == 0) {
                    startCode = i;
                    endCode = description.indexOf("|", i);
                    startDescription = endCode + 1;
                    if (description.startsWith("1=", endCode + 1)) {
                        startDescription += 2;
                        i += 2;
                    }
                }
                i++;
                count++;
            } else if (description.startsWith("}}", i)) {
                count--;
                if (count == 0) {
                    endDescription = i;
                    final String languageCode = description.substring(startCode + 2, endCode);
                    final String languageDescription = description.substring(startDescription, endDescription);
                    if (allLanguageCodes.contains(languageCode)) {
                        descriptionList.put(languageCode, languageDescription);
                    }
                }
                i++;
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

        if (requestCode == REQUEST_CODE_EDIT_DESCRIPTION && resultCode == RESULT_OK) {
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
            binding.progressBarEdit.setVisibility(GONE);
            binding.descriptionEdit.setVisibility(VISIBLE);

        } else if (requestCode == REQUEST_CODE_EDIT_DESCRIPTION && resultCode == RESULT_CANCELED) {
            binding.progressBarEdit.setVisibility(GONE);
            binding.descriptionEdit.setVisibility(VISIBLE);
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

    @SuppressLint("StringFormatInvalid")
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

                AlertDialog dialog = DialogUtil.showAlertDialog(getActivity(),
                    getString(R.string.nominate_delete),
                    null,
                    getString(R.string.about_translate_proceed),
                    getString(R.string.about_translate_cancel),
                    () -> onDeleteClicked(spinner),
                    () -> {},
                    spinner,
                    true);
                if (isDeleted) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
            //Reviewer correct me if i have misunderstood something over here
            //But how does this  if (delete.getVisibility() == View.VISIBLE) {
            //            enableDeleteButton(true);   makes sense ?
            else {
                final EditText input = new EditText(getActivity());
                input.requestFocus();
                AlertDialog d = DialogUtil.showAlertDialog(getActivity(),
                    null,
                    getString(R.string.dialog_box_text_nomination, media.getDisplayTitle()),
                    getString(R.string.ok),
                    getString(R.string.cancel),
                    () -> {
                        String reason = input.getText().toString();
                        onDeleteClickeddialogtext(reason);
                    },
                    () -> {},
                    input,
                    true);
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

    public void onSeeMoreClicked(){
        if (binding.nominatedDeletionBanner.getVisibility() == VISIBLE && getActivity() != null) {
            Utils.handleWebUrl(getActivity(), Uri.parse(media.getPageTitle().getMobileUri()));
        }
    }

    public void onAuthorViewClicked() {
        if (media == null || media.getUser() == null) {
            return;
        }
        if (sessionManager.getUserName() == null) {
            String userProfileLink = BuildConfig.COMMONS_URL + "/wiki/User:" + media.getUser();
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(userProfileLink));
            startActivity(browserIntent);
            return;
        }
        ProfileActivity.startYourself(getActivity(), media.getUser(), !Objects
            .equals(sessionManager.getUserName(), media.getUser()));
    }

    /**
     * Enable Progress Bar and Update delete button text.
     */
    private void enableProgressBar() {
        binding.progressBarDeletion.setVisibility(VISIBLE);
        binding.nominateDeletion.setText("Nominating for Deletion");
        isDeleted = true;
    }

    private void rebuildCatList(List<String> categories) {
        binding.mediaDetailCategoryContainer.removeAllViews();
        for (String category : categories) {
            binding.mediaDetailCategoryContainer.addView(buildCatLabel(sanitise(category), binding.mediaDetailCategoryContainer));
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
        String description = chooseDescription(media);
        if (!description.isEmpty()) {
            // Remove img tag that sometimes appears as a blue square in the app,
            // see https://github.com/commons-app/apps-android-commons/issues/4345
            description = description.replaceAll("[<](/)?img[^>]*[>]", "");
        }
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

    void showCaptionAndDescription() {
        if (binding.dummyCaptionDescriptionContainer.getVisibility() == GONE) {
            binding.dummyCaptionDescriptionContainer.setVisibility(VISIBLE);
            setUpCaptionAndDescriptionLayout();
        } else {
            binding.dummyCaptionDescriptionContainer.setVisibility(GONE);
        }
    }

    /**
     * setUp Caption And Description Layout
     */
    private void setUpCaptionAndDescriptionLayout() {
        List<Caption> captions = getCaptions();

        if (descriptionHtmlCode == null) {
            binding.showCaptionsBinding.pbCircular.setVisibility(VISIBLE);
        }

        getDescription();
        CaptionListViewAdapter adapter = new CaptionListViewAdapter(captions);
        binding.showCaptionsBinding.captionListview.setAdapter(adapter);
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
        compositeDisposable.add(mediaDataExtractor.getHtmlOfPage(
                Objects.requireNonNull(media.getFilename()))
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

        binding.showCaptionsBinding.descriptionWebview
            .loadDataWithBaseURL(null, descriptionHtmlCode, "text/html", "utf-8", null);
        binding.showCaptionsBinding.pbCircular.setVisibility(GONE);
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
                    if (binding.dummyCaptionDescriptionContainer.getVisibility() == VISIBLE) {
                        binding.dummyCaptionDescriptionContainer.setVisibility(GONE);
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
    
    /**
     * Called when the image background color is changed.
     * You should pass a useable color, not a resource id.
     * @param color
     */
    public void onImageBackgroundChanged(int color) {
        int currentColor = getImageBackgroundColor();
        if (currentColor == color) {
            return;
        }

        binding.mediaDetailImageView.setBackgroundColor(color);
        getImageBackgroundColorPref().edit().putInt(IMAGE_BACKGROUND_COLOR, color).apply();
    }

    private SharedPreferences getImageBackgroundColorPref() {
        return getContext().getSharedPreferences(IMAGE_BACKGROUND_COLOR + media.getPageId(), Context.MODE_PRIVATE);
    }

    private int getImageBackgroundColor() {
        SharedPreferences imageBackgroundColorPref = this.getImageBackgroundColorPref();
        return imageBackgroundColorPref.getInt(IMAGE_BACKGROUND_COLOR, DEFAULT_IMAGE_BACKGROUND_COLOR);
    }
}
