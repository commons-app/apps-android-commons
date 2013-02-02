package org.wikimedia.commons.contributions;

import android.graphics.Bitmap;
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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import org.wikimedia.commons.ImageLoaderTask;
import org.wikimedia.commons.R;
import org.wikimedia.commons.ShareActivity;
import org.wikimedia.commons.UploadService;
import org.wikimedia.commons.auth.AuthenticatedActivity;
import org.wikimedia.commons.auth.WikiAccountAuthenticator;

import java.text.SimpleDateFormat;
import java.util.Date;

// Inherit from SherlockFragmentActivity but not use Fragments. Because Loaders are available only from FragmentActivities
public class ContributionsActivity extends AuthenticatedActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private final static int SELECT_FROM_GALLERY = 1;

    public ContributionsActivity() {
        super(WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE);
    }

    private class ContributionAdapter extends CursorAdapter {

        private final int COLUMN_FILENAME;
        private final int COLUMN_LOCALURI;
        private final int COLUMN_STATE;
        private final int COLUMN_UPLOADED;
        public ContributionAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
            COLUMN_FILENAME = c.getColumnIndex(Contribution.Table.COLUMN_FILENAME);
            COLUMN_STATE = c.getColumnIndex(Contribution.Table.COLUMN_STATE);
            COLUMN_LOCALURI = c.getColumnIndex(Contribution.Table.COLUMN_LOCAL_URI);
            COLUMN_UPLOADED = c.getColumnIndex(Contribution.Table.COLUMN_UPLOADED);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return getLayoutInflater().inflate(R.layout.layout_contribution, viewGroup, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ImageView image = (ImageView)view.findViewById(R.id.contributionImage);
            TextView titleView = (TextView)view.findViewById(R.id.contributionTitle);
            TextView stateView = (TextView)view.findViewById(R.id.contributionState);

            Uri imageUri = Uri.parse(cursor.getString(COLUMN_LOCALURI));
            int state = cursor.getInt(COLUMN_STATE);

            ImageLoader.getInstance().displayImage(imageUri.toString(), image, contributionDisplayOptions);

            titleView.setText(cursor.getString(COLUMN_FILENAME));
            if(state == Contribution.STATE_COMPLETED) {
                Date uploaded = new Date(cursor.getLong(COLUMN_UPLOADED));
                stateView.setText(SimpleDateFormat.getDateInstance().format(uploaded));
            } else if(state == Contribution.STATE_QUEUED) {
                stateView.setText(R.string.contribution_state_queued);
            } else if(state == Contribution.STATE_IN_PROGRESS) {
                stateView.setText(R.string.contribution_state_in_progress);
            }

        }
    }
    private LocalBroadcastManager localBroadcastManager;

    private ListView contributionsList;

    private ContributionAdapter contributionsAdapter;

    private DisplayImageOptions contributionDisplayOptions;

    private String[] broadcastsToReceive = {
            UploadService.INTENT_CONTRIBUTION_STATE_CHANGED
    };

    private String[] CONTRIBUTIONS_PROJECTION = {
        Contribution.Table.COLUMN_ID,
        Contribution.Table.COLUMN_FILENAME,
        Contribution.Table.COLUMN_LOCAL_URI,
        Contribution.Table.COLUMN_STATE,
        Contribution.Table.COLUMN_UPLOADED
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
        contributionDisplayOptions = new DisplayImageOptions.Builder().cacheInMemory()
                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
                .displayer(new FadeInBitmapDisplayer(300))
                .resetViewBeforeLoading().build();

        Cursor allContributions = getContentResolver().query(ContributionsContentProvider.BASE_URI, CONTRIBUTIONS_PROJECTION, CONTRIBUTION_SELECTION, null, CONTRIBUTION_SORT);
        contributionsAdapter = new ContributionAdapter(this, allContributions, 0);
        contributionsList.setAdapter(contributionsAdapter);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title_activity_contributions);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        setContentView(R.layout.activity_contributions);
        contributionsList = (ListView)findViewById(R.id.contributionsList);

        requestAuthToken();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case SELECT_FROM_GALLERY:
                if(resultCode == RESULT_OK) {
                    Intent shareIntent = new Intent(this, ShareActivity.class);
                    shareIntent.setAction(Intent.ACTION_SEND);
                    Log.d("Commons", "Type is " + data.getType() + " Uri is " + data.getData());
                    shareIntent.setType("image/*"); //FIXME: Find out appropriate mime type
                    shareIntent.putExtra(Intent.EXTRA_STREAM, data.getData());
                    startActivity(shareIntent);
                    break;
                }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_from_gallery:
                Intent pickImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
                pickImageIntent.setType("image/*");
                startActivityForResult(pickImageIntent,  SELECT_FROM_GALLERY);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_contributions, menu);
        return true;
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
