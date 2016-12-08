package fr.free.nrw.commons.upload;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.Date;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.HandlerService;
import fr.free.nrw.commons.Prefs;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.contributions.Contribution;

public class UploadController {
    private UploadService uploadService;

    private final Activity activity;
    final CommonsApplication app;

    public interface ContributionUploadProgress {
        void onUploadStarted(Contribution contribution);
    }

    public UploadController(Activity activity) {
        this.activity = activity;
        app = (CommonsApplication)activity.getApplicationContext();
    }

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

    public void prepareService() {
        Intent uploadServiceIntent = new Intent(activity.getApplicationContext(), UploadService.class);
        uploadServiceIntent.setAction(UploadService.ACTION_START_SERVICE);
        activity.startService(uploadServiceIntent);
        activity.bindService(uploadServiceIntent, uploadServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void cleanup() {
        if(isUploadServiceConnected) {
            activity.unbindService(uploadServiceConnection);
        }
    }

    public void startUpload(String title, Uri mediaUri, String description, String mimeType, String source, String decimalCoords, ContributionUploadProgress onComplete) {
        Contribution contribution;

        //TODO: Modify this to include coords
        contribution = new Contribution(mediaUri, null, title, description, -1, null, null, app.getCurrentAccount().name, CommonsApplication.DEFAULT_EDIT_SUMMARY, decimalCoords);

        contribution.setTag("mimeType", mimeType);
        contribution.setSource(source);

        //Calls the next overloaded method
        startUpload(contribution, onComplete);
    }

    public void startUpload(final Contribution contribution, final ContributionUploadProgress onComplete) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);

        //Set creator, desc, and license
        if(TextUtils.isEmpty(contribution.getCreator())) {
            contribution.setCreator(app.getCurrentAccount().name);
        }

        if(contribution.getDescription() == null) {
            contribution.setDescription("");
        }

        String license = prefs.getString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA);
        contribution.setLicense(license);

        //FIXME: Add permission request here. Only executeAsyncTask if permission has been granted
        Utils.executeAsyncTask(new AsyncTask<Void, Void, Contribution>() {

            // Fills up missing information about Contributions
            // Only does things that involve some form of IO
            // Runs in background thread
            @Override
            protected Contribution doInBackground(Void... voids /* stare into you */) {
                long length;
                try {
                    if(contribution.getDataLength() <= 0) {
                        length = activity.getContentResolver().openAssetFileDescriptor(contribution.getLocalUri(), "r").getLength();
                        if(length == -1) {
                            // Let us find out the long way!
                            length = Utils.countBytes(activity.getContentResolver().openInputStream(contribution.getLocalUri()));
                        }
                        contribution.setDataLength(length);
                    }
                } catch(IOException e) {
                    Log.e("UploadController", "IO Exception: ", e);
                } catch(NullPointerException e) {
                    Log.e("UploadController", "Null Pointer Exception: ", e);
                } catch(SecurityException e) {
                    Log.e("UploadController", "Security Exception: ", e);
                }

                String mimeType = (String)contribution.getTag("mimeType");
                Boolean imagePrefix = false;

                if(mimeType == null || TextUtils.isEmpty(mimeType) || mimeType.endsWith("*")) {
                    mimeType = activity.getContentResolver().getType(contribution.getLocalUri());
                }

                if(mimeType != null) {
                    contribution.setTag("mimeType", mimeType);
                    imagePrefix = mimeType.startsWith("image/");
                    Log.d("UploadController", "MimeType is: " + mimeType);
                }

                if(imagePrefix && contribution.getDateCreated() == null) {
                    Cursor cursor = activity.getContentResolver().query(contribution.getLocalUri(),
                            new String[]{MediaStore.Images.ImageColumns.DATE_TAKEN}, null, null, null);
                    if(cursor != null && cursor.getCount() != 0) {
                        cursor.moveToFirst();
                        Date dateCreated = new Date(cursor.getLong(0));
                        Date epochStart = new Date(0);
                        if(dateCreated.equals(epochStart) || dateCreated.before(epochStart)) {
                            // If date is incorrect (1st second of unix time) then set it to the current date
                            dateCreated = new Date();
                        }
                        contribution.setDateCreated(dateCreated);
                        cursor.close();
                    } else {
                        contribution.setDateCreated(new Date());
                    }
                }
                return contribution;
            }

            @Override
            protected void onPostExecute(Contribution contribution) {
                super.onPostExecute(contribution);
                uploadService.queue(UploadService.ACTION_UPLOAD_FILE, contribution);
                onComplete.onUploadStarted(contribution);
            }
        });
    }
}
