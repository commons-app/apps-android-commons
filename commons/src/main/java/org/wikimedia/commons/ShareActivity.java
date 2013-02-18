package org.wikimedia.commons;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import org.wikimedia.commons.auth.AuthenticatedActivity;
import org.wikimedia.commons.auth.WikiAccountAuthenticator;

import android.net.Uri;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.widget.ImageView;
import android.support.v4.app.NavUtils;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import android.widget.*;
import android.view.*;
import org.wikimedia.commons.contributions.Contribution;

import java.io.IOException;
import java.util.Date;


public class ShareActivity extends AuthenticatedActivity {

    public ShareActivity() {
        super(WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE);
    }

    private CommonsApplication app;
   
    private ImageView backgroundImageView;
    private Button uploadButton;
    private EditText titleEdit;
    private EditText descEdit;

    private String source;
    private String mimeType;

    private Uri mediaUri;

    private UploadService uploadService;
    private ServiceConnection uploadServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            uploadService = (UploadService) ((HandlerService.HandlerServiceLocalBinder)binder).getService();
        }

        public void onServiceDisconnected(ComponentName componentName) {
            // this should never happen
            throw new RuntimeException("UploadService died but the rest of the process did not!");
        }
    };


    private class StartUploadTask extends AsyncTask<Void, Void, Contribution> {

        @Override
        protected void onPreExecute() {
            Toast startingToast = Toast.makeText(getApplicationContext(), R.string.uploading_started, Toast.LENGTH_LONG);
            startingToast.show();
        }

        @Override
        protected Contribution doInBackground(Void... voids) {
            String title = titleEdit.getText().toString();
            String description = descEdit.getText().toString();

            Date dateCreated = null;

            Long length = null;
            try {
                length = getContentResolver().openAssetFileDescriptor(mediaUri, "r").getLength();
                if(length == -1) {
                    // Let us find out the long way!
                    length = Utils.countBytes(getContentResolver().openInputStream(mediaUri));
                }
            } catch(IOException e) {
                throw new RuntimeException(e);
            }


            if(mimeType.startsWith("image/")) {
                Cursor cursor = getContentResolver().query(mediaUri,
                        new String[]{MediaStore.Images.ImageColumns.DATE_TAKEN}, null, null, null);
                if(cursor != null && cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    dateCreated = new Date(cursor.getLong(0));
                } // FIXME: Alternate way of setting dateCreated if this data is not found
            } /* else if (mimeType.startsWith("audio/")) {
             Removed Audio implementationf or now
           }  */
            Contribution contribution = new Contribution(mediaUri, null, title, description, length, dateCreated, null, app.getCurrentAccount().name, CommonsApplication.DEFAULT_EDIT_SUMMARY);
            contribution.setSource(source);
            return contribution;
        }

        @Override
        protected void onPostExecute(Contribution contribution) {
            uploadService.queue(UploadService.ACTION_UPLOAD_FILE, contribution);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        EventLog.schema(CommonsApplication.EVENT_UPLOAD_ATTEMPT)
                .param("username", app.getCurrentAccount().name)
                .param("source", getIntent().getStringExtra(UploadService.EXTRA_SOURCE))
                .param("result", "cancelled")
                .log();
    }

    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        super.onAuthCookieAcquired(authCookie);
        app.getApi().setAuthCookie(authCookie);
        Intent intent = getIntent();
      
        final Context that = this;
        
        if(intent.getAction().equals(Intent.ACTION_SEND)) {
            mediaUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if(intent.hasExtra(UploadService.EXTRA_SOURCE)) {
                source = intent.getStringExtra(UploadService.EXTRA_SOURCE);
            } else {
                source = Contribution.SOURCE_EXTERNAL;
            }
            
            mimeType = intent.getType();
            if(mimeType.startsWith("image/")) {
                ImageLoaderTask loader = new ImageLoaderTask(backgroundImageView);
                loader.execute(mediaUri);
            }

            Intent uploadServiceIntent = new Intent(getApplicationContext(), UploadService.class);
            uploadServiceIntent.setAction(UploadService.ACTION_START_SERVICE);
            startService(uploadServiceIntent);
            bindService(uploadServiceIntent, uploadServiceConnection, Context.BIND_AUTO_CREATE);
                
            uploadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StartUploadTask task = new StartUploadTask();
                    task.execute();
                }
            });
        }
    }
    
    

    @Override
    protected void onAuthFailure() {
        super.onAuthFailure();
        Toast failureToast = Toast.makeText(this, R.string.authentication_failed, Toast.LENGTH_LONG);
        failureToast.show();
        finish();
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Actionbar overlay on top of imageview (should be called before .setcontentview)
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        
        setContentView(R.layout.activity_share);
        
        app = (CommonsApplication)this.getApplicationContext();
        
        backgroundImageView = (ImageView)findViewById(R.id.backgroundImage);
        titleEdit = (EditText)findViewById(R.id.titleEdit);
        descEdit = (EditText)findViewById(R.id.descEdit);
        uploadButton = (Button)findViewById(R.id.uploadButton);
        
        requestAuthToken();
    
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_share, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
