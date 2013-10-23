package org.wikimedia.commons.contributions;

import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import org.wikimedia.commons.*;
import org.wikimedia.commons.auth.*;
import org.wikimedia.commons.campaigns.Campaign;
import org.wikimedia.commons.media.*;
import org.wikimedia.commons.upload.UploadService;

import java.util.ArrayList;

public  class       ContributionsActivity
        extends     AuthenticatedActivity
        implements  LoaderManager.LoaderCallbacks<Object>,
                    AdapterView.OnItemClickListener,
                    MediaDetailPagerFragment.MediaDetailProvider,
                    ContributionsListFragment.CurrentCampaignProvider,
                    FragmentManager.OnBackStackChangedListener {


    private Cursor allContributions;
    private ContributionsListFragment contributionsList;
    private MediaDetailPagerFragment mediaDetails;

    private Campaign campaign;

    public ContributionsActivity() {
        super(WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE);
    }

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



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isUploadServiceConnected) {
            unbindService(uploadServiceConnection);
        }
    }

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

        if(getIntent().hasExtra("campaign")) {
            this.campaign = (Campaign) getIntent().getSerializableExtra("campaign");
            this.setTitle(campaign.getTitle());
        }

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
        super.onAuthFailure();
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
        if(campaign == null) {
            return new CursorLoader(this, ContributionsContentProvider.BASE_URI, Contribution.Table.ALL_FIELDS, CONTRIBUTION_SELECTION, null, CONTRIBUTION_SORT);
        } else {
            return new CategoryImagesLoader(this, campaign.getTrackingCategory());
        }
    }

    public void onLoadFinished(Loader cursorLoader, Object result) {
        if(campaign == null) {
            Cursor cursor = (Cursor) result;
            if(contributionsList.getAdapter() == null) {
                contributionsList.setAdapter(new ContributionsListAdapter(this, cursor, 0));
            } else {
                ((CursorAdapter)contributionsList.getAdapter()).swapCursor(cursor);
            }

            getSupportActionBar().setSubtitle(getResources().getQuantityString(R.plurals.contributions_subtitle, cursor.getCount(), cursor.getCount()));
        } else {
            contributionsList.setAdapter(new MediaListAdapter(this, (ArrayList<Media>) result));
        }
    }

    public void onLoaderReset(Loader cursorLoader) {
        if(campaign == null) {
            ((CursorAdapter) contributionsList.getAdapter()).swapCursor(null);
        } else {
            //((MediaListAdapter) contributionsList.getAdapter()).
            // DO SOMETHING!
        }
    }

    public Media getMediaAtPosition(int i) {
        if(campaign == null) {
            return Contribution.fromCursor((Cursor) contributionsList.getAdapter().getItem(i));
        } else {
            return (Media) contributionsList.getAdapter().getItem(i);
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

    public void onBackStackChanged() {
        if(mediaDetails != null && mediaDetails.isVisible()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    public Campaign getCurrentCampaign() {
        return campaign;
    }
}
