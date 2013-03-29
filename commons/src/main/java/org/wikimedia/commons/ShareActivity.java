package org.wikimedia.commons;

import android.app.*;
import android.content.*;
import android.os.*;
import android.text.*;
import com.nostra13.universalimageloader.core.ImageLoader;
import android.net.*;
import android.support.v4.app.NavUtils;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import android.widget.*;
import android.view.*;

import org.wikimedia.commons.contributions.*;
import org.wikimedia.commons.auth.*;
import org.wikimedia.commons.modifications.CategoryModifier;
import org.wikimedia.commons.modifications.ModificationsContentProvider;
import org.wikimedia.commons.modifications.ModifierSequence;
import org.wikimedia.commons.modifications.PostUploadActivity;

import java.util.ArrayList;


public  class       ShareActivity
        extends     AuthenticatedActivity
        implements  SingleUploadFragment.OnUploadActionInitiated,
                    CategorizationFragment.OnCategoriesSaveHandler {

    private SingleUploadFragment shareView;
    private CategorizationFragment categorizationFragment;

    public ShareActivity() {
        super(WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE);
    }

    private CommonsApplication app;

    private String source;
    private String mimeType;

    private Uri mediaUri;

    private Contribution contribution;

    private ImageView backgroundImageView;

    private UploadService uploadService;
    private boolean isUploadServiceConnected;
    private ServiceConnection uploadServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            uploadService = (UploadService) ((HandlerService.HandlerServiceLocalBinder)binder).getService();
            isUploadServiceConnected = true;
        }

        public void onServiceDisconnected(ComponentName componentName) {
            // this should never happen
            throw new RuntimeException("UploadService died but the rest of the process did not!");
        }
    };

    public void uploadActionInitiated(String title, String description) {
        StartUploadTask task = new SingleStartUploadTask(ShareActivity.this, uploadService, title, mediaUri, description, mimeType,  source);
        task.execute();
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
        ModifierSequence categoriesSequence = new ModifierSequence(contribution.getContentUri());
        categoriesSequence.queueModifier(new CategoryModifier(categories.toArray(new String[]{})));
        categoriesSequence.setContentProviderClient(getContentResolver().acquireContentProviderClient(ModificationsContentProvider.AUTHORITY));
        categoriesSequence.save();
        finish();
    }

    private class SingleStartUploadTask extends StartUploadTask {

        private SingleStartUploadTask(Activity context, UploadService uploadService, String rawTitle, Uri mediaUri, String description, String mimeType, String source) {
            super(context, uploadService, rawTitle, mediaUri, description, mimeType, source);
        }

        @Override
        protected void onPreExecute() {
            Toast startingToast = Toast.makeText(getApplicationContext(), R.string.uploading_started, Toast.LENGTH_LONG);
            startingToast.show();
        }

        @Override
        protected void onPostExecute(Contribution contribution) {
            super.onPostExecute(contribution);
            ShareActivity.this.contribution = contribution;
            showPostUpload();
        }
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
        EventLog.schema(CommonsApplication.EVENT_UPLOAD_ATTEMPT)
                .param("username", app.getCurrentAccount().name)
                .param("source", getIntent().getStringExtra(UploadService.EXTRA_SOURCE))
                .param("result", "cancelled")
                .log();
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


        Intent uploadServiceIntent = new Intent(getApplicationContext(), UploadService.class);
        uploadServiceIntent.setAction(UploadService.ACTION_START_SERVICE);
        startService(uploadServiceIntent);
        bindService(uploadServiceIntent, uploadServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onAuthFailure() {
        super.onAuthFailure();
        Toast failureToast = Toast.makeText(this, R.string.authentication_failed, Toast.LENGTH_LONG);
        failureToast.show();
        finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        ImageLoader.getInstance().displayImage(mediaUri.toString(), backgroundImageView);

        if(savedInstanceState != null)  {
            contribution = savedInstanceState.getParcelable("contribution");
        }

        requestAuthToken();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isUploadServiceConnected) {
            unbindService(uploadServiceConnection);
        }
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
