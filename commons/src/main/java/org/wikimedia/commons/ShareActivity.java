package org.wikimedia.commons;

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


public class ShareActivity extends AuthenticatedActivity {

    public ShareActivity() {
        super(WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE);
    }

    private CommonsApplication app;
   
    private ImageView backgroundImageView;
    private Button uploadButton;
    private EditText titleEdit;
    private EditText descEdit;

    private Uri mediaUri;

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
            final String  source;
            if(intent.hasExtra(UploadService.EXTRA_SOURCE)) {
                source = intent.getStringExtra(UploadService.EXTRA_SOURCE);
            } else {
                source = Contribution.SOURCE_EXTERNAL;
            }
            
            final String mimeType = intent.getType();
            if(mimeType.startsWith("image/")) {
                ImageLoaderTask loader = new ImageLoaderTask(backgroundImageView);
                loader.execute(mediaUri);
            }
                
            uploadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent uploadIntent = new Intent(getApplicationContext(), UploadService.class);
                    uploadIntent.putExtra(UploadService.EXTRA_MEDIA_URI, mediaUri);
                    uploadIntent.putExtra(UploadService.EXTRA_TARGET_FILENAME, titleEdit.getText().toString());
                    uploadIntent.putExtra(UploadService.EXTRA_DESCRIPTION, descEdit.getText().toString());
                    uploadIntent.putExtra(UploadService.EXTRA_MIMETYPE, mimeType);
                    uploadIntent.putExtra(UploadService.EXTRA_EDIT_SUMMARY, "Mobile upload from Wikimedia Commons Android app");
                    uploadIntent.putExtra(UploadService.EXTRA_SOURCE, source);
                    startService(uploadIntent);
                    Toast startingToast = Toast.makeText(that, R.string.uploading_started, Toast.LENGTH_LONG);
                    startingToast.show(); 
                    finish();
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
