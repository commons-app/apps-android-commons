package fr.free.nrw.commons.upload;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.utils.ViewUtil;
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

    /**
     * Starts a new upload task.
     *
     * @param contribution the contribution object
     */
    @SuppressLint("StaticFieldLeak")
    public void prepareMedia(final Contribution contribution) {
        //Set creator, desc, and license

        // If author name is enabled and set, use it
        final Media media = contribution.getMedia();
        if (store.getBoolean("useAuthorName", false)) {
            final String authorName = store.getString("authorName", "");
            media.setAuthor(authorName);
        }

        if (TextUtils.isEmpty(media.getAuthor())) {
            final Account currentAccount = sessionManager.getCurrentAccount();
            if (currentAccount == null) {
                Timber.d("Current account is null");
                ViewUtil.showLongToast(context, context.getString(R.string.user_not_logged_in));
                sessionManager.forceLogin(context);
                return;
            }
            media.setAuthor(sessionManager.getUserName());
        }

        if (media.getFallbackDescription() == null) {
            media.setFallbackDescription("");
        }

        final String license = store.getString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA_3);
        media.setLicense(license);

        buildUpload(contribution);
    }

    /**
     * Make the Contribution object ready to be uploaded
     * @param contribution
     * @return
     */
    private void buildUpload(final Contribution contribution) {
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
    }

    private String resolveMimeType(final ContentResolver contentResolver, final Contribution contribution) {
        final String mimeType = contribution.getMimeType();
        if (mimeType == null || TextUtils.isEmpty(mimeType) || mimeType.endsWith("*")) {
            return contentResolver.getType(contribution.getLocalUri());
        }
        return mimeType;
    }

    private long resolveDataLength(final ContentResolver contentResolver, final Contribution contribution) {
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

    private Date resolveDateTakenOrNow(final ContentResolver contentResolver, final Contribution contribution) {
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

    private Cursor dateTakenCursor(final ContentResolver contentResolver, final Contribution contribution) {
        return contentResolver.query(contribution.getLocalUri(),
            new String[]{MediaStore.Images.ImageColumns.DATE_TAKEN}, null, null, null);
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
