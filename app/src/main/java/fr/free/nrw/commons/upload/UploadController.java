package fr.free.nrw.commons.upload;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.Executors;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.HandlerService;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.settings.Prefs;
import timber.log.Timber;

public class UploadController {
    private UploadService uploadService;
    private SessionManager sessionManager;
    private Context context;
    private SharedPreferences prefs;

    public interface ContributionUploadProgress {
        void onUploadStarted(Contribution contribution);
    }

    /**
     * Constructs a new UploadController.
     */
    public UploadController(SessionManager sessionManager, Context context, SharedPreferences sharedPreferences) {
        this.sessionManager = sessionManager;
        this.context = context;
        this.prefs = sharedPreferences;
    }

    private boolean isUploadServiceConnected;
    private ServiceConnection uploadServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            uploadService = (UploadService) ((HandlerService.HandlerServiceLocalBinder) binder).getService();
            isUploadServiceConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            // this should never happen
            Timber.e(new RuntimeException("UploadService died but the rest of the process did not!"));
        }
    };

    /**
     * Prepares the upload service.
     */
    public void prepareService() {
        Intent uploadServiceIntent = new Intent(context, UploadService.class);
        uploadServiceIntent.setAction(UploadService.ACTION_START_SERVICE);
        context.startService(uploadServiceIntent);
        context.bindService(uploadServiceIntent, uploadServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Disconnects the upload service.
     */
    public void cleanup() {
        if (isUploadServiceConnected) {
            context.unbindService(uploadServiceConnection);
        }
    }

    /**
     * Starts a new upload task.
     *
     * @param title         the title of the contribution
     * @param mediaUri      the media URI of the contribution
     * @param description   the description of the contribution
     * @param mimeType      the MIME type of the contribution
     * @param source        the source of the contribution
     * @param decimalCoords the coordinates in decimal. (e.g. "37.51136|-77.602615")
     * @param onComplete    the progress tracker
     */
    public void startUpload(String title, Uri mediaUri, String description, String mimeType, String source, String decimalCoords, ContributionUploadProgress onComplete) {
        Contribution contribution;

        //TODO: Modify this to include coords
        contribution = new Contribution(mediaUri, null, title, description, -1,
                null, null, sessionManager.getCurrentAccount().name,
                CommonsApplication.DEFAULT_EDIT_SUMMARY, decimalCoords);

        contribution.setTag("mimeType", mimeType);
        contribution.setSource(source);

        //Calls the next overloaded method
        startUpload(contribution, onComplete);
    }

    public void startUpload(Contribution contribution) {
        startUpload(contribution, c -> {});
    }

    /**
     * Starts a new upload task.
     *
     * @param contribution the contribution object
     * @param onComplete   the progress tracker
     */
    public void startUpload(final Contribution contribution, final ContributionUploadProgress onComplete) {
        //Set creator, desc, and license
        if (TextUtils.isEmpty(contribution.getCreator())) {
            contribution.setCreator(sessionManager.getCurrentAccount().name);
        }

        if (contribution.getDescription() == null) {
            contribution.setDescription("");
        }

        String license = prefs.getString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA_3);
        contribution.setLicense(license);

        //FIXME: Add permission request here. Only executeAsyncTask if permission has been granted
        new AsyncTask<Void, Void, Contribution>() {

            // Fills up missing information about Contributions
            // Only does things that involve some form of IO
            // Runs in background thread
            @Override
            protected Contribution doInBackground(Void... voids /* stare into you */) {
                long length;
                ContentResolver contentResolver = context.getContentResolver();
                try {
                    if (contribution.getDataLength() <= 0) {
                        AssetFileDescriptor assetFileDescriptor = contentResolver
                                .openAssetFileDescriptor(contribution.getLocalUri(), "r");
                        if (assetFileDescriptor != null) {
                            length = assetFileDescriptor.getLength();
                            if (length == -1) {
                                // Let us find out the long way!
                                length = countBytes(contentResolver
                                        .openInputStream(contribution.getLocalUri()));
                            }
                            contribution.setDataLength(length);
                        }
                    }
                } catch (IOException e) {
                    Timber.e(e, "IO Exception: ");
                } catch (NullPointerException e) {
                    Timber.e(e, "Null Pointer Exception: ");
                } catch (SecurityException e) {
                    Timber.e(e, "Security Exception: ");
                }

                String mimeType = (String) contribution.getTag("mimeType");
                Boolean imagePrefix = false;

                if (mimeType == null || TextUtils.isEmpty(mimeType) || mimeType.endsWith("*")) {
                    mimeType = contentResolver.getType(contribution.getLocalUri());
                }

                if (mimeType != null) {
                    contribution.setTag("mimeType", mimeType);
                    imagePrefix = mimeType.startsWith("image/");
                    Timber.d("MimeType is: %s", mimeType);
                }

                if (imagePrefix && contribution.getDateCreated() == null) {
                    Timber.d("local uri   " + contribution.getLocalUri());
                    Cursor cursor = contentResolver.query(contribution.getLocalUri(),
                            new String[]{MediaStore.Images.ImageColumns.DATE_TAKEN}, null, null, null);
                    if (cursor != null && cursor.getCount() != 0 && cursor.getColumnCount() != 0) {
                        cursor.moveToFirst();
                        Date dateCreated = new Date(cursor.getLong(0));
                        Date epochStart = new Date(0);
                        if (dateCreated.equals(epochStart) || dateCreated.before(epochStart)) {
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
                //Starts the upload. If commented out, user can proceed to next Fragment but upload doesn't happen
                uploadService.queue(UploadService.ACTION_UPLOAD_FILE, contribution);
                onComplete.onUploadStarted(contribution);
            }
        }.executeOnExecutor(Executors.newFixedThreadPool(1)); // TODO remove this by using a sensible thread handling strategy
    }


    /**
     * Counts the number of bytes in {@code stream}.
     *
     * @param stream the stream
     * @return the number of bytes in {@code stream}
     * @throws IOException if an I/O error occurs
     */
    private long countBytes(InputStream stream) throws IOException {
        long count = 0;
        BufferedInputStream bis = new BufferedInputStream(stream);
        while (bis.read() != -1) {
            count++;
        }
        return count;
    }
}
