package fr.free.nrw.commons.campaigns;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionsActivity;

public  class CampaignActivity
        extends SherlockFragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private ListView campaignsListView;
    private CampaignsListAdapter campaignsListAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campaigns);

        ContentResolver.setSyncAutomatically(((CommonsApplication)getApplicationContext()).getCurrentAccount(), CampaignsContentProvider.AUTHORITY, true); // Enable sync by default!
        campaignsListView = (ListView) findViewById(R.id.campaignsList);

        campaignsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Campaign c = Campaign.fromCursor((Cursor) adapterView.getItemAtPosition(i));
                Intent intent = new Intent(CampaignActivity.this, ContributionsActivity.class);
                intent.putExtra("campaign", c);
                startActivity(intent);
            }
        });
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