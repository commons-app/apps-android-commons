package fr.free.nrw.commons.contributions;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
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

import javax.inject.Inject;

import butterknife.ButterKnife;
import dagger.android.AndroidInjection;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.HandlerService;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.upload.UploadService;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.content.ContentResolver.requestSync;
import static fr.free.nrw.commons.contributions.Contribution.STATE_FAILED;
import static fr.free.nrw.commons.contributions.Contribution.Table.ALL_FIELDS;
import static fr.free.nrw.commons.contributions.ContributionsContentProvider.AUTHORITY;
import static fr.free.nrw.commons.contributions.ContributionsContentProvider.BASE_URI;
import static fr.free.nrw.commons.settings.Prefs.UPLOADS_SHOWING;

public class ContributionsActivity extends AuthenticatedActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener,
        MediaDetailPagerFragment.MediaDetailProvider, FragmentManager.OnBackStackChangedListener,
        ContributionsListFragment.SourceRefresher {

    private Cursor allContributions;
    private ContributionsListFragment contributionsList;
    private MediaDetailPagerFragment mediaDetails;
    private UploadService uploadService;
    private boolean isUploadServiceConnected;
    private ArrayList<DataSetObserver> observersWaitingForLoad = new ArrayList<>();
    private String CONTRIBUTION_SELECTION = "";

    @Inject
    MediaWikiApi mediaWikiApi;

    /*
        This sorts in the following order:
        Currently Uploading
        Failed (Sorted in ascending order of time added - FIFO)
        Queued to Upload (Sorted in ascending order of time added - FIFO)
        Completed (Sorted in descending order of time added)

        This is why Contribution.STATE_COMPLETED is -1.
     */
    private String CONTRIBUTION_SORT = Contribution.Table.COLUMN_STATE + " DESC, "
            + Contribution.Table.COLUMN_UPLOADED + " DESC , ("
            + Contribution.Table.COLUMN_TIMESTAMP + " * "
            + Contribution.Table.COLUMN_STATE + ")";

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private ServiceConnection uploadServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            uploadService = (UploadService) ((HandlerService.HandlerServiceLocalBinder) binder)
                    .getService();
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
        compositeDisposable.clear();
        getSupportFragmentManager().removeOnBackStackChangedListener(this);
        super.onDestroy();
        if (isUploadServiceConnected) {
            unbindService(uploadServiceConnection);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isSettingsChanged =
                sharedPreferences.getBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED, false);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED, false);
        editor.apply();
        if (isSettingsChanged) {
            refreshSource();
        }
    }

    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        // Do a sync every time we get here!
        CommonsApplication app = ((CommonsApplication) getApplication());
        requestSync(app.getCurrentAccount(), AUTHORITY, new Bundle());
        Intent uploadServiceIntent = new Intent(this, UploadService.class);
        uploadServiceIntent.setAction(UploadService.ACTION_START_SERVICE);
        startService(uploadServiceIntent);
        bindService(uploadServiceIntent, uploadServiceConnection, Context.BIND_AUTO_CREATE);

        allContributions = getContentResolver().query(BASE_URI, ALL_FIELDS,
                CONTRIBUTION_SELECTION, null, CONTRIBUTION_SORT);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidInjection.inject(this);
        setContentView(R.layout.activity_contributions);
        ButterKnife.bind(this);

        // Activity can call methods in the fragment by acquiring a
        // reference to the Fragment from FragmentManager, using findFragmentById()
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        contributionsList = (ContributionsListFragment) supportFragmentManager
                .findFragmentById(R.id.contributionsListFragment);

        supportFragmentManager.addOnBackStackChangedListener(this);
        if (savedInstanceState != null) {
            mediaDetails = (MediaDetailPagerFragment) supportFragmentManager
                    .findFragmentById(R.id.contributionsFragmentContainer);

            getSupportLoaderManager().initLoader(0, null, this);
        }
        requestAuthToken();
        initDrawer();
        setTitle(getString(R.string.title_activity_contributions));
        setUploadCount();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        boolean mediaDetailsVisible = mediaDetails != null && mediaDetails.isVisible();
        outState.putBoolean("mediaDetailsVisible", mediaDetailsVisible);
    }

    /**
     * Replace whatever is in the current contributionsFragmentContainer view with
     * mediaDetailPagerFragment, and preserve previous state in back stack.
     * Called when user selects a contribution.
     */
    private void showDetail(int i) {
        if (mediaDetails == null || !mediaDetails.isVisible()) {
            mediaDetails = new MediaDetailPagerFragment();
            FragmentManager supportFragmentManager = getSupportFragmentManager();
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.contributionsFragmentContainer, mediaDetails)
                    .addToBackStack(null)
                    .commit();
            supportFragmentManager.executePendingTransactions();
        }
        mediaDetails.showImage(i);
    }

    public void retryUpload(int i) {
        allContributions.moveToPosition(i);
        Contribution c = Contribution.fromCursor(allContributions);
        if (c.getState() == STATE_FAILED) {
            uploadService.queue(UploadService.ACTION_UPLOAD_FILE, c);
            Timber.d("Restarting for %s", c.toContentValues());
        } else {
            Timber.d("Skipping re-upload for non-failed %s", c.toContentValues());
        }
    }

    public void deleteUpload(int i) {
        allContributions.moveToPosition(i);
        Contribution c = Contribution.fromCursor(allContributions);
        if (c.getState() == STATE_FAILED) {
            Timber.d("Deleting failed contrib %s", c.toContentValues());
            c.setContentProviderClient(getContentResolver().acquireContentProviderClient(AUTHORITY));
            c.delete();
        } else {
            Timber.d("Skipping deletion for non-failed contrib %s", c.toContentValues());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mediaDetails.isVisible()) {
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
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        int uploads = sharedPref.getInt(UPLOADS_SHOWING, 100);
        return new CursorLoader(this, BASE_URI,
                ALL_FIELDS, CONTRIBUTION_SELECTION, null,
                CONTRIBUTION_SORT + "LIMIT " + uploads);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        contributionsList.changeProgressBarVisibility(false);

        if (contributionsList.getAdapter() == null) {
            contributionsList.setAdapter(new ContributionsListAdapter(getApplicationContext(),
                    cursor, 0));
        } else {
            ((CursorAdapter) contributionsList.getAdapter()).swapCursor(cursor);
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
        } else {
            return Contribution.fromCursor((Cursor) contributionsList.getAdapter().getItem(i));
        }
    }

    @Override
    public int getTotalMediaCount() {
        if (contributionsList.getAdapter() == null) {
            return 0;
        }
        return contributionsList.getAdapter().getCount();
    }

    @SuppressWarnings("ConstantConditions")
    private void setUploadCount() {
        CommonsApplication app = ((CommonsApplication) getApplication());
        Disposable uploadCountDisposable = mediaWikiApi
                .getUploadCount(app.getCurrentAccount().name)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        uploadCount -> getSupportActionBar().setSubtitle(getResources()
                                .getQuantityString(R.plurals.contributions_subtitle,
                                        uploadCount, uploadCount)),
                        t -> Timber.e(t, "Fetching upload count failed")
                );
        compositeDisposable.add(uploadCountDisposable);
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
        initBackButton();
    }

    @Override
    public void refreshSource() {
        getSupportLoaderManager().restartLoader(0, null, this);
    }
}
