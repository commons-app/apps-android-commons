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
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;

import java.util.ArrayList;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.HandlerService;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.*;
import fr.free.nrw.commons.media.*;
import fr.free.nrw.commons.upload.UploadService;

public  class       ContributionsActivity
        extends     AuthenticatedActivity
        implements  LoaderManager.LoaderCallbacks<Object>,
                    AdapterView.OnItemClickListener,
                    MediaDetailPagerFragment.MediaDetailProvider,
                    FragmentManager.OnBackStackChangedListener,
                    ContributionsListFragment.SourceRefresher {

    private Cursor allContributions;
    private ContributionsListFragment contributionsList;
    private MediaDetailPagerFragment mediaDetails;
    private UploadService uploadService;
    private boolean isUploadServiceConnected;
    private ArrayList<DataSetObserver> observersWaitingForLoad = new ArrayList<DataSetObserver>();
    private String CONTRIBUTION_SELECTION = "";
    private ListView mDrawerList;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private String mActivityTitle;
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
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            uploadService = (UploadService) ((HandlerService.HandlerServiceLocalBinder)binder).getService();
            isUploadServiceConnected = true;
        }

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

    private void addDrawerItems() {
        String[] osArray = {"About", "Nearby", "Settings", "Feedback",};
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, osArray);
        mDrawerList.setAdapter(mAdapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("ContributionsActivity", "Item " + position + " selected");
            }
        });
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle("Navigation!");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mActivityTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title_activity_contributions);
        setContentView(R.layout.activity_contributions);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mActivityTitle = getTitle().toString();

        mDrawerList = (ListView)findViewById(R.id.drawer_list);
        addDrawerItems();

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
            Log.d("Commons", "Restarting for" + c.toContentValues().toString());
        } else {
            Log.d("Commons", "Skipping re-upload for non-failed " + c.toContentValues().toString());
        }
    }

    public void deleteUpload(int i) {
        allContributions.moveToPosition(i);
        Contribution c = Contribution.fromCursor(allContributions);
        if(c.getState() == Contribution.STATE_FAILED) {
            Log.d("Commons", "Deleting failed contrib " + c.toContentValues().toString());
            c.setContentProviderClient(getContentResolver().acquireContentProviderClient(ContributionsContentProvider.AUTHORITY));
            c.delete();
        } else {
            Log.d("Commons", "Skipping deletion for non-failed contrib " + c.toContentValues().toString());
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


    public void onItemClick(AdapterView<?> adapterView, View view, int position, long item) {
        showDetail(position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    public Loader onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this, ContributionsContentProvider.BASE_URI, Contribution.Table.ALL_FIELDS, CONTRIBUTION_SELECTION, null, CONTRIBUTION_SORT);
    }

    public void onLoadFinished(Loader cursorLoader, Object result) {

        Cursor cursor = (Cursor) result;
        if(contributionsList.getAdapter() == null) {
            contributionsList.setAdapter(new ContributionsListAdapter(this, cursor, 0));
        } else {
            ((CursorAdapter)contributionsList.getAdapter()).swapCursor(cursor);
        }

        getSupportActionBar().setSubtitle(getResources().getQuantityString(R.plurals.contributions_subtitle, cursor.getCount(), cursor.getCount()));

        contributionsList.clearSyncMessage();
        notifyAndMigrateDataSetObservers();

    }

    public void onLoaderReset(Loader cursorLoader) {

        ((CursorAdapter) contributionsList.getAdapter()).swapCursor(null);

    }

    //FIXME: Potential cause of wrong image display bug
    public Media getMediaAtPosition(int i) {
        if (contributionsList.getAdapter() == null) {
            // not yet ready to return data
            return null;
        } else  {
            return Contribution.fromCursor((Cursor) contributionsList.getAdapter().getItem(i));
        }
    }

    public int getTotalMediaCount() {
        if(contributionsList.getAdapter() == null) {
            return 0;
        }
        return contributionsList.getAdapter().getCount();
    }

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

    public void registerDataSetObserver(DataSetObserver observer) {
        Adapter adapter = contributionsList.getAdapter();
        if (adapter == null) {
            observersWaitingForLoad.add(observer);
        } else {
            adapter.registerDataSetObserver(observer);
        }
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        Adapter adapter = contributionsList.getAdapter();
        if (adapter == null) {
            observersWaitingForLoad.remove(observer);
        } else {
            adapter.unregisterDataSetObserver(observer);
        }
    }

    public void onBackStackChanged() {
        if(mediaDetails != null && mediaDetails.isVisible()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    public void refreshSource() {
        getSupportLoaderManager().restartLoader(0, null, this);
    }
}
