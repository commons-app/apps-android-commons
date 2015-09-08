package fr.nrw.free.commons.upload;

import android.app.*;
import android.content.*;
import android.os.*;
import com.nostra13.universalimageloader.core.ImageLoader;
import android.net.*;
import android.support.v4.app.NavUtils;
import com.actionbarsherlock.view.MenuItem;
import android.widget.*;

import fr.nrw.free.commons.*;
import fr.nrw.free.commons.campaigns.Campaign;
import fr.nrw.free.commons.category.CategorizationFragment;
import fr.nrw.free.commons.contributions.*;
import fr.nrw.free.commons.auth.*;
import fr.nrw.free.commons.modifications.CategoryModifier;
import fr.nrw.free.commons.modifications.ModificationsContentProvider;
import fr.nrw.free.commons.modifications.ModifierSequence;
import fr.nrw.free.commons.modifications.TemplateRemoveModifier;

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

    private UploadController uploadController;

    public void uploadActionInitiated(String title, String description) {
        Toast startingToast = Toast.makeText(getApplicationContext(), R.string.uploading_started, Toast.LENGTH_LONG);
        startingToast.show();
        uploadController.startUpload(title, mediaUri, description, mimeType,  source, new UploadController.ContributionUploadProgress() {
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Campaign campaign = null;
        if(getIntent().hasExtra(UploadService.EXTRA_CAMPAIGN)) {
            campaign = (Campaign) getIntent().getSerializableExtra(UploadService.EXTRA_CAMPAIGN);
        }
        uploadController = new UploadController(this, campaign);
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
