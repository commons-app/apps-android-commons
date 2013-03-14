package org.wikimedia.commons;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import org.wikimedia.commons.CommonsApplication;
import org.wikimedia.commons.R;
import org.wikimedia.commons.UploadService;
import org.wikimedia.commons.Utils;
import org.wikimedia.commons.contributions.Contribution;

import java.io.IOException;
import java.util.Date;

public class StartUploadTask extends AsyncTask<Void, Void, Contribution> {

    private Activity context;
    private String rawTitle;
    private Uri mediaUri;
    private String description;
    private String mimeType;
    private String source;
    private UploadService uploadService;
    private CommonsApplication app;

    public StartUploadTask(Activity context, UploadService uploadService, String rawTitle, Uri mediaUri, String description, String mimeType, String source) {
        this.context = context;
        this.rawTitle = rawTitle;
        this.mediaUri = mediaUri;
        this.description = description;
        this.mimeType = mimeType;
        this.source = source;
        this.uploadService = uploadService;

        app = (CommonsApplication)context.getApplicationContext();
    }

    @Override
    protected Contribution doInBackground(Void... voids) {
        String title = rawTitle;

        Date dateCreated = null;

        Long length = null;
        try {
            length = context.getContentResolver().openAssetFileDescriptor(mediaUri, "r").getLength();
            if(length == -1) {
                // Let us find out the long way!
                length = Utils.countBytes(context.getContentResolver().openInputStream(mediaUri));
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

        if(extension != null && !title.toLowerCase().endsWith(extension.toLowerCase())) {
            title += "." + extension;
        }

        Log.d("Commons", "Title is " + title + " mimetype is " + mimeType);

        if(mimeType.startsWith("image/")) {
            Cursor cursor = context.getContentResolver().query(mediaUri,
                    new String[]{MediaStore.Images.ImageColumns.DATE_TAKEN}, null, null, null);
            if(cursor != null && cursor.getCount() != 0) {
                cursor.moveToFirst();
                dateCreated = new Date(cursor.getLong(0));
            } // FIXME: Alternate way of setting dateCreated if this data is not found
        }
        Contribution contribution = new Contribution(mediaUri, null, title, description, length, dateCreated, null, app.getCurrentAccount().name, CommonsApplication.DEFAULT_EDIT_SUMMARY);
        contribution.setSource(source);
        return contribution;
    }

    @Override
    protected void onPostExecute(Contribution contribution) {
        uploadService.queue(UploadService.ACTION_UPLOAD_FILE, contribution);
    }
}