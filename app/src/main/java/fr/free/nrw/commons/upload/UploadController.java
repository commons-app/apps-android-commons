package fr.free.nrw.commons.upload;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.TextUtils;
import fr.free.nrw.commons.HandlerService;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton
public class UploadController {
    private UploadService uploadService;
    private final SessionManager sessionManager;
    private final Context context;
    private final JsonKvStore store;

    @Inject
    public UploadController(final SessionManager sessionManager,
                            final Context context,
                            final JsonKvStore store) {
        this.sessionManager = sessionManager;
        this.context = context;
        this.store = store;
    }

    private boolean isUploadServiceConnected;
    public ServiceConnection uploadServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName componentName, final IBinder binder) {
            uploadService = (UploadService) ((HandlerService.HandlerServiceLocalBinder) binder).getService();
            isUploadServiceConnected = true;
        }

        @Override
        public void onServiceDisconnected(final ComponentName componentName) {
            // this should never happen
            isUploadServiceConnected = false;
            Timber.e(new RuntimeException("UploadService died but the rest of the process did not!"));
        }
    };

    /**
     * Prepares the upload service.
     */
    public void prepareService() {
        final Intent uploadServiceIntent = new Intent(context, UploadService.class);
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
     * @param contribution the contribution object
     */
    @SuppressLint("StaticFieldLeak")
    public void startUpload(final Contribution contribution) {
        //Set author, desc, and license

        // If author name is enabled and set, use it
        if (store.getBoolean("useAuthorName", false)) {
            final String authorName = store.getString("authorName", "");
            contribution.setAuthor(authorName);
        }

        if (TextUtils.isEmpty(contribution.getAuthor())) {
            final Account currentAccount = sessionManager.getCurrentAccount();
            if (currentAccount == null) {
                Timber.d("Current account is null");
                ViewUtil.showLongToast(context, context.getString(R.string.user_not_logged_in));
                sessionManager.forceLogin(context);
                return;
            }
            contribution.setAuthor(sessionManager.getUserName());
        }

        if (contribution.getDescription() == null) {
            contribution.setDescription("");
        }

        final String license = store.getString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA_3);
        contribution.setLicense(license);

        uploadTask(contribution);
    }

    /**
     * Initiates the upload task
     * @param contribution
     * @return
     */
    private Disposable uploadTask(final Contribution contribution) {
        return Single.just(contribution)
                .map(this::buildUpload)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::upload);
    }

    /**
     * Make the Contribution object ready to be uploaded
     * @param contribution
     * @return
     */
    private Contribution buildUpload(final Contribution contribution) {
        final ContentResolver contentResolver = context.getContentResolver();

        contribution.setDataLength(resolveDataLength(contentResolver, contribution));

        final String mimeType = resolveMimeType(contentResolver, contribution);

        if (mimeType != null) {
            Timber.d("MimeType is: %s", mimeType);
            contribution.setMimeType(mimeType);
            if(mimeType.startsWith("image/") && contribution.getDateCreated() == null){
                contribution.setDateCreated(resolveDateTakenOrNow(contentResolver, contribution));
            }
        }

        return contribution;
    }

    private String resolveMimeType(final ContentResolver contentResolver, final Contribution contribution) {
        final String mimeType = contribution.getMimeType();
        if (mimeType == null || TextUtils.isEmpty(mimeType) || mimeType.endsWith("*")) {
            return contentResolver.getType(contribution.getLocalUri());
        }
        return mimeType;
    }

    private long resolveDataLength(final ContentResolver contentResolver, final Media contribution) {
        try {
            if (contribution.getDataLength() <= 0) {
                Timber.d("UploadController/doInBackground, contribution.getLocalUri():%s", contribution.getLocalUri());
                final AssetFileDescriptor assetFileDescriptor = contentResolver
                    .openAssetFileDescriptor(Uri.fromFile(new File(contribution.getLocalUri().getPath())), "r");
                if (assetFileDescriptor != null) {
                    final long length = assetFileDescriptor.getLength();
                    return length != -1 ? length
                        : countBytes(contentResolver.openInputStream(contribution.getLocalUri()));
                }
            }
        } catch (final IOException | NullPointerException | SecurityException e) {
            Timber.e(e, "Exception occurred while uploading image");
        }
        return contribution.getDataLength();
    }

    private Date resolveDateTakenOrNow(final ContentResolver contentResolver, final Media contribution) {
        Timber.d("local uri   %s", contribution.getLocalUri());
        try(final Cursor cursor = dateTakenCursor(contentResolver, contribution)) {
            if (cursor != null && cursor.getCount() != 0 && cursor.getColumnCount() != 0) {
                cursor.moveToFirst();
                final Date dateCreated = new Date(cursor.getLong(0));
                if (dateCreated.after(new Date(0))) {
                    return dateCreated;
                }
            }
            return new Date();
        }
    }

    private Cursor dateTakenCursor(final ContentResolver contentResolver, final Media contribution) {
        return contentResolver.query(contribution.getLocalUri(),
            new String[]{MediaStore.Images.ImageColumns.DATE_TAKEN}, null, null, null);
    }

    /**
     * When the contribution object is completely formed, the item is queued to the upload service
     * @param contribution
     */
    private void upload(final Contribution contribution) {
        //Starts the upload. If commented out, user can proceed to next Fragment but upload doesn't happen
        uploadService.queue(UploadService.ACTION_UPLOAD_FILE, contribution);
    }


    /**
     * Counts the number of bytes in {@code stream}.
     *
     * @param stream the stream
     * @return the number of bytes in {@code stream}
     * @throws IOException if an I/O error occurs
     */
    private long countBytes(final InputStream stream) throws IOException {
        long count = 0;
        final BufferedInputStream bis = new BufferedInputStream(stream);
        while (bis.read() != -1) {
            count++;
        }
        return count;
    }
}
