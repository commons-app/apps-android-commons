package org.wikimedia.commons;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.*;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import org.wikimedia.commons.auth.AuthenticatedActivity;
import org.wikimedia.commons.auth.WikiAccountAuthenticator;
import org.wikimedia.commons.contributions.Contribution;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class MultipleShareActivity extends AuthenticatedActivity {
    private CommonsApplication app;
    private ArrayList<Contribution> photosList = new ArrayList<Contribution>();

    private MultipleUploadListFragment uploadsList;

    public MultipleShareActivity() {
        super(WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE);
    }

    private class StartMultipleUploadTask extends AsyncTask<Void, Integer, Void> {

        ProgressDialog dialog;

        @Override
        protected Void doInBackground(Void... voids) {
            for(int i = 0; i < photosList.size(); i++) {
                Contribution up = photosList.get(i);
                String curMimetype = (String)up.getTag("mimeType");
                if(curMimetype == null || TextUtils.isEmpty(curMimetype) || curMimetype.endsWith("*")) {
                    String mimeType = getContentResolver().getType(up.getLocalUri());
                    if(mimeType != null) {
                        up.setTag("mimeType", mimeType);
                    }
                }

                StartUploadTask startUploadTask = new StartUploadTask(MultipleShareActivity.this, uploadService,  up.getFilename(),  up.getLocalUri(),  up.getDescription(), (String)up.getTag("mimeType"), Contribution.SOURCE_EXTERNAL);
                try {
                    Utils.executeAsyncTask(startUploadTask);
                    startUploadTask.get();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
                this.publishProgress(i);

            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(MultipleShareActivity.this);
            dialog.setIndeterminate(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMax(photosList.size());
            dialog.setTitle(getResources().getQuantityString(R.plurals.starting_multiple_uploads, photosList.size(), photosList.size()));
            dialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            dialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            dialog.dismiss();
            Toast startingToast = Toast.makeText(getApplicationContext(), R.string.uploading_started, Toast.LENGTH_LONG);
            startingToast.show();
            finish();
        }
    }

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_multiple_share, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_upload_multiple:

                StartMultipleUploadTask startUploads = new StartMultipleUploadTask();
                Utils.executeAsyncTask(startUploads);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_multiple_uploads);
        uploadsList = (MultipleUploadListFragment)getSupportFragmentManager().findFragmentById(R.id.uploadsListFragment);
        app = (CommonsApplication)this.getApplicationContext();
        requestAuthToken();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isUploadServiceConnected) {
            unbindService(uploadServiceConnection);
        }
    }

    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        app.getApi().setAuthCookie(authCookie);
        Intent intent = getIntent();

        if(intent.getAction() == Intent.ACTION_SEND_MULTIPLE) {
            ArrayList<Uri> urisList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            for(Uri uri: urisList) {
                Contribution up = new Contribution();
                up.setLocalUri(uri);
                up.setTag("mimeType", intent.getType());
                photosList.add(up);
            }

            uploadsList.setData(photosList);

            setTitle(getResources().getQuantityString(R.plurals.multiple_uploads_title, urisList.size(), urisList.size()));

            Intent uploadServiceIntent = new Intent(getApplicationContext(), UploadService.class);
            uploadServiceIntent.setAction(UploadService.ACTION_START_SERVICE);
            startService(uploadServiceIntent);
            bindService(uploadServiceIntent, uploadServiceConnection, Context.BIND_AUTO_CREATE);


        }

    }


    @Override
    protected void onAuthFailure() {
        super.onAuthFailure();
        Toast failureToast = Toast.makeText(this, R.string.authentication_failed, Toast.LENGTH_LONG);
        failureToast.show();
        finish();
    }

}