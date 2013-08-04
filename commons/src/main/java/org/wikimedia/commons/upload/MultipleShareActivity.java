package org.wikimedia.commons.upload;

import java.util.*;
import java.util.concurrent.*;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.support.v4.app.FragmentManager;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.actionbarsherlock.view.MenuItem;

import org.wikimedia.commons.*;
import org.wikimedia.commons.auth.*;
import org.wikimedia.commons.category.CategorizationFragment;
import org.wikimedia.commons.contributions.*;
import org.wikimedia.commons.media.*;
import org.wikimedia.commons.modifications.CategoryModifier;
import org.wikimedia.commons.modifications.ModificationsContentProvider;
import org.wikimedia.commons.modifications.ModifierSequence;
import org.wikimedia.commons.modifications.TemplateRemoveModifier;

public  class       MultipleShareActivity
        extends     AuthenticatedActivity
        implements  MediaDetailPagerFragment.MediaDetailProvider,
                    AdapterView.OnItemClickListener,
                    FragmentManager.OnBackStackChangedListener,
                    MultipleUploadListFragment.OnMultipleUploadInitiatedHandler,
        CategorizationFragment.OnCategoriesSaveHandler {
    private CommonsApplication app;
    private ArrayList<Contribution> photosList = null;

    private MultipleUploadListFragment uploadsList;
    private MediaDetailPagerFragment mediaDetails;
    private CategorizationFragment categorizationFragment;

    private UploadController uploadController;

    public MultipleShareActivity() {
        super(WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE);
    }

    public Media getMediaAtPosition(int i) {
        return photosList.get(i);
    }

    public int getTotalMediaCount() {
        if(photosList == null) {
            return 0;
        }
        return photosList.size();
    }

    public void notifyDatasetChanged() {
        if(uploadsList != null) {
            uploadsList.notifyDatasetChanged();
        }
    }


    public void onItemClick(AdapterView<?> adapterView, View view, int index, long item) {
        showDetail(index);

    }

    public void OnMultipleUploadInitiated() {
        final ProgressDialog dialog = new ProgressDialog(MultipleShareActivity.this);
        dialog.setIndeterminate(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMax(photosList.size());
        dialog.setTitle(getResources().getQuantityString(R.plurals.starting_multiple_uploads, photosList.size(), photosList.size()));
        dialog.show();

        for(int i = 0; i < photosList.size(); i++) {
            Contribution up = photosList.get(i);
            final int uploadCount = i + 1; // Goddamn Java

            uploadController.startUpload(up, new UploadController.ContributionUploadProgress() {
                public void onUploadStarted(Contribution contribution) {
                    dialog.setProgress(uploadCount);
                    if(uploadCount == photosList.size()) {
                        dialog.dismiss();
                        Toast startingToast = Toast.makeText(getApplicationContext(), R.string.uploading_started, Toast.LENGTH_LONG);
                        startingToast.show();
                    }
                }
            });
        }

        uploadsList.setImageOnlyMode(true);

        categorizationFragment = (CategorizationFragment) this.getSupportFragmentManager().findFragmentByTag("categorization");
        if(categorizationFragment == null) {
            categorizationFragment = new CategorizationFragment();
        }
        // FIXME: Stops the keyboard from being shown 'stale' while moving out of this fragment into the next
        View target = this.getCurrentFocus();
        if (target != null) {
            InputMethodManager imm = (InputMethodManager) target.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(target.getWindowToken(), 0);
        }
        getSupportFragmentManager().beginTransaction()
                .add(R.id.uploadsFragmentContainer, categorizationFragment, "categorization")
                .commit();
    }

    public void onCategoriesSave(ArrayList<String> categories) {
        if(categories.size() > 0) {
        ContentProviderClient client = getContentResolver().acquireContentProviderClient(ModificationsContentProvider.AUTHORITY);
            for(Contribution contribution: photosList) {
                ModifierSequence categoriesSequence = new ModifierSequence(contribution.getContentUri());

                categoriesSequence.queueModifier(new CategoryModifier(categories.toArray(new String[]{})));
                categoriesSequence.queueModifier(new TemplateRemoveModifier("Uncategorized"));

                categoriesSequence.setContentProviderClient(client);
                categoriesSequence.save();
            }
        }
        // FIXME: Make sure that the content provider is up
        // This is the wrong place for it, but bleh - better than not having it turned on by default for people who don't go throughl ogin
        ContentResolver.setSyncAutomatically(app.getCurrentAccount(), ModificationsContentProvider.AUTHORITY, true); // Enable sync by default!
        EventLog.schema(CommonsApplication.EVENT_CATEGORIZATION_ATTEMPT)
                .param("username", app.getCurrentAccount().name)
                .param("categories-count", categories.size())
                .param("files-count", photosList.size())
                .param("source", Contribution.SOURCE_EXTERNAL)
                .param("result", "queued")
                .log();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                if(mediaDetails.isVisible()) {
                    getSupportFragmentManager().popBackStack();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uploadController = new UploadController(this);

        setContentView(R.layout.activity_multiple_uploads);
        app = (CommonsApplication)this.getApplicationContext();

        if(savedInstanceState != null) {
            photosList = savedInstanceState.getParcelableArrayList("uploadsList");
        }

        getSupportFragmentManager().addOnBackStackChangedListener(this);
        requestAuthToken();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        uploadController.cleanup();
    }

    private void showDetail(int i) {
        if(mediaDetails == null ||!mediaDetails.isVisible()) {
            mediaDetails = new MediaDetailPagerFragment(true);
            this.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.uploadsFragmentContainer, mediaDetails)
                    .addToBackStack(null)
                    .commit();
            this.getSupportFragmentManager().executePendingTransactions();
        }
        mediaDetails.showImage(i);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("uploadsList", photosList);
    }

    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        app.getApi().setAuthCookie(authCookie);
        Intent intent = getIntent();

        if(intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
            if(photosList == null) {
                photosList = new ArrayList<Contribution>();
                ArrayList<Uri> urisList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                for(int i=0; i < urisList.size(); i++) {
                    Contribution up = new Contribution();
                    Uri uri = urisList.get(i);
                    up.setLocalUri(uri);
                    up.setTag("mimeType", intent.getType());
                    up.setTag("sequence", i);
                    up.setSource(Contribution.SOURCE_EXTERNAL);
                    up.setMultiple(true);
                    photosList.add(up);
                }
            }

            uploadsList = (MultipleUploadListFragment) getSupportFragmentManager().findFragmentByTag("uploadsList");
            if(uploadsList == null) {
                uploadsList =  new MultipleUploadListFragment();
                this.getSupportFragmentManager()
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
        super.onAuthFailure();
        Toast failureToast = Toast.makeText(this, R.string.authentication_failed, Toast.LENGTH_LONG);
        failureToast.show();
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(categorizationFragment != null && categorizationFragment.isVisible()) {
            EventLog.schema(CommonsApplication.EVENT_CATEGORIZATION_ATTEMPT)
                    .param("username", app.getCurrentAccount().name)
                    .param("categories-count", categorizationFragment.getCurrentSelectedCount())
                    .param("files-count", photosList.size())
                    .param("source", Contribution.SOURCE_EXTERNAL)
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

    public void onBackStackChanged() {
        if(mediaDetails != null && mediaDetails.isVisible()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

}