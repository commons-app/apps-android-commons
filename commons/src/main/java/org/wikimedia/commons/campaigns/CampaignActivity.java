package org.wikimedia.commons.campaigns;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import org.wikimedia.commons.R;

public  class CampaignActivity
        extends SherlockFragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private ListView campaignsListView;
    private CampaignsListAdapter campaignsListAdapter;
    private Cursor allCampaigns;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campaigns);
        campaignsListView = (ListView) findViewById(R.id.campaignsList);
        getSupportLoaderManager().initLoader(0, null, this);
    }

    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this, CampaignsContentProvider.BASE_URI, Campaign.Table.ALL_FIELDS, "", null, "");
    }

    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if(campaignsListAdapter == null) {
            campaignsListAdapter = new CampaignsListAdapter(this, cursor, 0);
            campaignsListView.setAdapter(campaignsListAdapter);
        } else {
            campaignsListAdapter.swapCursor(cursor);
        }
    }

    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        campaignsListAdapter.swapCursor(null);
    }
}