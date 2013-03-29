package org.wikimedia.commons;

import android.app.*;
import android.database.Cursor;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.*;
import java.util.*;

import org.wikimedia.commons.contributions.*;

public class StartUploadTask extends AsyncTask<Void, Void, Contribution> {

    private Activity context;
    private UploadService uploadService;

    private Contribution contribution;

    private CommonsApplication app;

    public StartUploadTask(Activity context, UploadService uploadService, String rawTitle, Uri mediaUri, String description, String mimeType, String source) {

        this.context = context;
        this.uploadService = uploadService;

        app = (CommonsApplication)context.getApplicationContext();

        contribution = new Contribution(mediaUri, null, rawTitle, description, -1, null, null, app.getCurrentAccount().name, CommonsApplication.DEFAULT_EDIT_SUMMARY);
        contribution.setTag("mimeType", mimeType);
        contribution.setSource(source);
    }

    public StartUploadTask(Activity context, UploadService uploadService, Contribution contribution) {
        this.context = context;
        this.uploadService = uploadService;
        this.contribution = contribution;

        app = (CommonsApplication)context.getApplicationContext();
    }


    @Override
    protected Contribution doInBackground(Void... voids) {
        String title = contribution.getFilename();

        long length;
        try {
            if(contribution.getDataLength() <= 0) {
                length = context.getContentResolver().openAssetFileDescriptor(contribution.getLocalUri(), "r").getLength();
                if(length == -1) {
                    // Let us find out the long way!
                    length = Utils.countBytes(context.getContentResolver().openInputStream(contribution.getLocalUri()));
                }
                contribution.setDataLength(length);
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        if(TextUtils.isEmpty(contribution.getCreator())) {
            contribution.setCreator(app.getCurrentAccount().name);
        }

        if(contribution.getDescription() == null) {
            contribution.setDescription("");
        }

        String mimeType = (String)contribution.getTag("mimeType");
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

        if(extension != null && !title.toLowerCase().endsWith(extension.toLowerCase())) {
            title += "." + extension;
        }

        if(mimeType.startsWith("image/") && contribution.getDateCreated() == null) {
            Cursor cursor = context.getContentResolver().query(contribution.getLocalUri(),
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
        uploadService.queue(UploadService.ACTION_UPLOAD_FILE, contribution);
    }
}