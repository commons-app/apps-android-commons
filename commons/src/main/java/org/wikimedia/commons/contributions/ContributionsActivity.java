package org.wikimedia.commons.contributions;

import android.*;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import org.wikimedia.commons.*;
import org.wikimedia.commons.R;
import org.wikimedia.commons.auth.AuthenticatedActivity;
import org.wikimedia.commons.auth.WikiAccountAuthenticator;
import org.wikimedia.commons.media.MediaDetailPagerFragment;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public  class       ContributionsActivity
        extends     AuthenticatedActivity
        implements  LoaderManager.LoaderCallbacks<Cursor>,
                    AdapterView.OnItemClickListener,
                    MediaDetailPagerFragment.MediaDetailProvider,
                    FragmentManager.OnBackStackChangedListener {


    private Cursor allContributions;
    private ContributionsListFragment contributionsList;
    private MediaDetailPagerFragment mediaDetails;

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

        contributionsList = (ContributionsListFragment)getSupportFragmentManager().findFragmentById(R.id.contributionsListFragment);

        getSupportFragmentManager().addOnBackStackChangedListener(this);

        requestAuthToken();
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
        Cursor cursor = (Cursor)adapterView.getItemAtPosition(position);
        Contribution c = Contribution.fromCursor(cursor);
        if(c.getState() == Contribution.STATE_FAILED) {
            uploadService.queue(UploadService.ACTION_UPLOAD_FILE, c);
            Log.d("Commons", "Restarting for" + c.toContentValues().toString());
        } else {
            Log.d("Commons", "CLicking for " + c.toContentValues());
            showDetail(position);
        }
        Log.d("Commons", "You clicked on:" + c.toContentValues().toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this, ContributionsContentProvider.BASE_URI, Contribution.Table.ALL_FIELDS, CONTRIBUTION_SELECTION, null, CONTRIBUTION_SORT);
    }

    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        allContributions = cursor;
        contributionsList.setCursor(cursor);

        getSupportActionBar().setSubtitle(getResources().getQuantityString(R.plurals.contributions_subtitle, cursor.getCount(), cursor.getCount()));
    }

    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        contributionsList.setCursor(null);
    }

    public Media getItem(int i) {
        allContributions.moveToPosition(i);
        return Contribution.fromCursor(allContributions);
    }

    public int getCount() {
        if(allContributions == null) {
            return 0;
        }
        return allContributions.getCount();
    }

    public void onBackStackChanged() {
        if(mediaDetails.isVisible()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }
}
