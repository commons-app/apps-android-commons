package fr.free.nrw.commons.upload;

import android.Manifest;
import android.Manifest.permission;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.DexterBuilder;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.single.BasePermissionListener;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.ButterKnife;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.category.CategorizationFragment;
import fr.free.nrw.commons.category.OnCategoriesSaveHandler;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.modifications.CategoryModifier;
import fr.free.nrw.commons.modifications.ModifierSequence;
import fr.free.nrw.commons.modifications.ModifierSequenceDao;
import fr.free.nrw.commons.modifications.TemplateRemoveModifier;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.utils.ContributionUtils;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.DialogUtil.Callback;
import fr.free.nrw.commons.utils.ExternalStorageUtils;
import fr.free.nrw.commons.utils.PermissionUtils;
import timber.log.Timber;

//TODO: We should use this class to see how multiple uploads are handled, and then REMOVE it.

public class MultipleShareActivity extends AuthenticatedActivity
        implements MediaDetailPagerFragment.MediaDetailProvider,
        AdapterView.OnItemClickListener,
        FragmentManager.OnBackStackChangedListener,
        MultipleUploadListFragment.OnMultipleUploadInitiatedHandler,
        OnCategoriesSaveHandler,
        ActivityCompat.OnRequestPermissionsResultCallback{

    @Inject
    MediaWikiApi mwApi;
    @Inject
    SessionManager sessionManager;
    @Inject
    UploadController uploadController;
    @Inject
    ModifierSequenceDao modifierSequenceDao;
    @Inject
    @Named("default_preferences")
    SharedPreferences prefs;

    private ArrayList<Contribution> photosList = null;

    private MultipleUploadListFragment uploadsList;
    private MediaDetailPagerFragment mediaDetails;
    private CategorizationFragment categorizationFragment;

    private boolean locationPermitted = false;
    private boolean isMultipleUploadsPrepared = false;
    private boolean isMultipleUploadsFinalised = false; // Checks is user clicked to upload button or regret before this phase
    private final String TAG="#MultipleShareActivity#";
    private AlertDialog storagePermissionInfoDialog;
    private DexterBuilder dexterStoragePermissionBuilder;

    private PermissionDeniedResponse permissionDeniedResponse;

    @Override
    public Media getMediaAtPosition(int i) {
        return photosList.get(i);
    }

    @Override
    public int getTotalMediaCount() {
        if (photosList == null) {
            return 0;
        }
        return photosList.size();
    }

    @Override
    public void notifyDatasetChanged() {
        if (uploadsList != null) {
            uploadsList.notifyDatasetChanged();
        }
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        // fixme implement me if needed
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        // fixme implement me if needed
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int index, long item) {
        showDetail(index);
    }

    @Override
    public void OnMultipleUploadInitiated() {
        // No need to request external permission here, because if user can reach this point, then she permission granted
        Timber.d("OnMultipleUploadInitiated");
        multipleUploadBegins();
    }

    private void multipleUploadBegins() {

        Timber.d("Multiple upload begins");
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setIndeterminate(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMax(photosList.size());
        dialog.setTitle(getResources().getQuantityString(R.plurals.starting_multiple_uploads, photosList.size(), photosList.size()));
        dialog.show();

        for (int i = 0; i < photosList.size(); i++) {
            Contribution up = photosList.get(i);
            final int uploadCount = i + 1; // Goddamn Java

            uploadController.startUpload(up, contribution -> {
                dialog.setProgress(uploadCount);
                if (uploadCount == photosList.size()) {
                    dialog.dismiss();
                    Toast startingToast = Toast.makeText(this, R.string.uploading_started, Toast.LENGTH_LONG);
                    startingToast.show();
                }
            });
        }

        uploadsList.setImageOnlyMode(true);

        categorizationFragment = (CategorizationFragment) getSupportFragmentManager().findFragmentByTag("categorization");
        if (categorizationFragment == null) {
            categorizationFragment = new CategorizationFragment();
        }
        // FIXME: Stops the keyboard from being shown 'stale' while moving out of this fragment into the next
        View target = getCurrentFocus();
        if (target != null) {
            InputMethodManager imm = (InputMethodManager) target.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null)
                imm.hideSoftInputFromWindow(target.getWindowToken(), 0);
        }
        getSupportFragmentManager().beginTransaction()
                .add(R.id.uploadsFragmentContainer, categorizationFragment, "categorization")
                .commitAllowingStateLoss();
        isMultipleUploadsFinalised = true;
        //See http://stackoverflow.com/questions/7469082/getting-exception-illegalstateexception-can-not-perform-this-action-after-onsa
    }

    @Override
    public void onCategoriesSave(List<String> categories) {
        if (categories.size() > 0) {
            for (Contribution contribution : photosList) {
                ModifierSequence categoriesSequence = new ModifierSequence(contribution.getContentUri());

                categoriesSequence.queueModifier(new CategoryModifier(categories.toArray(new String[]{})));
                categoriesSequence.queueModifier(new TemplateRemoveModifier("Uncategorized"));

                modifierSequenceDao.save(categoriesSequence);
            }
        }
        // FIXME: Make sure that the content provider is up
        // This is the wrong place for it, but bleh - better than not having it turned on by default for people who don't go throughl ogin
        ContentResolver.setSyncAutomatically(sessionManager.getCurrentAccount(), BuildConfig.MODIFICATION_AUTHORITY, true); // Enable sync by default!
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mediaDetails.isVisible()) {
                    getSupportFragmentManager().popBackStack();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_multiple_uploads);
        ButterKnife.bind(this);
        initDrawer();
        initPermissionsRationaleDialog();

        if (savedInstanceState != null) {
            photosList = savedInstanceState.getParcelableArrayList("uploadsList");
        }

        getSupportFragmentManager().addOnBackStackChangedListener(this);
        requestAuthToken();

        //TODO: 15/10/17 should location permission be explicitly requested if not provided?
        //check if location permission is enabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
             {
                locationPermitted = true;
            }
        }
    }


    /**
     * We have agreed to show a dialog showing why we need a particular permission.
     * This method is used to initialise the dialog which is going to show the permission's rationale.
     * The dialog is initialised along with a callback for positive and negative user actions.
     */
    private void initPermissionsRationaleDialog() {
        if (storagePermissionInfoDialog == null) {
            storagePermissionInfoDialog = DialogUtil
                    .getAlertDialogWithPositiveAndNegativeCallbacks(
                            MultipleShareActivity.this,
                            getString(R.string.storage_permission), getString(
                                    R.string.write_storage_permission_rationale_for_image_share),
                            R.drawable.ic_launcher, new Callback() {
                                @Override
                                public void onPositiveButtonClicked() {
                                    //If the user is willing to give us the permission
                                    //But had somehow previously choose never ask again, we take him to app settings to manually enable permission
                                    if (null== permissionDeniedResponse){
                                        //Dexter returned null, lets see if this ever happens
                                        return;
                                    }
                                    else if (permissionDeniedResponse.isPermanentlyDenied()) {
                                        PermissionUtils.askUserToManuallyEnablePermissionFromSettings(MultipleShareActivity.this);
                                    } else {
                                        //or if we still have chance to show runtime permission dialog, we show him that.
                                        askDexterToHandleExternalStoragePermission();
                                    }
                                }

                                @Override
                                public void onNegativeButtonClicked() {
                                    //This was the behaviour as of now, I was planning to maybe snack him with some message
                                    //and then call finish after some time, or may be it could be associated with some action on the snack
                                    //If the user does not want us to give the permission, even after showing rationale dialog, lets not trouble him anymore
                                    finish();
                                }
                            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getSupportFragmentManager().removeOnBackStackChangedListener(this);
        uploadController.cleanup();
    }

    private void showDetail(int i) {
        if (mediaDetails == null || !mediaDetails.isVisible()) {
            mediaDetails = new MediaDetailPagerFragment(true, false);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.uploadsFragmentContainer, mediaDetails)
                    .addToBackStack(null)
                    .commit();
            getSupportFragmentManager().executePendingTransactions();
        }
        mediaDetails.showImage(i);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        /* This will be true if permission request is granted before we request. Otherwise we will
         * explicitly call operations under this method again.
        */
        if (isMultipleUploadsPrepared) {
            super.onSaveInstanceState(outState);
            Timber.d("onSaveInstanceState multiple uploads is prepared, permission granted");
            outState.putParcelableArrayList("uploadsList", photosList);
        } else {
            Timber.d("onSaveInstanceState multiple uploads is not prepared, permission not granted");
            return;
        }
    }

    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        // Multiple uploads prepared boolean is used to decide when to call multipleUploadsBegin()
        isMultipleUploadsFinalised = false;
        isMultipleUploadsPrepared = false;
        mwApi.setAuthCookie(authCookie);
        if (!ExternalStorageUtils.isStoragePermissionGranted(this)) {
            //If permission is not there, handle the negative cases
            askDexterToHandleExternalStoragePermission();
            isMultipleUploadsPrepared = false;
            return; // Postpone operation to do after gettion permission
        } else {
            isMultipleUploadsPrepared = true;
            prepareMultipleUploadList();
        }
    }

    /**
     * This method initialised the Dexter's permission builder (if not already initialised). Also makes sure that the builder is initialised
     * only once, otherwise we would'nt know on which instance of it, the user is working on. And after the builder is initialised, it checks for the required
     * permission and then handles the permission status, thanks to Dexter's appropriate callbacks.
     */
    private void askDexterToHandleExternalStoragePermission() {
        Timber.d(TAG, "External storage permission is being requested");
        if (null == dexterStoragePermissionBuilder) {
            dexterStoragePermissionBuilder = Dexter.withActivity(this)
                    .withPermission(permission.WRITE_EXTERNAL_STORAGE)
                    .withListener(new BasePermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {
                            Timber.d(TAG,"User has granted us the permission for writing the external storage");
                            //If permission is granted, well and good
                            prepareMultipleUploadList();
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse response) {
                            Timber.d(TAG,"User has granted us the permission for writing the external storage");
                            //If permission is not granted in whatsoever scenario, we show him a dialog stating why we need the permission
                            permissionDeniedResponse=response;
                            if (null != storagePermissionInfoDialog && !storagePermissionInfoDialog
                                    .isShowing()) {
                                storagePermissionInfoDialog.show();
                            }
                        }
                    });
        }
        dexterStoragePermissionBuilder.check();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CommonsApplication.OPEN_APPLICATION_DETAIL_SETTINGS) {
            //OnActivity result, no matter what the result is, our function can handle that.
            askDexterToHandleExternalStoragePermission();
        }
    }

    /**
     * Prepares a list from files will be uploaded. Saves these files temporarily to external
     * storage. Adds them to uploads list
     */
    private void prepareMultipleUploadList() {
        Intent intent = getIntent();

        if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            if (photosList == null) {
                photosList = new ArrayList<>();
                ArrayList<Uri> urisList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                for (int i = 0; i < urisList.size(); i++) {
                    Contribution up = new Contribution();
                    Uri uri = urisList.get(i);
                    // Use temporarily saved file Uri instead
                    uri = ContributionUtils.saveFileBeingUploadedTemporarily(this, uri);
                    up.setLocalUri(uri);
                    up.setTag("mimeType", intent.getType());
                    up.setTag("sequence", i);
                    up.setSource(Contribution.SOURCE_EXTERNAL);
                    up.setMultiple(true);
                    String imageGpsCoordinates = extractImageGpsData(uri);
                    if (imageGpsCoordinates != null) {
                        Timber.d("GPS data for image found!");
                        up.setDecimalCoords(imageGpsCoordinates);
                    }
                    photosList.add(up);
                }
            }

            uploadsList = (MultipleUploadListFragment) getSupportFragmentManager().findFragmentByTag("uploadsList");
            if (uploadsList == null) {
                uploadsList = new MultipleUploadListFragment();
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.uploadsFragmentContainer, uploadsList, "uploadsList")
                        .commit();
            }
            setTitle(getResources().getQuantityString(R.plurals.multiple_uploads_title, photosList.size(), photosList.size()));
            uploadController.prepareService();
        }
    }

    @Override
    protected void onAuthFailure() {
        Toast failureToast = Toast.makeText(this, R.string.authentication_failed, Toast.LENGTH_LONG);
        failureToast.show();
        finish();
    }

    @Override
    public void onBackStackChanged() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(mediaDetails != null && mediaDetails.isVisible());
    }

    /**
     * Will attempt to extract the gps coordinates using exif data or by using the current
     * location if available for the image who's imageUri has been provided.
     * @param imageUri The uri of the image who's GPS coordinates data we wish to extract
     * @return GPS coordinates as a String as is returned by {@link GPSExtractor}
     */
    @Nullable
    private String extractImageGpsData(Uri imageUri) {
        Timber.d("Entering extractImagesGpsData");

        if (imageUri == null) {
            //now why would you do that???
            return null;
        }

        GPSExtractor gpsExtractor = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(imageUri,"r");
                if (fd != null) {
                    gpsExtractor = new GPSExtractor(fd.getFileDescriptor());
                }
            } else {
                String filePath = FileUtils.getPath(this,imageUri);
                if (filePath != null) {
                    gpsExtractor = new GPSExtractor(filePath);
                }
            }

            if (gpsExtractor != null) {
                //get image coordinates from exif data or user location
                return gpsExtractor.getCoords();
            }

        } catch (FileNotFoundException fnfe) {
            Timber.w(fnfe);
            return null;
        }

        return null;
    }

    // If on back pressed before sharing
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        // Remove saved files if activity is stopped before upload operation, ie user changed mind
        if (!isMultipleUploadsFinalised) {
            if (photosList != null) {
                for (Contribution contribution : photosList) {
                    Timber.d("User changed mind, didn't click to upload button, deleted file: "+contribution.getLocalUri());
                    ContributionUtils.removeTemporaryFile(contribution.getLocalUri());
                }
            }
        }
        super.onStop();
    }
}
