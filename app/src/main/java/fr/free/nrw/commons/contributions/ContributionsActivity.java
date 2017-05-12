package fr.free.nrw.commons.contributions;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.Locale;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.HandlerService;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.auth.WikiAccountAuthenticator;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.upload.UploadService;
import timber.log.Timber;

public  class       ContributionsActivity
        extends     AuthenticatedActivity
        implements  LoaderManager.LoaderCallbacks<Cursor>,
                    AdapterView.OnItemClickListener,
                    MediaDetailPagerFragment.MediaDetailProvider,
                    FragmentManager.OnBackStackChangedListener,
                    ContributionsListFragment.SourceRefresher {

    private Cursor allContributions;
    private ContributionsListFragment contributionsList;
    private MediaDetailPagerFragment mediaDetails;
    private UploadService uploadService;
    private boolean isUploadServiceConnected;
    private ArrayList<DataSetObserver> observersWaitingForLoad = new ArrayList<>();
    private String CONTRIBUTION_SELECTION = "";
    /*
        This sorts in the following order:
        Currently Uploading
        Failed (Sorted in ascending order of time added - FIFO)
        Queued to Upload (Sorted in ascending order of time added - FIFO)
        Completed (Sorted in descending order of time added)

        This is why Contribution.STATE_COMPLETED is -1.
     */
    private String CONTRIBUTION_SORT = Contribution.Table.COLUMN_STATE + " DESC, " + Contribution.Table.COLUMN_UPLOADED + " DESC , (" + Contribution.Table.COLUMN_TIMESTAMP + " * " + Contribution.Table.COLUMN_STATE + ")";

    public ContributionsActivity() {
        super(WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE);
    }

    private ServiceConnection uploadServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            uploadService = (UploadService) ((HandlerService.HandlerServiceLocalBinder)binder).getService();
            isUploadServiceConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            // this should never happen
            throw new RuntimeException("UploadService died but the rest of the process did not!");
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isUploadServiceConnected) {
            unbindService(uploadServiceConnection);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        // Do a sync everytime we get here!
        ContentResolver.requestSync(((CommonsApplication) getApplicationContext()).getCurrentAccount(), ContributionsContentProvider.AUTHORITY, new Bundle());
        Intent uploadServiceIntent = new Intent(this, UploadService.class);
        uploadServiceIntent.setAction(UploadService.ACTION_START_SERVICE);
        startService(uploadServiceIntent);
        bindService(uploadServiceIntent, uploadServiceConnection, Context.BIND_AUTO_CREATE);

        allContributions = getContentResolver().query(ContributionsContentProvider.BASE_URI, Contribution.Table.ALL_FIELDS, CONTRIBUTION_SELECTION, null, CONTRIBUTION_SORT);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title_activity_contributions);
        setContentView(R.layout.activity_contributions);

        // Activity can call methods in the fragment by acquiring a reference to the Fragment from FragmentManager, using findFragmentById()
        contributionsList = (ContributionsListFragment)getSupportFragmentManager().findFragmentById(R.id.contributionsListFragment);

        getSupportFragmentManager().addOnBackStackChangedListener(this);
        if (savedInstanceState != null) {
            mediaDetails = (MediaDetailPagerFragment)getSupportFragmentManager().findFragmentById(R.id.contributionsFragmentContainer);
            // onBackStackChanged uses mediaDetails.isVisible() but this returns false now.
            // Use the saved value from before pause or orientation change.
            if (mediaDetails != null && savedInstanceState.getBoolean("mediaDetailsVisible")) {
                // Feels awful that we have to reset this manually!
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
        requestAuthToken();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("mediaDetailsVisible", (mediaDetails != null && mediaDetails.isVisible()));
    }

    /** Replace whatever is in the current contributionsFragmentContainer view with mediaDetailPagerFragment,
    /   and preserve previous state in back stack.
    /   Called when user selects a contribution. */
    private void showDetail(int i) {
        if(mediaDetails == null ||!mediaDetails.isVisible()) {
            mediaDetails = new MediaDetailPagerFragment();
            this.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.contributionsFragmentContainer, mediaDetails)
                    .addToBackStack(null)
                    .commit();
            this.getSupportFragmentManager().executePendingTransactions();
        }
        mediaDetails.showImage(i);
    }

    public void retryUpload(int i) {
        allContributions.moveToPosition(i);
        Contribution c = Contribution.fromCursor(allContributions);
        if(c.getState() == Contribution.STATE_FAILED) {
            uploadService.queue(UploadService.ACTION_UPLOAD_FILE, c);
            Timber.d("Restarting for %s", c.toContentValues());
        } else {
            Timber.d("Skipping re-upload for non-failed %s", c.toContentValues());
        }
    }

    public void deleteUpload(int i) {
        allContributions.moveToPosition(i);
        Contribution c = Contribution.fromCursor(allContributions);
        if(c.getState() == Contribution.STATE_FAILED) {
            Timber.d("Deleting failed contrib %s", c.toContentValues());
            c.setContentProviderClient(getContentResolver().acquireContentProviderClient(ContributionsContentProvider.AUTHORITY));
            c.delete();
        } else {
            Timber.d("Skipping deletion for non-failed contrib %s", c.toContentValues());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                if(mediaDetails.isVisible()) {
                    getSupportFragmentManager().popBackStack();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onAuthFailure() {
        finish(); // If authentication failed, we just exit
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long item) {
        showDetail(position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this, ContributionsContentProvider.BASE_URI, Contribution.Table.ALL_FIELDS, CONTRIBUTION_SELECTION, null, CONTRIBUTION_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if(contributionsList.getAdapter() == null) {
            contributionsList.setAdapter(new ContributionsListAdapter(this, cursor, 0));
        } else {
            ((CursorAdapter)contributionsList.getAdapter()).swapCursor(cursor);
        }

        if (cursor.getCount() == 0
                && Locale.getDefault().getISO3Language().equals(Locale.ENGLISH.getISO3Language())) {
            //cursor count is zero and language is english -
            // we need to set the message for 0 case explicitly.
            getSupportActionBar().setSubtitle(getResources()
                    .getString(R.string.contributions_subtitle_zero));
        } else {
            getSupportActionBar().setSubtitle(getResources().getQuantityString(R.plurals.contributions_subtitle, cursor.getCount(), cursor.getCount()));
        }

        contributionsList.clearSyncMessage();
        notifyAndMigrateDataSetObservers();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        ((CursorAdapter) contributionsList.getAdapter()).swapCursor(null);
    }

    //FIXME: Potential cause of wrong image display bug
    @Override
    public Media getMediaAtPosition(int i) {
        if (contributionsList.getAdapter() == null) {
            // not yet ready to return data
            return null;
        } else  {
            return Contribution.fromCursor((Cursor) contributionsList.getAdapter().getItem(i));
        }
    }

    @Override
    public int getTotalMediaCount() {
        if(contributionsList.getAdapter() == null) {
            return 0;
        }
        return contributionsList.getAdapter().getCount();
    }

    @Override
    public void notifyDatasetChanged() {
        // Do nothing for now
    }

    private void notifyAndMigrateDataSetObservers() {
        Adapter adapter = contributionsList.getAdapter();

        // First, move the observers over to the adapter now that we have it.
        for (DataSetObserver observer : observersWaitingForLoad) {
            adapter.registerDataSetObserver(observer);
        }
        observersWaitingForLoad.clear();

        // Now fire off a first notification...
        for (DataSetObserver observer : observersWaitingForLoad) {
            observer.onChanged();
        }
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        Adapter adapter = contributionsList.getAdapter();
        if (adapter == null) {
            observersWaitingForLoad.add(observer);
        } else {
            adapter.registerDataSetObserver(observer);
        }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        Adapter adapter = contributionsList.getAdapter();
        if (adapter == null) {
            observersWaitingForLoad.remove(observer);
        } else {
            adapter.unregisterDataSetObserver(observer);
        }
    }

    @Override
    public void onBackStackChanged() {
        if(mediaDetails != null && mediaDetails.isVisible()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public void refreshSource() {
        getSupportLoaderManager().restartLoader(0, null, this);
    }
}
