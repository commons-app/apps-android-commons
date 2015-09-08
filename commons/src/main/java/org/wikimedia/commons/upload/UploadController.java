package fr.nrw.free.commons.upload;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import fr.nrw.free.commons.CommonsApplication;
import fr.nrw.free.commons.HandlerService;
import fr.nrw.free.commons.Prefs;
import fr.nrw.free.commons.Utils;
import fr.nrw.free.commons.campaigns.Campaign;
import fr.nrw.free.commons.campaigns.CampaignContribution;
import fr.nrw.free.commons.contributions.Contribution;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class UploadController {
    private UploadService uploadService;

    private final Activity activity;
    private Campaign campaign;
    final CommonsApplication app;

    public interface ContributionUploadProgress {
        void onUploadStarted(Contribution contribution);
    }

    public UploadController(Activity activity) {
        this.activity = activity;
        app = (CommonsApplication)activity.getApplicationContext();
    }

    public UploadController(Activity activity, Campaign campaign) {
        this(activity);
        this.campaign = campaign;
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

    public void startUpload(String rawTitle, Uri mediaUri, String description, String mimeType, String source, ContributionUploadProgress onComplete) {
        Contribution contribution;

        String title = rawTitle;
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        // People are used to ".jpg" more than ".jpeg" which the system gives us.
        if (extension != null && extension.toLowerCase().equals("jpeg")) {
            extension = "jpg";
        }
        if(extension != null && !title.toLowerCase().endsWith(extension.toLowerCase())) {
            title += "." + extension;
        }

        if(campaign == null) {
            contribution = new Contribution(mediaUri, null, title, description, -1, null, null, app.getCurrentAccount().name, CommonsApplication.DEFAULT_EDIT_SUMMARY);
        } else {
            contribution = new CampaignContribution(mediaUri, null, title, description, -1, null, null, app.getCurrentAccount().name, CommonsApplication.DEFAULT_EDIT_SUMMARY, campaign);
        }
        contribution.setTag("mimeType", mimeType);
        contribution.setSource(source);

        startUpload(contribution, onComplete);
    }

    public void startUpload(final Contribution contribution, final ContributionUploadProgress onComplete) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);

        if(TextUtils.isEmpty(contribution.getCreator())) {
            contribution.setCreator(app.getCurrentAccount().name);
        }

        if(contribution.getDescription() == null) {
            contribution.setDescription("");
        }

        String license = prefs.getString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA);
        contribution.setLicense(license);

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
                    throw new RuntimeException(e);
                }

                String mimeType = (String)contribution.getTag("mimeType");
                if(mimeType == null || TextUtils.isEmpty(mimeType) || mimeType.endsWith("*")) {
                    mimeType = activity.getContentResolver().getType(contribution.getLocalUri());
                    if(mimeType != null) {
                        contribution.setTag("mimeType", mimeType);
                    }
                }

                if(mimeType.startsWith("image/") && contribution.getDateCreated() == null) {
                    Cursor cursor = activity.getContentResolver().query(contribution.getLocalUri(),
                            new String[]{MediaStore.Images.ImageColumns.DATE_TAKEN}, null, null, null);
                    if(cursor != null && cursor.getCount() != 0) {
                        cursor.moveToFirst();
                        contribution.setDateCreated(new Date(cursor.getLong(0)));
                    } // FIXME: Alternate way of setting dateCreated if this data is not found
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
