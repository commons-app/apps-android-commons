package fr.free.nrw.commons.upload;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;

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
import fr.free.nrw.commons.contributions.ContributionsActivity;
import fr.free.nrw.commons.modifications.CategoryModifier;
import fr.free.nrw.commons.modifications.ModificationsContentProvider;
import fr.free.nrw.commons.modifications.ModifierSequence;
import fr.free.nrw.commons.modifications.ModifierSequenceDao;
import fr.free.nrw.commons.modifications.TemplateRemoveModifier;

import fr.free.nrw.commons.utils.ImageUtils;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
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
    @Named("default_preferences")
    SharedPreferences prefs;

    private String source;
    private String mimeType;

    private Uri mediaUri;
    private Contribution contribution;
    private SimpleDraweeView backgroundImageView;

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
        backgroundImageView = (SimpleDraweeView) findViewById(R.id.backgroundImage);
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

    @Override
    public void onPostiveResponse() {
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
     * Then initiates the calls to MediaWiki API through an instance of MwVolleyApi.
     */
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

            MwVolleyApi apiCall = new MwVolleyApi(this);

            List<String> displayCatList = cacheController.findCategory();
            boolean catListEmpty = displayCatList.isEmpty();

            // If no categories found in cache, call MediaWiki API to match image coords with nearby Commons categories
            if (catListEmpty) {
                cacheFound = false;
                apiCall.request(decimalCoords);
                Timber.d("displayCatList size 0, calling MWAPI %s", displayCatList);
            } else {
                cacheFound = true;
                Timber.d("Cache found, setting categoryList in MwVolleyApi to %s", displayCatList);
                MwVolleyApi.setGpsCat(displayCatList);
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

    // Get SHA1 of file from input stream
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
}
