package fr.free.nrw.commons.upload;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.EventLog;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.auth.WikiAccountAuthenticator;
import fr.free.nrw.commons.category.CategorizationFragment;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.modifications.CategoryModifier;
import fr.free.nrw.commons.modifications.ModificationsContentProvider;
import fr.free.nrw.commons.modifications.ModifierSequence;
import fr.free.nrw.commons.modifications.TemplateRemoveModifier;

/**
 * Activity for the title/desc screen after image is selected. Also starts processing image
 * GPS coordinates or user location (if enabled in Settings) for category suggestions.
 */
public  class       ShareActivity
        extends     AuthenticatedActivity
        implements  SingleUploadFragment.OnUploadActionInitiated,
        CategorizationFragment.OnCategoriesSaveHandler {

    private static final String TAG = ShareActivity.class.getName();

    private SingleUploadFragment shareView;
    private CategorizationFragment categorizationFragment;

    private CommonsApplication app;

    private String source;
    private String mimeType;
    private String mediaUriString;

    private Uri mediaUri;
    private Contribution contribution;
    private ImageView backgroundImageView;
    private UploadController uploadController;

    private CommonsApplication cacheObj;
    private boolean cacheFound;

    private GPSExtractor imageObj;

    public ShareActivity() {
        super(WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE);
    }

    public void uploadActionInitiated(String title, String description) {
        Toast startingToast = Toast.makeText(getApplicationContext(), R.string.uploading_started, Toast.LENGTH_LONG);
        startingToast.show();

        if (cacheFound == false) {
            //Has to be called after apiCall.request()
            app.cacheData.cacheCategory();
            Log.d(TAG, "Cache the categories found");
        }

        uploadController.startUpload(title, mediaUri, description, mimeType, source, new UploadController.ContributionUploadProgress() {
            public void onUploadStarted(Contribution contribution) {
                ShareActivity.this.contribution = contribution;
                showPostUpload();
            }
        });
    }

    private void showPostUpload() {
        if(categorizationFragment == null) {
            categorizationFragment = new CategorizationFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.single_upload_fragment_container, categorizationFragment, "categorization")
                .commit();
    }

    public void onCategoriesSave(ArrayList<String> categories) {
        if(categories.size() > 0) {
            ModifierSequence categoriesSequence = new ModifierSequence(contribution.getContentUri());

            categoriesSequence.queueModifier(new CategoryModifier(categories.toArray(new String[]{})));
            categoriesSequence.queueModifier(new TemplateRemoveModifier("Uncategorized"));
            categoriesSequence.setContentProviderClient(getContentResolver().acquireContentProviderClient(ModificationsContentProvider.AUTHORITY));
            categoriesSequence.save();
        }

        // FIXME: Make sure that the content provider is up
        // This is the wrong place for it, but bleh - better than not having it turned on by default for people who don't go throughl ogin
        ContentResolver.setSyncAutomatically(app.getCurrentAccount(), ModificationsContentProvider.AUTHORITY, true); // Enable sync by default!

        EventLog.schema(CommonsApplication.EVENT_CATEGORIZATION_ATTEMPT)
                .param("username", app.getCurrentAccount().name)
                .param("categories-count", categories.size())
                .param("files-count", 1)
                .param("source", contribution.getSource())
                .param("result", "queued")
                .log();
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(contribution != null) {
            outState.putParcelable("contribution", contribution);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(categorizationFragment != null && categorizationFragment.isVisible()) {
            EventLog.schema(CommonsApplication.EVENT_CATEGORIZATION_ATTEMPT)
                    .param("username", app.getCurrentAccount().name)
                    .param("categories-count", categorizationFragment.getCurrentSelectedCount())
                    .param("files-count", 1)
                    .param("source", contribution.getSource())
                    .param("result", "cancelled")
                    .log();
        } else {
            EventLog.schema(CommonsApplication.EVENT_UPLOAD_ATTEMPT)
                    .param("username", app.getCurrentAccount().name)
                    .param("source", getIntent().getStringExtra(UploadService.EXTRA_SOURCE))
                    .param("multiple", true)
                    .param("result", "cancelled")
                    .log();
        }
    }

    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        super.onAuthCookieAcquired(authCookie);
        app.getApi().setAuthCookie(authCookie);


        shareView = (SingleUploadFragment) getSupportFragmentManager().findFragmentByTag("shareView");
        categorizationFragment = (CategorizationFragment) getSupportFragmentManager().findFragmentByTag("categorization");
        if(shareView == null && categorizationFragment == null) {
                shareView = new SingleUploadFragment();
                this.getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.single_upload_fragment_container, shareView, "shareView")
                        .commit();
        }
        uploadController.prepareService();
    }

    @Override
    protected void onAuthFailure() {
        super.onAuthFailure();
        Toast failureToast = Toast.makeText(this, R.string.authentication_failed, Toast.LENGTH_LONG);
        failureToast.show();
        finish();
    }

    /**
     * Initiates retrieval of image coordinates or user coordinates, and caching of coordinates.
     * Then initiates the calls to MediaWiki API through an instance of MwVolleyApi.
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uploadController = new UploadController(this);
        setContentView(R.layout.activity_share);

        app = (CommonsApplication)this.getApplicationContext();
        backgroundImageView = (ImageView)findViewById(R.id.backgroundImage);

        Intent intent = getIntent();

        if(intent.getAction().equals(Intent.ACTION_SEND)) {
            mediaUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if(intent.hasExtra(UploadService.EXTRA_SOURCE)) {
                source = intent.getStringExtra(UploadService.EXTRA_SOURCE);
            } else {
                source = Contribution.SOURCE_EXTERNAL;
            }
            mimeType = intent.getType();
        }

        if (mediaUri != null) {
            mediaUriString = mediaUri.toString();
            ImageLoader.getInstance().displayImage(mediaUriString, backgroundImageView);
        }

        if(savedInstanceState != null)  {
            contribution = savedInstanceState.getParcelable("contribution");
        }
        requestAuthToken();


        Log.d(TAG, "Uri: " + mediaUriString);
        Log.d(TAG, "Ext storage dir: " + Environment.getExternalStorageDirectory());

        // Check storage permissions if marshmallow or newer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            if (!(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) && (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)))) {
                String permissionRationales = getResources().getString(R.string.storage_permission_rationale) + "\n" + getResources().getString(R.string.location_permission_rationale);
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), permissionRationales,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ActivityCompat.requestPermissions(ShareActivity.this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        });
                snackbar.show();
                View snackbarView = snackbar.getView();
                TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
                textView.setMaxLines(3);
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Snackbar.make(findViewById(android.R.id.content), R.string.storage_permission_rationale,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ActivityCompat.requestPermissions(ShareActivity.this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                            }
                        }).show();
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Snackbar.make(findViewById(android.R.id.content), R.string.location_permission_rationale,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ActivityCompat.requestPermissions(ShareActivity.this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                            }
                        }).show();
            }
        } else {
            // Convert image Uri to file path
            String filePath = FileUtils.getPath(this, mediaUri);
            Log.d(TAG, "Filepath: " + filePath);

            // Check location permissions if marshmallow or newer
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Calling GPSExtractor");
                imageObj = new GPSExtractor(filePath, this);
                imageObj.registerLocationManager();

                if (filePath != null && !filePath.equals("")) {
                    // Gets image coords if exist, otherwise gets last known coords
                    String decimalCoords = imageObj.getCoords();

                    if (decimalCoords != null) {
                        Log.d(TAG, "Decimal coords of image: " + decimalCoords);

                        // Only set cache for this point if image has coords
                        if (imageObj.imageCoordsExists) {
                            double decLongitude = imageObj.getDecLongitude();
                            double decLatitude = imageObj.getDecLatitude();
                            app.cacheData.setQtPoint(decLongitude, decLatitude);
                        }

                        MwVolleyApi apiCall = new MwVolleyApi(this);

                        List displayCatList = app.cacheData.findCategory();
                        boolean catListEmpty = displayCatList.isEmpty();

                        // If no categories found in cache, call MediaWiki API to match image coords with nearby Commons categories
                        if (catListEmpty) {
                            cacheFound = false;
                            apiCall.request(decimalCoords);
                            Log.d(TAG, "displayCatList size 0, calling MWAPI" + displayCatList.toString());
                        } else {
                            cacheFound = true;
                            Log.d(TAG, "Cache found, setting categoryList in MwVolleyApi to " + displayCatList.toString());
                            MwVolleyApi.setGpsCat(displayCatList);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        try {
            imageObj.unregisterLocationManager();
            Log.d(TAG, "Unregistered locationManager");
        }
        catch (NullPointerException e) {
            Log.d(TAG, "locationManager does not exist, not unregistered");
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
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
