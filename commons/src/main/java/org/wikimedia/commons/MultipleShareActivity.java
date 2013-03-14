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
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import de.akquinet.android.androlog.Log;
import org.wikimedia.commons.auth.AuthenticatedActivity;
import org.wikimedia.commons.auth.WikiAccountAuthenticator;
import org.wikimedia.commons.contributions.Contribution;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class MultipleShareActivity extends AuthenticatedActivity {
    private CommonsApplication app;
    private GridView photosGrid;
    private PhotoDisplayAdapter photosAdapter;
    private ArrayList<PreparedUpload> photosList = new ArrayList<PreparedUpload>();
    private EditText baseTitle;
    private int picHeight;
    private DisplayImageOptions uploadDisplayOptions;

    public MultipleShareActivity() {
        super(WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE);
    }

    private static class UploadHolderView {
        Uri imageUri;

        ImageView image;
        TextView title;
    }

    private static class PreparedUpload {
        Uri uri;
        String title;
        boolean isDirty;
        int sequence;
        String mimeType;

        private PreparedUpload(Uri uri, int seq) {
            this.uri = uri;
            this.sequence = seq;
        }
    }
    
    private class PhotoDisplayAdapter extends BaseAdapter {

        private ArrayList<PreparedUpload> urisList;

        private PhotoDisplayAdapter(ArrayList<PreparedUpload> urisList) {
            this.urisList = urisList;
        }

        public int getCount() {
            return urisList.size();
        }

        public Object getItem(int i) {
            return urisList.get(i);
        }

        public long getItemId(int i) {
            return i;
        }

        public View getView(int i, View view, ViewGroup viewGroup) {
            UploadHolderView holder;

            if(view == null) {
                view = getLayoutInflater().inflate(R.layout.layout_upload_item, null);
                holder = new UploadHolderView();
                holder.image = (ImageView) view.findViewById(R.id.uploadImage);
                holder.title = (TextView) view.findViewById(R.id.uploadTitle);

                holder.image.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, picHeight));

                view.setTag(holder);
            } else {
                holder = (UploadHolderView)view.getTag();
            }


            PreparedUpload up = (PreparedUpload)this.getItem(i);

            if(holder.imageUri == null || !holder.imageUri.equals(up.uri)) {
                ImageLoader.getInstance().displayImage(up.uri.toString(), holder.image, uploadDisplayOptions);
                holder.imageUri = up.uri;
            }

            holder.title.setText(up.title);

            return view;

        }
    }

    private class StartMultipleUploadTask extends AsyncTask<Void, Integer, Void> {

        ProgressDialog dialog;

        @Override
        protected Void doInBackground(Void... voids) {
            for(int i = 0; i < photosList.size(); i++) {
                PreparedUpload up = photosList.get(i);
                if(up.mimeType == null || TextUtils.isEmpty(up.mimeType) || up.mimeType.endsWith("*")) {
                    String mimeType = getContentResolver().getType(up.uri);
                    if(mimeType != null) {
                        up.mimeType = mimeType;
                    }
                }

                StartUploadTask startUploadTask = new StartUploadTask(MultipleShareActivity.this, uploadService,  up.title,  up.uri,  "", up.mimeType, Contribution.SOURCE_EXTERNAL);
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

        setContentView(R.layout.activity_multiple_share);

        app = (CommonsApplication)this.getApplicationContext();
        photosGrid = (GridView)findViewById(R.id.multipleShareBackground);
        baseTitle = (EditText)findViewById(R.id.multipleBaseTitle);

        uploadDisplayOptions = new DisplayImageOptions.Builder().cacheInMemory()
                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
                .displayer(new FadeInBitmapDisplayer(300))
                .cacheInMemory()
                .resetViewBeforeLoading().build();

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
            for(int i=0; i < urisList.size(); i++) {
                PreparedUpload up = new PreparedUpload(urisList.get(i), i + 1);
                up.mimeType = intent.getType();
                photosList.add(up);
            }
            DisplayMetrics screenMetrics = getResources().getDisplayMetrics();
            int screenWidth = screenMetrics.widthPixels;
            int screenHeight = screenMetrics.heightPixels;

            int picWidth = Math.min((int) Math.sqrt(screenWidth * screenHeight / urisList.size()), screenWidth);
            picWidth = Math.min((int)(192 * screenMetrics.density), Math.max((int) (120  * screenMetrics.density), picWidth / 48 * 48));
            picHeight = Math.min(picWidth, (int)(192 * screenMetrics.density)); // Max Height is same as Contributions list

            photosGrid.setColumnWidth(picWidth);
            photosGrid.invalidateViews();

            photosAdapter = new PhotoDisplayAdapter(photosList);
            photosGrid.setAdapter(photosAdapter);

            Intent uploadServiceIntent = new Intent(getApplicationContext(), UploadService.class);
            uploadServiceIntent.setAction(UploadService.ACTION_START_SERVICE);
            startService(uploadServiceIntent);
            bindService(uploadServiceIntent, uploadServiceConnection, Context.BIND_AUTO_CREATE);

            baseTitle.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                }

                public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                    for(PreparedUpload up: photosList) {
                        if(!up.isDirty) {
                            if(!TextUtils.isEmpty(charSequence)) {
                                up.title = charSequence.toString() + " - " + up.sequence;
                            } else {
                                up.title = "";
                            }
                        }
                    }
                    photosAdapter.notifyDataSetChanged();

                }

                public void afterTextChanged(Editable editable) {

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

}