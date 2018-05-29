package fr.free.nrw.commons.upload;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.caching.CacheController;
import fr.free.nrw.commons.category.CategorizationFragment;
import fr.free.nrw.commons.category.OnCategoriesSaveHandler;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.modifications.CategoryModifier;
import fr.free.nrw.commons.modifications.ModificationsContentProvider;
import fr.free.nrw.commons.modifications.ModifierSequence;
import fr.free.nrw.commons.modifications.ModifierSequenceDao;
import fr.free.nrw.commons.modifications.TemplateRemoveModifier;
import fr.free.nrw.commons.mwapi.CategoryApi;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.utils.ViewUtil;
import timber.log.Timber;

import static fr.free.nrw.commons.upload.ExistingFileAsync.Result.DUPLICATE_PROCEED;
import static fr.free.nrw.commons.upload.ExistingFileAsync.Result.NO_DUPLICATE;
import static fr.free.nrw.commons.upload.FileUtils.getSHA1;

/**
 * Activity for the title/desc screen after image is selected. Also starts processing image
 * GPS coordinates or user location (if enabled in Settings) for category suggestions.
 */
public class ShareActivity
        extends AuthenticatedActivity
        implements SingleUploadFragment.OnUploadActionInitiated,
        OnCategoriesSaveHandler {

    private static final int REQUEST_PERM_ON_CREATE_STORAGE = 1;
    private static final int REQUEST_PERM_ON_CREATE_LOCATION = 2;
    private static final int REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION = 3;
    private static final int REQUEST_PERM_ON_SUBMIT_STORAGE = 4;
    //Had to make them class variables, to extract out the click listeners, also I see no harm in this
    final Rect startBounds = new Rect();
    final Rect finalBounds = new Rect();
    final Point globalOffset = new Point();
    @Inject
    MediaWikiApi mwApi;
    @Inject
    CacheController cacheController;
    @Inject
    SessionManager sessionManager;
    @Inject
    UploadController uploadController;
    @Inject
    ModifierSequenceDao modifierSequenceDao;
    @Inject
    CategoryApi apiCall;
    @Inject
    @Named("default_preferences")
    SharedPreferences prefs;
    @Inject
    GpsCategoryModel gpsCategoryModel;

    @BindView(R.id.container)
    FrameLayout flContainer;
    @BindView(R.id.backgroundImage)
    SimpleDraweeView backgroundImageView;
    @BindView(R.id.media_map)
    FloatingActionButton mapButton;
    @BindView(R.id.media_upload_zoom_in)
    FloatingActionButton zoomInButton;
    @BindView(R.id.media_upload_zoom_out)
    FloatingActionButton zoomOutButton;
    @BindView(R.id.main_fab)
    FloatingActionButton mainFab;
    @BindView(R.id.expanded_image)
    PhotoView expandedImageView;

    private String source;
    private String mimeType;
    private CategorizationFragment categorizationFragment;
    private Uri mediaUri;
    private Contribution contribution;
    private boolean cacheFound;
    private GPSExtractor imageObj;
    private GPSExtractor tempImageObj;
    private String decimalCoords;
    private FileProcessor fileObj;
    private boolean useNewPermissions = false;
    private boolean storagePermitted = false;
    private boolean locationPermitted = false;
    private String title;
    private String description;
    private Snackbar snackbar;
    private boolean duplicateCheckPassed = false;
    private boolean isNearbyUpload = false;
    private Animator CurrentAnimator;
    private long ShortAnimationDuration;
    private boolean isFABOpen = false;
    private float startScaleFinal;

    /**
     * Called when user taps the submit button.
     * Requests Storage permission, if needed.
     */
    @Override
    public void uploadActionInitiated(String title, String description) {

        this.title = title;
        this.description = description;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (needsToRequestStoragePermission()) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERM_ON_SUBMIT_STORAGE);
            } else {
                uploadBegins();
            }
        } else {
            uploadBegins();
        }
    }

    /**
     * Checks whether storage permissions need to be requested.
     * Permissions are needed if the file is not owned by this application, (e.g. shared from the Gallery)
     * @return true if file is not owned by this application and permission hasn't been granted beforehand
     */
    @RequiresApi(16)
    private boolean needsToRequestStoragePermission() {
        return !FileUtils.isSelfOwned(getApplicationContext(), mediaUri)
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED);
    }

    /**
     * Called after permission checks are done.
     * Gets file metadata for category suggestions, displays toast, caches categories found, calls uploadController
     */
    private void uploadBegins() {
        fileObj.getFileCoordinates(locationPermitted);

        Toast startingToast = Toast.makeText(this, R.string.uploading_started, Toast.LENGTH_LONG);
        startingToast.show();

        if (!fileObj.isCacheFound()) {
            //Has to be called after apiCall.request()
            cacheController.cacheCategory();
            Timber.d("Cache the categories found");
        }

        uploadController.startUpload(title, mediaUri, description, mimeType, source, decimalCoords, c -> {
            ShareActivity.this.contribution = c;
            showPostUpload();
        });
    }

    /**
     * Starts CategorizationFragment after uploadBegins.
     */
    private void showPostUpload() {
        if (categorizationFragment == null) {
            categorizationFragment = new CategorizationFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.single_upload_fragment_container, categorizationFragment, "categorization")
                .commit();
    }

    /**
     * Send categories to modifications queue after they are selected
     * @param categories categories selected
     */
    @Override
    public void onCategoriesSave(List<String> categories) {
        if (categories.size() > 0) {
            ModifierSequence categoriesSequence = new ModifierSequence(contribution.getContentUri());

            categoriesSequence.queueModifier(new CategoryModifier(categories.toArray(new String[]{})));
            categoriesSequence.queueModifier(new TemplateRemoveModifier("Uncategorized"));
            modifierSequenceDao.save(categoriesSequence);
        }

        // FIXME: Make sure that the content provider is up
        // This is the wrong place for it, but bleh - better than not having it turned on by default for people who don't go throughl ogin
        ContentResolver.setSyncAutomatically(sessionManager.getCurrentAccount(), ModificationsContentProvider.MODIFICATIONS_AUTHORITY, true); // Enable sync by default!

        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (contribution != null) {
            outState.putParcelable("contribution", contribution);
        }
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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_share);
        ButterKnife.bind(this);
        initBack();
        backgroundImageView.setHierarchy(GenericDraweeHierarchyBuilder
                .newInstance(getResources())
                .setPlaceholderImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_image_black_24dp, getTheme()))
                .setFailureImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_error_outline_black_24dp, getTheme()))
                .build());

        receiveImageIntent();

        if (savedInstanceState != null) {
            contribution = savedInstanceState.getParcelable("contribution");
        }

        requestAuthToken();
        Timber.d("Uri: %s", mediaUri.toString());
        Timber.d("Ext storage dir: %s", Environment.getExternalStorageDirectory());

        useNewPermissions = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            useNewPermissions = true;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationPermitted = true;
            }
        }

        // Check location permissions if M or newer for category suggestions, request via snackbar if not present
        if (!locationPermitted) {
            requestPermissionUsingSnackBar(
                    getString(R.string.location_permission_rationale),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERM_ON_CREATE_LOCATION);
        }

        SingleUploadFragment shareView = (SingleUploadFragment) getSupportFragmentManager().findFragmentByTag("shareView");
        categorizationFragment = (CategorizationFragment) getSupportFragmentManager().findFragmentByTag("categorization");
        if (shareView == null && categorizationFragment == null) {
            shareView = new SingleUploadFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.single_upload_fragment_container, shareView, "shareView")
                    .commitAllowingStateLoss();
        }
        uploadController.prepareService();

        ContentResolver contentResolver = this.getContentResolver();
        fileObj = new FileProcessor(mediaUri, contentResolver, this);
        checkIfFileExists();
        imageObj = fileObj.getFileCoordinates(locationPermitted);
    }

    /**
     * Receive intent from ContributionController.java when user selects picture to upload
     */
    private void receiveImageIntent() {
        Intent intent = getIntent();

        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            mediaUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (intent.hasExtra(UploadService.EXTRA_SOURCE)) {
                source = intent.getStringExtra(UploadService.EXTRA_SOURCE);
            } else {
                source = Contribution.SOURCE_EXTERNAL;
            }
            if (intent.hasExtra("isDirectUpload")) {
                Timber.d("This was initiated by a direct upload from Nearby");
                isNearbyUpload = true;
            }
            mimeType = intent.getType();
        }

        if (mediaUri != null) {
            backgroundImageView.setImageURI(mediaUri);
        }
    }

    /**
     * Function to display the zoom and map FAB
     */
    private void showFABMenu() {
        isFABOpen=true;

        if( imageObj != null && imageObj.imageCoordsExists)
            mapButton.setVisibility(View.VISIBLE);
        zoomInButton.setVisibility(View.VISIBLE);

        mainFab.animate().rotationBy(180);
        mapButton.animate().translationY(-getResources().getDimension(R.dimen.second_fab));
        zoomInButton.animate().translationY(-getResources().getDimension(R.dimen.first_fab));
    }

    /**
     * Function to close the zoom and map FAB
     */
    private void closeFABMenu(){
        isFABOpen=false;
        mainFab.animate().rotationBy(-180);
        mapButton.animate().translationY(0);
        zoomInButton.animate().translationY(0).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if(!isFABOpen){
                    mapButton.setVisibility(View.GONE);
                    zoomInButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
    }

    /**
     * Checks if upload was initiated via Nearby
     * @return true if upload was initiated via Nearby
     */
    protected boolean isNearbyUpload() {
        return isNearbyUpload;
    }

    /**
     * Handles BOTH snackbar permission request (for location) and submit button permission request (for storage)
     * @param requestCode type of request
     * @param permissions permissions requested
     * @param grantResults grant results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERM_ON_CREATE_LOCATION: {
                if (grantResults.length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermitted = true;
                    checkIfFileExists();
                }
                return;
            }

            // Storage (from submit button) - this needs to be separate from (1) because only the
            // submit button should bring user to next screen
            case REQUEST_PERM_ON_SUBMIT_STORAGE: {
                if (grantResults.length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //It is OK to call this at both (1) and (4) because if perm had been granted at
                    //snackbar, user should not be prompted at submit button
                    checkIfFileExists();

                    //Uploading only begins if storage permission granted from arrow icon
                    uploadBegins();
                    snackbar.dismiss();
                }
            }
        }
    }

    /**
     *  Displays Snackbar to ask for location permissions
     */
    private Snackbar requestPermissionUsingSnackBar(String rationale, final String[] perms, final int code) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), rationale,
                Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok,
                view -> ActivityCompat.requestPermissions(ShareActivity.this, perms, code));
        snackbar.show();
        return snackbar;
    }

    /**
     * Check if file user wants to upload already exists on Commons
     */
    private void checkIfFileExists() {
        if (!useNewPermissions || storagePermitted) {
            if (!duplicateCheckPassed) {
                //Test SHA1 of image to see if it matches SHA1 of a file on Commons
                try {
                    InputStream inputStream = getContentResolver().openInputStream(mediaUri);
                    String fileSHA1 = getSHA1(inputStream);
                    Timber.d("Input stream created from %s", mediaUri.toString());
                    Timber.d("File SHA1 is: %s", fileSHA1);

                    ExistingFileAsync fileAsyncTask =
                            new ExistingFileAsync(new WeakReference<Activity>(this), fileSHA1, new WeakReference<Context>(this), result -> {
                                Timber.d("%s duplicate check: %s", mediaUri.toString(), result);
                                duplicateCheckPassed = (result == DUPLICATE_PROCEED || result == NO_DUPLICATE);
                                if (duplicateCheckPassed) {
                                    //image is not a duplicate, so now check if its a unwanted picture or not
                                    fileObj.detectUnwantedPictures();
                                }
                            },mwApi);
                    fileAsyncTask.execute();
                } catch (IOException e) {
                    Timber.e(e, "IO Exception: ");
                }
            }
        } else {
            Timber.w("not ready for preprocessing: useNewPermissions=%s storage=%s location=%s",
                    useNewPermissions, storagePermitted, locationPermitted);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            imageObj.unregisterLocationManager();
            Timber.d("Unregistered locationManager");
        } catch (NullPointerException e) {
            Timber.d("locationManager does not exist, not unregistered");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uploadController.cleanup();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (categorizationFragment != null && categorizationFragment.isVisible()) {
                    categorizationFragment.showBackButtonDialog();
                } else {
                    onBackPressed();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Allows zooming in to the image about to be uploaded. Called when zoom FAB is tapped
     */
    private void zoomImageFromThumb(final View thumbView, Uri imageuri) {
        // If there's an animation in progress, cancel it immediately and proceed with this one.
        if (CurrentAnimator != null) {
            CurrentAnimator.cancel();
        }
        ViewUtil.hideKeyboard(ShareActivity.this.findViewById(R.id.titleEdit | R.id.descEdit));
        closeFABMenu();
        mainFab.setVisibility(View.GONE);

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(imageuri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Zoom zoomObj = new Zoom(thumbView, flContainer, this.getContentResolver());
        Bitmap scaledImage = zoomObj.createScaledImage(input, imageuri);

        // Load the high-resolution "zoomed-in" image.
        expandedImageView.setImageBitmap(scaledImage);

        float startScale = zoomObj.adjustStartEndBounds(startBounds, finalBounds, globalOffset);

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.setAlpha(0f);
        expandedImageView.setVisibility(View.VISIBLE);
        zoomOutButton.setVisibility(View.VISIBLE);
        zoomInButton.setVisibility(View.GONE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale, 1f))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale, 1f));
        set.setDuration(ShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                CurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                CurrentAnimator = null;
            }
        });
        set.start();
        CurrentAnimator = set;

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        startScaleFinal = startScale;
    }

    /**
     * Called when user taps the ^ FAB button, expands to show Zoom and Map
     */
    @OnClick(R.id.main_fab)
    public void onMainFabClicked() {
        if (!isFABOpen) {
            showFABMenu();
        } else {
            closeFABMenu();
        }
    }

    @OnClick(R.id.media_upload_zoom_in)
    public void onZoomInFabClicked() {
        try {
            zoomImageFromThumb(backgroundImageView, mediaUri);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @OnClick(R.id.media_upload_zoom_out)
    public void onZoomOutFabClicked() {
        if (CurrentAnimator != null) {
            CurrentAnimator.cancel();
        }
        zoomOutButton.setVisibility(View.GONE);
        mainFab.setVisibility(View.VISIBLE);

        // Animate the four positioning/sizing properties in parallel,
        // back to their original values.
        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScaleFinal))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScaleFinal));
        set.setDuration(ShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //background image view is thumbView
                backgroundImageView.setAlpha(1f);
                expandedImageView.setVisibility(View.GONE);
                CurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                //background image view is thumbView
                backgroundImageView.setAlpha(1f);
                expandedImageView.setVisibility(View.GONE);
                CurrentAnimator = null;
            }
        });
        set.start();
        CurrentAnimator = set;
    }

    @OnClick(R.id.media_map)
    public void onFabShowMapsClicked() {
        if (imageObj != null && imageObj.imageCoordsExists) {
            Uri gmmIntentUri = Uri.parse("google.streetview:cbll=" + imageObj.getDecLatitude() + "," + imageObj.getDecLongitude());
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        }
    }
}
