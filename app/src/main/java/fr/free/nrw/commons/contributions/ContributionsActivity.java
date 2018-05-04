package fr.free.nrw.commons.contributions;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;

import butterknife.ButterKnife;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.HandlerService;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.hamburger.HamburgerMenuContainer;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.upload.UploadService;
import fr.free.nrw.commons.utils.ExecutorUtils;
import timber.log.Timber;

public  class       ContributionsActivity
        extends     AuthenticatedActivity
        implements  LoaderManager.LoaderCallbacks<Cursor>,
                    AdapterView.OnItemClickListener,
                    MediaDetailPagerFragment.MediaDetailProvider,
                    FragmentManager.OnBackStackChangedListener,
                    ContributionsListFragment.SourceRefresher,
                    HamburgerMenuContainer {

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
        getSupportFragmentManager().removeOnBackStackChangedListener(this);
        super.onDestroy();
        if(isUploadServiceConnected) {
            unbindService(uploadServiceConnection);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isSettingsChanged =
                sharedPreferences.getBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED,false);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED,false);
        editor.apply();
        if (isSettingsChanged) {
            refreshSource();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        // Do a sync everytime we get here!
        ContentResolver.requestSync(CommonsApplication.getInstance().getCurrentAccount(), ContributionsContentProvider.AUTHORITY, new Bundle());
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
        setContentView(R.layout.activity_contributions);
        ButterKnife.bind(this);

        // Activity can call methods in the fragment by acquiring a
        // reference to the Fragment from FragmentManager, using findFragmentById()
        contributionsList = (ContributionsListFragment)getSupportFragmentManager()
                .findFragmentById(R.id.contributionsListFragment);

        getSupportFragmentManager().addOnBackStackChangedListener(this);
        if (savedInstanceState != null) {
            mediaDetails = (MediaDetailPagerFragment)getSupportFragmentManager()
                    .findFragmentById(R.id.contributionsFragmentContainer);
        }
        requestAuthToken();
        initDrawer();
        setTitle(getString(R.string.title_activity_contributions));
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
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        int uploads = sharedPref.getInt(Prefs.UPLOADS_SHOWING, 100);
        return new CursorLoader(this, ContributionsContentProvider.BASE_URI,
                Contribution.Table.ALL_FIELDS, CONTRIBUTION_SELECTION, null,
                CONTRIBUTION_SORT + "LIMIT " + uploads);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if(contributionsList.getAdapter() == null) {
            contributionsList
                    .setAdapter(new ContributionsListAdapter(getApplicationContext(), cursor, 0));
        } else {
            ((CursorAdapter)contributionsList.getAdapter()).swapCursor(cursor);
        }

        setUploadCount();

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

    private void setUploadCount() {
        UploadCountClient uploadCountClient = new UploadCountClient();
        CommonsApplication application = CommonsApplication.getInstance();
        ListenableFuture<Integer> future = uploadCountClient
                .getUploadCount(application.getCurrentAccount().name);
        Futures.addCallback(future, new FutureCallback<Integer>() {
            @Override
            public void onSuccess(Integer uploadCount) {
                getSupportActionBar().setSubtitle(getResources()
                        .getQuantityString(R.plurals.contributions_subtitle,
                                uploadCount,
                                uploadCount));
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Timber.e(t, "Fetching upload count failed");
            }
        }, ExecutorUtils.uiExecutor());
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

    public static void startYourself(Context context) {
        Intent contributionsIntent = new Intent(context, ContributionsActivity.class);
        context.startActivity(contributionsIntent);
    }
}
