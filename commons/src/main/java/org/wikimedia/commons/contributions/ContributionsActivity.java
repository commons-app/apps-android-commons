package org.wikimedia.commons.contributions;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.*;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import org.wikimedia.commons.*;
import org.wikimedia.commons.auth.AuthenticatedActivity;
import org.wikimedia.commons.auth.WikiAccountAuthenticator;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

// Inherit from SherlockFragmentActivity but not use Fragments. Because Loaders are available only from FragmentActivities
public class ContributionsActivity extends AuthenticatedActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private final static int SELECT_FROM_GALLERY = 1;
    private final static int SELECT_FROM_CAMERA = 2;

    private TextView progressUpdateTextView;

    public ContributionsActivity() {
        super(WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE);
    }

    private UploadService uploadService;
    private ServiceConnection uploadServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            uploadService = ((UploadService.UploadServiceLocalBinder)binder).getService();
        }

        public void onServiceDisconnected(ComponentName componentName) {
            // this should never happen
            throw new RuntimeException("UploadService died but the rest of the process did not!");
        }
    };

    private class ContributionAdapter extends CursorAdapter {

        private final int COLUMN_FILENAME;
        private final int COLUMN_LOCALURI;
        private final int COLUMN_STATE;
        private final int COLUMN_UPLOADED;
        private final int COLUMN_TRANSFERRED;
        private final int COLUMN_LENGTH;
        private final int COLUMN_IMAGE_URL;

        public ContributionAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
            COLUMN_FILENAME = c.getColumnIndex(Contribution.Table.COLUMN_FILENAME);
            COLUMN_STATE = c.getColumnIndex(Contribution.Table.COLUMN_STATE);
            COLUMN_LOCALURI = c.getColumnIndex(Contribution.Table.COLUMN_LOCAL_URI);
            COLUMN_UPLOADED = c.getColumnIndex(Contribution.Table.COLUMN_UPLOADED);
            COLUMN_LENGTH = c.getColumnIndex(Contribution.Table.COLUMN_LENGTH);
            COLUMN_TRANSFERRED = c.getColumnIndex(Contribution.Table.COLUMN_TRANSFERRED);
            COLUMN_IMAGE_URL = c.getColumnIndex(Contribution.Table.COLUMN_IMAGE_URL);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return getLayoutInflater().inflate(R.layout.layout_contribution, viewGroup, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ImageView imageView = (ImageView)view.findViewById(R.id.contributionImage);
            TextView titleView = (TextView)view.findViewById(R.id.contributionTitle);
            TextView stateView = (TextView)view.findViewById(R.id.contributionState);

            String localUri = cursor.getString(COLUMN_LOCALURI);
            String imageUrl = cursor.getString(COLUMN_IMAGE_URL);
            String title = cursor.getString(COLUMN_FILENAME);
            int state = cursor.getInt(COLUMN_STATE);

            String actualUrl = TextUtils.isEmpty(imageUrl) ? localUri : Contribution.makeThumbUrl(imageUrl, title, 320);
            Log.d("Commons", "Trying URL " + actualUrl);

            if(imageView.getTag() == null || !imageView.getTag().equals(actualUrl)) {
                ImageLoader.getInstance().displayImage(actualUrl, imageView, contributionDisplayOptions);
                imageView.setTag(actualUrl);
            }

            titleView.setText(Utils.displayTitleFromTitle(title));
            switch(state) {
                case Contribution.STATE_COMPLETED:
                    Date uploaded = new Date(cursor.getLong(COLUMN_UPLOADED));
                    stateView.setText(SimpleDateFormat.getDateInstance().format(uploaded));
                    break;
                case Contribution.STATE_QUEUED:
                    stateView.setText(R.string.contribution_state_queued);
                    break;
                case Contribution.STATE_IN_PROGRESS:
                    stateView.setText(R.string.contribution_state_starting);
                    long total = cursor.getLong(COLUMN_LENGTH);
                    long transferred = cursor.getLong(COLUMN_TRANSFERRED);
                    String stateString = String.format(getString(R.string.contribution_state_in_progress), (int)(((double)transferred / (double)total) * 100));
                    stateView.setText(stateString);
                    break;
                case Contribution.STATE_FAILED:
                    stateView.setText(R.string.contribution_state_failed);
                    break;
            }

        }
    }
    private GridView contributionsList;

    private ContributionAdapter contributionsAdapter;

    private DisplayImageOptions contributionDisplayOptions;

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
        ContentResolver.requestSync(((CommonsApplication)getApplicationContext()).getCurrentAccount(), ContributionsContentProvider.AUTHORITY, new Bundle());
        contributionDisplayOptions = new DisplayImageOptions.Builder().cacheInMemory()
                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
                .displayer(new FadeInBitmapDisplayer(300))
                .cacheInMemory()
                .cacheOnDisc()
                .resetViewBeforeLoading().build();

        bindService(new Intent(this, UploadService.class), uploadServiceConnection, Context.BIND_AUTO_CREATE);

        Cursor allContributions = getContentResolver().query(ContributionsContentProvider.BASE_URI, Contribution.Table.ALL_FIELDS, CONTRIBUTION_SELECTION, null, CONTRIBUTION_SORT);
        contributionsAdapter = new ContributionAdapter(this, allContributions, 0);
        contributionsList.setAdapter(contributionsAdapter);

        getSupportLoaderManager().initLoader(0, null, this);

        contributionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long item) {
                Cursor cursor = (Cursor)adapterView.getItemAtPosition(position);
                Contribution c = Contribution.fromCursor(cursor);
                if(c.getState() == Contribution.STATE_FAILED) {
                    uploadService.queueContribution(c);
                    Log.d("Commons", "Restarting for" + c.toContentValues().toString());
                }
                Log.d("Commons", "You clicked on:" + c.toContentValues().toString());
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title_activity_contributions);
        setContentView(R.layout.activity_contributions);
        contributionsList = (GridView)findViewById(R.id.contributionsList);

        requestAuthToken();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("lastGeneratedCaptureURI", lastGeneratedCaptureURI);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        lastGeneratedCaptureURI = (Uri) savedInstanceState.getParcelable("lastGeneratedCaptureURI");
        super.onRestoreInstanceState(savedInstanceState);
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
                }
                break;
            case SELECT_FROM_CAMERA:
                if(resultCode == RESULT_OK) {
                    Intent shareIntent = new Intent(this, ShareActivity.class);
                    shareIntent.setAction(Intent.ACTION_SEND);
                    Log.d("Commons", "Uri is " + lastGeneratedCaptureURI);
                    shareIntent.setType("image/jpeg"); //FIXME: Find out appropriate mime type
                    shareIntent.putExtra(Intent.EXTRA_STREAM, lastGeneratedCaptureURI);
                    startActivity(shareIntent);
                }
                break;
        }
    }

    // See http://stackoverflow.com/a/5054673/17865 for why this is done
    private Uri lastGeneratedCaptureURI;

    @Override
    protected void onAuthFailure() {
        super.onAuthFailure();
        finish(); // If authentication failed, we just exit
    }

    private void reGenerateImageCaptureURI() {
        String storageState = Environment.getExternalStorageState();
        if(storageState.equals(Environment.MEDIA_MOUNTED)) {

            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Commons/images/" + new Date().getTime() + ".jpg";
            File _photoFile = new File(path);
            try {
                if(_photoFile.exists() == false) {
                    _photoFile.getParentFile().mkdirs();
                    _photoFile.createNewFile();
                }

            } catch (IOException e) {
                Log.e("Commons", "Could not create file: " + path, e);
            }

            lastGeneratedCaptureURI = Uri.fromFile(_photoFile);
        }   else {
            throw new RuntimeException("No external storage found!");
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
            case R.id.menu_from_camera:
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                reGenerateImageCaptureURI();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, lastGeneratedCaptureURI);
                startActivityForResult(takePictureIntent, SELECT_FROM_CAMERA);
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
        return new CursorLoader(this, ContributionsContentProvider.BASE_URI, Contribution.Table.ALL_FIELDS, CONTRIBUTION_SELECTION, null, CONTRIBUTION_SORT);
    }

    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        contributionsAdapter.swapCursor(cursor);
    }

    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        contributionsAdapter.swapCursor(null);
    }

}
