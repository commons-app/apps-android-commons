package org.wikimedia.commons;

import java.io.*;
import java.net.*;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import android.support.v4.app.NavUtils;

public class ShareActivity extends Activity {

    private CommonsApplication app;
   
    private ImageView backgroundImageView;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        app = (CommonsApplication)this.getApplicationContext();
        backgroundImageView = (ImageView)findViewById(R.id.backgroundImage);
        Intent intent = getIntent();
        if(intent.getAction().equals(Intent.ACTION_SEND)) {
            if(intent.getType().startsWith("image/")) {
                ImageLoaderTask loader = new ImageLoaderTask(backgroundImageView);
                Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                loader.execute(imageUri);
            }
        }
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
