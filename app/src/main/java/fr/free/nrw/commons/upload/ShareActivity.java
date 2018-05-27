package fr.free.nrw.commons.upload;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.BitmapCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.OnClick;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;
import com.github.chrisbanes.photoview.PhotoView;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.ButterKnife;
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
import io.reactivex.schedulers.Schedulers;
import fr.free.nrw.commons.utils.ViewUtil;
import timber.log.Timber;

import static fr.free.nrw.commons.upload.ExistingFileAsync.Result.DUPLICATE_PROCEED;
import static fr.free.nrw.commons.upload.ExistingFileAsync.Result.NO_DUPLICATE;

/**
 * Activity for the title/desc screen after image is selected. Also starts processing image
 * GPS coordinates or user location (if enabled in Settings) for category suggestions.
 */
public class ShareActivity
        extends AuthenticatedActivity
        implements SingleUploadFragment.OnUploadActionInitiated,
        OnCategoriesSaveHandler,SimilarImageDialogFragment.onResponse {

    @BindView(R.id.container)
    FrameLayout flContainer;
    @BindView(R.id.backgroundImage)
    SimpleDraweeView backgroundImageView;
    @BindView(R.id.media_map)
    FloatingActionButton mapsFragment; //Lets stick to camelCase
    @BindView(R.id.media_upload_zoom_in)
    FloatingActionButton zoomInButton;
    @BindView(R.id.media_upload_zoom_out)
    FloatingActionButton zoomOutButton;
    @BindView(R.id.main_fab)
    FloatingActionButton mainFab;
    @BindView(R.id.expanded_image)
    PhotoView expandedImageView;


    private static final int REQUEST_PERM_ON_CREATE_STORAGE = 1;
    private static final int REQUEST_PERM_ON_CREATE_LOCATION = 2;
    private static final int REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION = 3;
    private static final int REQUEST_PERM_ON_SUBMIT_STORAGE = 4;
    private CategorizationFragment categorizationFragment;

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

    private String source;
    private String mimeType;

    private Uri mediaUri;
    private Contribution contribution;
    private boolean cacheFound;

    private GPSExtractor imageObj;
    private GPSExtractor tempImageObj;
    private String decimalCoords;

    private boolean useNewPermissions = false;
    private boolean storagePermitted = false;
    private boolean locationPermitted = false;

    private String title;
    private String description;
    private Snackbar snackbar;
    private boolean duplicateCheckPassed = false;

    private boolean haveCheckedForOtherImages = false;
    private boolean isNearbyUpload = false;

    private Animator CurrentAnimator;
    private long ShortAnimationDuration;
    private boolean isFABOpen = false;

    //Had to make them class variables, to extract out the click listeners, also I see no harm in this
    final Rect startBounds = new Rect();
    final Rect finalBounds = new Rect();
    final Point globalOffset = new Point();
    private float startScaleFinal;


    /**
     * Called when user taps the submit button.
     */
    @Override
    public void uploadActionInitiated(String title, String description) {

        this.title = title;
        this.description = description;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check for Storage permission that is required for upload.
            // Do not allow user to proceed without permission, otherwise will crash
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

    @RequiresApi(16)
    private boolean needsToRequestStoragePermission() {
        // We need to ask storage permission when
        // the file is not owned by this application, (e.g. shared from the Gallery)
        // and permission is not obtained.
        return !FileUtils.isSelfOwned(getApplicationContext(), mediaUri)
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED);
    }

    private void uploadBegins() {
        getFileMetadata(locationPermitted);

        Toast startingToast = Toast.makeText(this, R.string.uploading_started, Toast.LENGTH_LONG);
        startingToast.show();

        if (!cacheFound) {
            //Has to be called after apiCall.request()
            cacheController.cacheCategory();
            Timber.d("Cache the categories found");
        }

        uploadController.startUpload(title, mediaUri, description, mimeType, source, decimalCoords, c -> {
            ShareActivity.this.contribution = c;
            showPostUpload();
        });
    }

    private void showPostUpload() {
        if (categorizationFragment == null) {
            categorizationFragment = new CategorizationFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.single_upload_fragment_container, categorizationFragment, "categorization")
                .commit();
    }

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

    protected boolean isNearbyUpload() {
        return isNearbyUpload;
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

        //Receive intent from ContributionController.java when user selects picture to upload
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
        if (savedInstanceState != null) {
            contribution = savedInstanceState.getParcelable("contribution");
        }

        requestAuthToken();

        Timber.d("Uri: %s", mediaUri.toString());
        Timber.d("Ext storage dir: %s", Environment.getExternalStorageDirectory());

        useNewPermissions = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            useNewPermissions = true;

            if (!needsToRequestStoragePermission()) {
                storagePermitted = true;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationPermitted = true;
            }
        }

        // Check storage permissions if marshmallow or newer
        if (useNewPermissions && (!storagePermitted || !locationPermitted)) {
            if (!storagePermitted && !locationPermitted) {
                String permissionRationales =
                        getResources().getString(R.string.read_storage_permission_rationale) + "\n"
                                + getResources().getString(R.string.location_permission_rationale);
                snackbar = requestPermissionUsingSnackBar(
                        permissionRationales,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION);
                View snackbarView = snackbar.getView();
                TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
                textView.setMaxLines(3);
            } else if (!storagePermitted) {
                requestPermissionUsingSnackBar(
                        getString(R.string.read_storage_permission_rationale),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERM_ON_CREATE_STORAGE);
            } else if (!locationPermitted) {
                requestPermissionUsingSnackBar(
                        getString(R.string.location_permission_rationale),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERM_ON_CREATE_LOCATION);
            }
        }
        performPreUploadProcessingOfFile();


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
        mapsFragment.setVisibility(View.VISIBLE);
        if( imageObj == null || imageObj.imageCoordsExists != true){
            mapsFragment.setVisibility(View.INVISIBLE);
        }

    }

    /*
     * Function to display the zoom and map FAB
     */
    private void showFABMenu(){
        isFABOpen=true;

        if( imageObj != null && imageObj.imageCoordsExists == true)
        mapsFragment.setVisibility(View.VISIBLE);
        zoomInButton.setVisibility(View.VISIBLE);

        mainFab.animate().rotationBy(180);
        mapsFragment.animate().translationY(-getResources().getDimension(R.dimen.second_fab));
        zoomInButton.animate().translationY(-getResources().getDimension(R.dimen.first_fab));
    }

    /*
     * function to close the zoom and map FAB
     */
    private void closeFABMenu(){
        isFABOpen=false;
        mainFab.animate().rotationBy(-180);
        mapsFragment.animate().translationY(0);
        zoomInButton.animate().translationY(0).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if(!isFABOpen){
                    mapsFragment.setVisibility(View.GONE);
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


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERM_ON_CREATE_STORAGE: {
                if (grantResults.length >= 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    backgroundImageView.setImageURI(mediaUri);
                    storagePermitted = true;
                    performPreUploadProcessingOfFile();
                }
                return;
            }
            case REQUEST_PERM_ON_CREATE_LOCATION: {
                if (grantResults.length >= 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermitted = true;
                    performPreUploadProcessingOfFile();
                }
                return;
            }
            case REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION: {
                if (grantResults.length >= 2
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    backgroundImageView.setImageURI(mediaUri);
                    storagePermitted = true;
                    performPreUploadProcessingOfFile();
                }
                if (grantResults.length >= 2
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    locationPermitted = true;
                    performPreUploadProcessingOfFile();
                }
                return;
            }
            // Storage (from submit button) - this needs to be separate from (1) because only the
            // submit button should bring user to next screen
            case REQUEST_PERM_ON_SUBMIT_STORAGE: {
                if (grantResults.length >= 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //It is OK to call this at both (1) and (4) because if perm had been granted at
                    //snackbar, user should not be prompted at submit button
                    performPreUploadProcessingOfFile();

                    //Uploading only begins if storage permission granted from arrow icon
                    uploadBegins();
                    snackbar.dismiss();
                }
                return;
            }
        }
    }

    private void performPreUploadProcessingOfFile() {
        if (!useNewPermissions || storagePermitted) {
            if (!duplicateCheckPassed) {
                //Test SHA1 of image to see if it matches SHA1 of a file on Commons
                try {
                    InputStream inputStream = getContentResolver().openInputStream(mediaUri);
                    Timber.d("Input stream created from %s", mediaUri.toString());
                    String fileSHA1 = getSHA1(inputStream);
                    Timber.d("File SHA1 is: %s", fileSHA1);

                    ExistingFileAsync fileAsyncTask =
                            new ExistingFileAsync(new WeakReference<Activity>(this), fileSHA1, new WeakReference<Context>(this), result -> {
                                Timber.d("%s duplicate check: %s", mediaUri.toString(), result);
                                duplicateCheckPassed = (result == DUPLICATE_PROCEED
                                        || result == NO_DUPLICATE);
                                /*
                                 TODO: 16/9/17 should we run DetectUnwantedPicturesAsync if DUPLICATE_PROCEED is returned? Since that means
                                 we are processing images that are already on server???...
                                */

                                if (duplicateCheckPassed) {
                                    //image can be uploaded, so now check if its a useless picture or not
                                    performUnwantedPictureDetectionProcess();
                                }

                            },mwApi);
                    fileAsyncTask.execute();
                } catch (IOException e) {
                    Timber.d(e, "IO Exception: ");
                }
            }

            getFileMetadata(locationPermitted);
        } else {
            Timber.w("not ready for preprocessing: useNewPermissions=%s storage=%s location=%s",
                    useNewPermissions, storagePermitted, locationPermitted);
        }
    }

    private void performUnwantedPictureDetectionProcess() {
        String imageMediaFilePath = FileUtils.getPath(this,mediaUri);
        DetectUnwantedPicturesAsync detectUnwantedPicturesAsync
                = new DetectUnwantedPicturesAsync(new WeakReference<Activity>(this)
                                                                , imageMediaFilePath);

        detectUnwantedPicturesAsync.execute();
    }

    /*
     *  to display permission snackbar in share activity
     */
    private Snackbar requestPermissionUsingSnackBar(String rationale,
                                                    final String[] perms,
                                                    final int code) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), rationale,
                Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok,
                view -> ActivityCompat.requestPermissions(ShareActivity.this, perms, code));
        snackbar.show();
        return snackbar;
    }

    @Nullable
    private String getPathOfMediaOrCopy() {
        String filePath = FileUtils.getPath(getApplicationContext(), mediaUri);
        Timber.d("Filepath: " + filePath);
        if (filePath == null) {
            // in older devices getPath() may fail depending on the source URI
            // creating and using a copy of the file seems to work instead.
            // TODO: there might be a more proper solution than this
            String copyPath = null;
            try {
                ParcelFileDescriptor descriptor
                        = getContentResolver().openFileDescriptor(mediaUri, "r");
                if (descriptor != null) {
                    boolean useExtStorage = prefs.getBoolean("useExternalStorage", true);
                    if (useExtStorage) {
                        copyPath = Environment.getExternalStorageDirectory().toString()
                                + "/CommonsApp/" + new Date().getTime() + ".jpg";
                        File newFile = new File(Environment.getExternalStorageDirectory().toString() + "/CommonsApp");
                        newFile.mkdir();
                        FileUtils.copy(
                                descriptor.getFileDescriptor(),
                                copyPath);
                        Timber.d("Filepath (copied): %s", copyPath);
                        return copyPath;
                    }
                    copyPath = getApplicationContext().getCacheDir().getAbsolutePath()
                            + "/" + new Date().getTime() + ".jpg";
                    FileUtils.copy(
                            descriptor.getFileDescriptor(),
                            copyPath);
                    Timber.d("Filepath (copied): %s", copyPath);
                    return copyPath;
                }
            } catch (IOException e) {
                Timber.w(e, "Error in file " + copyPath);
                return null;
            }
        }
        return filePath;
    }

    /**
     * Gets coordinates for category suggestions, either from EXIF data or user location
     *
     * @param gpsEnabled if true use GPS
     */
    private void getFileMetadata(boolean gpsEnabled) {
        Timber.d("Calling GPSExtractor");
        try {
            if (imageObj == null) {
                ParcelFileDescriptor descriptor
                        = getContentResolver().openFileDescriptor(mediaUri, "r");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (descriptor != null) {
                        imageObj = new GPSExtractor(descriptor.getFileDescriptor(), this, prefs);
                    }
                } else {
                    String filePath = getPathOfMediaOrCopy();
                    if (filePath != null) {
                        imageObj = new GPSExtractor(filePath, this, prefs);
                    }
                }
            }

            if (imageObj != null) {
                // Gets image coords from exif data or user location
                decimalCoords = imageObj.getCoords(gpsEnabled);
                if(decimalCoords==null || !imageObj.imageCoordsExists){
//                  Check if the location is from GPS or EXIF
//                  Find other photos taken around the same time which has gps coordinates
                    Timber.d("EXIF:false");
                    Timber.d("EXIF call"+(imageObj==tempImageObj));
                    if(!haveCheckedForOtherImages)
                        findOtherImages(gpsEnabled);// Do not do repeat the process
                }
                else {
//                  As the selected image has GPS data in EXIF go ahead with the same.
                    useImageCoords();
                }
            }
        } catch (FileNotFoundException e) {
            Timber.w("File not found: " + mediaUri, e);
        }
    }

    private void findOtherImages(boolean gpsEnabled) {
        Timber.d("filePath"+getPathOfMediaOrCopy());
        String filePath = getPathOfMediaOrCopy();
        long timeOfCreation = new File(filePath).lastModified();//Time when the original image was created
        File folder = new File(filePath.substring(0,filePath.lastIndexOf('/')));
        File[] files = folder.listFiles();
        Timber.d("folderTime Number:"+files.length);

        for(File file : files){
            if(file.lastModified()-timeOfCreation<=(120*1000) && file.lastModified()-timeOfCreation>=-(120*1000)){
                //Make sure the photos were taken within 20seconds
                Timber.d("fild date:"+file.lastModified()+ " time of creation"+timeOfCreation);
                tempImageObj = null;//Temporary GPSExtractor to extract coords from these photos
                ParcelFileDescriptor descriptor
                        = null;
                try {
                    descriptor = getContentResolver().openFileDescriptor(Uri.parse(file.getAbsolutePath()), "r");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (descriptor != null) {
                        tempImageObj = new GPSExtractor(descriptor.getFileDescriptor(),this, prefs);
                    }
                } else {
                    if (filePath != null) {
                        tempImageObj = new GPSExtractor(file.getAbsolutePath(), this, prefs);
                    }
                }

                if(tempImageObj!=null){
                    Timber.d("not null fild EXIF"+tempImageObj.imageCoordsExists +" coords"+tempImageObj.getCoords(gpsEnabled));
                    if(tempImageObj.getCoords(gpsEnabled)!=null && tempImageObj.imageCoordsExists){
//                       Current image has gps coordinates and it's not current gps locaiton
                        Timber.d("This fild has image coords:"+ file.getAbsolutePath());
//                       Create a dialog fragment for the suggestion
                        FragmentManager fragmentManager = getSupportFragmentManager();
                        SimilarImageDialogFragment newFragment = new SimilarImageDialogFragment();
                        Bundle args = new Bundle();
                        args.putString("originalImagePath",filePath);
                        args.putString("possibleImagePath",file.getAbsolutePath());
                        newFragment.setArguments(args);
                        newFragment.show(fragmentManager, "dialog");
                        break;
                    }

                }

            }
        }
        haveCheckedForOtherImages = true; //Finished checking for other images
        return;
    }

    //I might not be supposed to change it, but still, I saw it
    @Override
    public void onPositiveResponse() {
        imageObj = tempImageObj;
        decimalCoords = imageObj.getCoords(false);// Not necessary to use gps as image already ha EXIF data
        Timber.d("EXIF from tempImageObj");
        useImageCoords();
    }

    @Override
    public void onNegativeResponse() {
        Timber.d("EXIF from imageObj");
        useImageCoords();

    }

    /**
     * Initiates retrieval of image coordinates or user coordinates, and caching of coordinates.
     * Then initiates the calls to MediaWiki API through an instance of CategpryApi.
     */
    @SuppressLint("CheckResult")
    public void useImageCoords() {
        if (decimalCoords != null) {
            Timber.d("Decimal coords of image: %s", decimalCoords);
            Timber.d("is EXIF data present:"+imageObj.imageCoordsExists+" from findOther image:"+(imageObj==tempImageObj));

            // Only set cache for this point if image has coords
            if (imageObj.imageCoordsExists) {
                double decLongitude = imageObj.getDecLongitude();
                double decLatitude = imageObj.getDecLatitude();
                cacheController.setQtPoint(decLongitude, decLatitude);
            }

            List<String> displayCatList = cacheController.findCategory();
            boolean catListEmpty = displayCatList.isEmpty();

            // If no categories found in cache, call MediaWiki API to match image coords with nearby Commons categories
            if (catListEmpty) {
                cacheFound = false;
                apiCall.request(decimalCoords)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(
                                gpsCategoryModel::setCategoryList,
                                throwable -> {
                                    Timber.e(throwable);
                                    gpsCategoryModel.clear();
                                }
                        );
                Timber.d("displayCatList size 0, calling MWAPI %s", displayCatList);
            } else {
                cacheFound = true;
                Timber.d("Cache found, setting categoryList in model to %s", displayCatList);
                gpsCategoryModel.setCategoryList(displayCatList);
            }
        }else{
            Timber.d("EXIF: no coords");
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

    /*
     * Get SHA1 of file from input stream
     */
    private String getSHA1(InputStream is) {

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            Timber.e(e, "Exception while getting Digest");
            return "";
        }

        byte[] buffer = new byte[8192];
        int read;
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            // Fill to 40 chars
            output = String.format("%40s", output).replace(' ', '0');
            Timber.i("File SHA1: %s", output);

            return output;
        } catch (IOException e) {
            Timber.e(e, "IO Exception");
            return "";
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Timber.e(e, "Exception on closing MD5 input stream");
            }
        }
    }

    /*
     * function to provide pinch zoom
     */
    private void zoomImageFromThumb(final View thumbView, Uri imageuri ) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (CurrentAnimator != null) {
            CurrentAnimator.cancel();
        }
        ViewUtil.hideKeyboard(ShareActivity.this.findViewById(R.id.titleEdit | R.id.descEdit));
        closeFABMenu();
        mainFab.setVisibility(View.GONE);
        InputStream input = null;
        Bitmap scaled = null;
        try {
            input = this.getContentResolver().openInputStream(imageuri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BitmapRegionDecoder decoder = null;
        try {
            decoder = BitmapRegionDecoder.newInstance(input, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap bitmap = decoder.decodeRegion(new Rect(10, 10, 50, 50), null);
        try {
            //Compress the Image
            System.gc();
            Runtime rt = Runtime.getRuntime();
            long maxMemory = rt.freeMemory();
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageuri);
            int bitmapByteCount= BitmapCompat.getAllocationByteCount(bitmap);
            long height = bitmap.getHeight();
            long width = bitmap.getWidth();
            long calHeight = (long) ((height * maxMemory)/(bitmapByteCount * 1.1));
            long calWidth = (long) ((width * maxMemory)/(bitmapByteCount * 1.1));
            scaled = Bitmap.createScaledBitmap(bitmap,(int) Math.min(width,calWidth), (int) Math.min(height,calHeight), true);
        } catch (IOException e) {
        } catch (NullPointerException e){
            scaled = bitmap;
        }
        // Load the high-resolution "zoomed-in" image.
        expandedImageView.setImageBitmap(scaled);



        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds);
        flContainer.getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
       float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
        // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

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
        set
                .play(ObjectAnimator.ofFloat(expandedImageView, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X,
                        startScale, 1f))
                .with(ObjectAnimator.ofFloat(expandedImageView,
                        View.SCALE_Y, startScale, 1f));
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

    /*
     * called when upper arrow floating button
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
        //This try catch block was originally holding the entire click listener on the fab button, I did not wanted to risk exceptions
        try {
            zoomImageFromThumb(backgroundImageView, mediaUri);
        } catch (Exception e) {
            Log.i("exception", e.toString());
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
        set.play(ObjectAnimator
                .ofFloat(expandedImageView, View.X, startBounds.left))
                .with(ObjectAnimator
                        .ofFloat(expandedImageView,
                                View.Y, startBounds.top))
                .with(ObjectAnimator
                        .ofFloat(expandedImageView,
                                View.SCALE_X, startScaleFinal))
                .with(ObjectAnimator
                        .ofFloat(expandedImageView,
                                View.SCALE_Y, startScaleFinal));
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
        if (imageObj != null && imageObj.imageCoordsExists == true) {
            Uri gmmIntentUri = Uri
                    .parse("google.streetview:cbll=" + imageObj.getDecLatitude() + "," + imageObj
                            .getDecLongitude());
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        }
    }
}
