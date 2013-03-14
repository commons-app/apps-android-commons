package org.wikimedia.commons;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.wikimedia.commons.*;
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


    private class SingleStartUploadTask extends StartUploadTask {

        private SingleStartUploadTask(Activity context, UploadService uploadService, String rawTitle, Uri mediaUri, String description, String mimeType, String source) {
            super(context, uploadService, rawTitle, mediaUri, description, mimeType, source);
        }

        @Override
        protected void onPreExecute() {
            Toast startingToast = Toast.makeText(getApplicationContext(), R.string.uploading_started, Toast.LENGTH_LONG);
            startingToast.show();
        }

        @Override
        protected void onPostExecute(Contribution contribution) {
            super.onPostExecute(contribution);
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
                ImageLoader.getInstance().displayImage(mediaUri.toString(), backgroundImageView);
            }

            Intent uploadServiceIntent = new Intent(getApplicationContext(), UploadService.class);
            uploadServiceIntent.setAction(UploadService.ACTION_START_SERVICE);
            startService(uploadServiceIntent);
            bindService(uploadServiceIntent, uploadServiceConnection, Context.BIND_AUTO_CREATE);
                
            uploadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StartUploadTask task = new SingleStartUploadTask(ShareActivity.this, uploadService, titleEdit.getText().toString(), mediaUri, descEdit.getText().toString(), mimeType,  source);
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

        setContentView(R.layout.activity_share);
        
        app = (CommonsApplication)this.getApplicationContext();
        
        backgroundImageView = (ImageView)findViewById(R.id.backgroundImage);
        titleEdit = (EditText)findViewById(R.id.titleEdit);
        descEdit = (EditText)findViewById(R.id.descEdit);
        uploadButton = (Button)findViewById(R.id.uploadButton);

        TextWatcher uploadEnabler = new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) { }

            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            public void afterTextChanged(Editable editable) {
                if(titleEdit.getText().length() != 0) {
                    uploadButton.setEnabled(true);
                } else {
                    uploadButton.setEnabled(false);
                }

            }
        };

        titleEdit.addTextChangedListener(uploadEnabler);
        
        requestAuthToken();
    
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isUploadServiceConnected) {
            unbindService(uploadServiceConnection);
        }
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
