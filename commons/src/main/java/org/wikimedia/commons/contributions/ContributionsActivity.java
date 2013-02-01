package org.wikimedia.commons.contributions;

import android.net.Uri;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.content.BroadcastReceiver;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.*;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import org.wikimedia.commons.ImageLoaderTask;
import org.wikimedia.commons.R;
import org.wikimedia.commons.UploadService;
import org.wikimedia.commons.auth.AuthenticatedActivity;
import org.wikimedia.commons.auth.WikiAccountAuthenticator;

// Inherit from SherlockFragmentActivity but not use Fragments. Because Loaders are available only from FragmentActivities
public class ContributionsActivity extends AuthenticatedActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public ContributionsActivity() {
        super(WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE);
    }

    private class ContributionAdapter extends CursorAdapter {

        private final int COLUMN_FILENAME;
        private final int COLUMN_LOCALURI;
        public ContributionAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
            COLUMN_FILENAME = c.getColumnIndex(Contribution.Table.COLUMN_FILENAME);
            COLUMN_LOCALURI = c.getColumnIndex(Contribution.Table.COLUMN_LOCAL_URI);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return getLayoutInflater().inflate(R.layout.layout_contribution, viewGroup, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ImageView image = (ImageView)view.findViewById(R.id.contributionImage);
            TextView title = (TextView)view.findViewById(R.id.contributionTitle);

            ImageLoaderTask imageLoader = new ImageLoaderTask(image);
            imageLoader.execute(Uri.parse(cursor.getString(COLUMN_LOCALURI)));

            title.setText(cursor.getString(COLUMN_FILENAME));

        }
    }
    private LocalBroadcastManager localBroadcastManager;

    private ListView contributionsList;
    private ContributionAdapter contributionsAdapter;

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
    protected void onAuthCookieAcquired(String authCookie) {
        Cursor allContributions = getContentResolver().query(ContributionsContentProvider.BASE_URI, CONTRIBUTIONS_PROJECTION, CONTRIBUTION_SELECTION, null, CONTRIBUTION_SORT);
        contributionsAdapter = new ContributionAdapter(this, allContributions, 0);
        contributionsList.setAdapter(contributionsAdapter);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        setContentView(R.layout.activity_contributions);
        contributionsList = (ListView)findViewById(R.id.contributionsList);

        requestAuthToken();
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
