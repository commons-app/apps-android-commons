package fr.free.nrw.commons.upload;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.category.CategorizationFragment;
import fr.free.nrw.commons.category.OnCategoriesSaveHandler;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.modifications.CategoryModifier;
import fr.free.nrw.commons.modifications.ModificationsContentProvider;
import fr.free.nrw.commons.modifications.ModifierSequence;
import fr.free.nrw.commons.modifications.TemplateRemoveModifier;
import fr.free.nrw.commons.mwapi.EventLog;
import timber.log.Timber;

public  class       MultipleShareActivity
        extends     AuthenticatedActivity
        implements  MediaDetailPagerFragment.MediaDetailProvider,
                    AdapterView.OnItemClickListener,
                    FragmentManager.OnBackStackChangedListener,
                    MultipleUploadListFragment.OnMultipleUploadInitiatedHandler,
        OnCategoriesSaveHandler {
    private CommonsApplication app;
    private ArrayList<Contribution> photosList = null;

    private MultipleUploadListFragment uploadsList;
    private MediaDetailPagerFragment mediaDetails;
    private CategorizationFragment categorizationFragment;

    private UploadController uploadController;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Check for Storage permission that is required for upload. Do not allow user to proceed without permission, otherwise will crash
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            } else {
                multipleUploadBegins();
            }
        } else {
            multipleUploadBegins();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            multipleUploadBegins();
        }
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
                    Toast startingToast = Toast.makeText(
                            CommonsApplication.getInstance(),
                            R.string.uploading_started,
                            Toast.LENGTH_LONG
                    );
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
            imm.hideSoftInputFromWindow(target.getWindowToken(), 0);
        }
        getSupportFragmentManager().beginTransaction()
                .add(R.id.uploadsFragmentContainer, categorizationFragment, "categorization")
                .commitAllowingStateLoss();
        //See http://stackoverflow.com/questions/7469082/getting-exception-illegalstateexception-can-not-perform-this-action-after-onsa
    }

    @Override
    public void onCategoriesSave(List<String> categories) {
        if (categories.size() > 0) {
        ContentProviderClient client = getContentResolver().acquireContentProviderClient(ModificationsContentProvider.AUTHORITY);
            for (Contribution contribution: photosList) {
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
        uploadController = new UploadController();

        setContentView(R.layout.activity_multiple_uploads);
        app = CommonsApplication.getInstance();
        ButterKnife.bind(this);
        initDrawer();

        if (savedInstanceState != null) {
            photosList = savedInstanceState.getParcelableArrayList("uploadsList");
        }

        getSupportFragmentManager().addOnBackStackChangedListener(this);
        requestAuthToken();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getSupportFragmentManager().removeOnBackStackChangedListener(this);
        uploadController.cleanup();
    }

    private void showDetail(int i) {
        if (mediaDetails == null || !mediaDetails.isVisible()) {
            mediaDetails = new MediaDetailPagerFragment(true);
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
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("uploadsList", photosList);
    }

    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        app.getMWApi().setAuthCookie(authCookie);
        Intent intent = getIntent();

        if (intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
            if (photosList == null) {
                photosList = new ArrayList<>();
                ArrayList<Uri> urisList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                for (int i = 0; i < urisList.size(); i++) {
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
            if (uploadsList == null) {
                uploadsList =  new MultipleUploadListFragment();
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
    public void onBackPressed() {
        super.onBackPressed();
        if (categorizationFragment != null && categorizationFragment.isVisible()) {
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

    @Override
    public void onBackStackChanged() {
        if (mediaDetails != null && mediaDetails.isVisible()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

}