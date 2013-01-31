package org.wikimedia.commons.contributions;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.content.BroadcastReceiver;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.*;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import org.wikimedia.commons.R;
import org.wikimedia.commons.UploadService;

// Inherit from SherlockFragmentActivity but not use Fragments. Because Loaders are available only from FragmentActivities
public class ContributionsActivity extends SherlockFragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private LocalBroadcastManager localBroadcastManager;

    private ListView contributionsList;
    private SimpleCursorAdapter contributionsAdapter;

    private String[] broadcastsToReceive = {
            UploadService.INTENT_CONTRIBUTION_STATE_CHANGED
    };

    private String[] CONTRIBUTIONS_PROJECTION = {
        Contribution.Table.COLUMN_ID,
        Contribution.Table.COLUMN_FILENAME,
        Contribution.Table.COLUMN_LOCAL_URI,
        Contribution.Table.COLUMN_STATE
    };

    private String CONTRIBUTION_SELECTION = "";
    private String CONTRIBUTION_SORT = Contribution.Table.COLUMN_TIMESTAMP + " DESC";

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        for(int i=0; i < broadcastsToReceive.length; i++) {
            localBroadcastManager.registerReceiver(messageReceiver, new IntentFilter(broadcastsToReceive[i]));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        for(int i=0; i < broadcastsToReceive.length; i++) {
            localBroadcastManager.unregisterReceiver(messageReceiver);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        setContentView(R.layout.activity_contributions);
        contributionsList = (ListView)findViewById(R.id.contributionsList);

        contributionsAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, null, new String[] { Contribution.Table.COLUMN_FILENAME, Contribution.Table.COLUMN_STATE }, new int[] { android.R.id.text1, android.R.id.text2 }, SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        contributionsList.setAdapter(contributionsAdapter);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this, ContributionsContentProvider.BASE_URI, CONTRIBUTIONS_PROJECTION, CONTRIBUTION_SELECTION, null, CONTRIBUTION_SORT);
    }

    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        contributionsAdapter.swapCursor(cursor);
    }

    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        contributionsAdapter.swapCursor(null);
    }

}
