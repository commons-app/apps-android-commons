package org.wikimedia.commons;

import java.io.*;

import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;
import org.wikimedia.commons.auth.AuthenticatedActivity;
import org.wikimedia.commons.auth.LoginActivity;
import org.wikimedia.commons.auth.WikiAccountAuthenticator;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.app.*;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.support.v4.app.NavUtils;

public class ShareActivity extends AuthenticatedActivity {

    public ShareActivity() {
        super(WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE);
    }

    private CommonsApplication app;
   
    private ImageView backgroundImageView;
    private Button uploadButton;
    private EditText titleEdit;
    private EditText descEdit;
    
    private Uri imageUri;
    
    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        super.onAuthCookieAcquired(authCookie);
        app.getApi().setAuthCookie(authCookie);
        Intent intent = getIntent();
      
        final Context that = this;
        
        if(intent.getAction().equals(Intent.ACTION_SEND)) {
            if(intent.getType().startsWith("image/")) {
                ImageLoaderTask loader = new ImageLoaderTask(backgroundImageView);
                imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                loader.execute(imageUri);
                
                uploadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent uploadIntent = new Intent(getApplicationContext(), UploadService.class);
                        uploadIntent.putExtra(UploadService.EXTRA_MEDIA_URI, imageUri);
                        uploadIntent.putExtra(UploadService.EXTRA_TARGET_FILENAME, titleEdit.getText().toString());
                        uploadIntent.putExtra(UploadService.EXTRA_DESCRIPTION, descEdit.getText().toString());
                        uploadIntent.putExtra(UploadService.EXTRA_EDIT_SUMMARY, "Mobile upload from Wikimedia Commons Android app");
                        startService(uploadIntent);
                        Toast startingToast = Toast.makeText(that, R.string.uploading_started, Toast.LENGTH_LONG);
                        startingToast.show(); 
                        finish();
                    }
                });
            }
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
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getActionBar().setDisplayShowTitleEnabled(false);
        
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
        getMenuInflater().inflate(R.menu.activity_share, menu);
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
