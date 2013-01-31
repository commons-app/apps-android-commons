package org.wikimedia.commons.contributions;

import java.text.SimpleDateFormat;
import java.util.*;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.*;
import android.os.RemoteException;
import org.wikimedia.commons.Media;

public class Contribution extends Media {

    // No need to be bitwise - they're mutually exclusive
    public static final int STATE_COMPLETED = 0;
    public static final int STATE_QUEUED = 1;
    public static final int STATE_IN_PROGRESS = 2;

    private ContentProviderClient client;
    private Uri contentUri;

    public String getEditSummary() {
        return editSummary;
    }

    private String editSummary;

    public Date getTimestamp() {
        return timestamp;
    }

    private Date timestamp;
    private int state;

    public Contribution(Uri localUri, Uri remoteUri, String filename, String description, String commonsURL, long dataLength, Date dateCreated, Date dateUploaded, String creator, String editSummary) {
        super(localUri, remoteUri, filename, description, commonsURL, dataLength, dateCreated, dateUploaded, creator);
        this.editSummary = editSummary;
        timestamp = new Date(System.currentTimeMillis());
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getPageContents() {
        StringBuffer buffer = new StringBuffer();
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");
        buffer
            .append("== {{int:filedesc}} ==\n")
                .append("{{Information")
                    .append("|Description=").append(description)
                    .append("|source=").append("{{own}}")
                    .append("|author=[[User:").append(creator).append("]]");
        if(dateCreated != null) {
            buffer
                    .append("|date={{According to EXIF data|").append(isoFormat.format(dateCreated)).append("}}");
        }
        buffer
                .append("}}").append("\n")
            .append("== {{int:license-header}} ==\n")
                .append("{{self|cc-by-sa-3.0}}")
            ;
        return buffer.toString();
    }

    public void setContentProviderClient(ContentProviderClient client) {
        this.client = client;
    }

    public void save() {
        try {
            if(contentUri == null) {
                contentUri = client.insert(ContributionsContentProvider.BASE_URI, this.toContentValues());
            } else {
                client.update(contentUri, toContentValues(), null, null);
            }
        } catch(RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Table.COLUMN_FILENAME, getFilename());
        if(getLocalUri() != null) {
            cv.put(Table.COLUMN_LOCAL_URI, getLocalUri().toString());
        }
        if(getRemoteUri() != null) {
            cv.put(Table.COLUMN_REMOTE_URI, getRemoteUri().toString());
        }
        cv.put(Table.COLUMN_LENGTH, getDataLength());
        cv.put(Table.COLUMN_TIMESTAMP, getTimestamp().getTime());
        cv.put(Table.COLUMN_STATE, getState());
        return cv;
    }


    public static class Table {
        public static final String TABLE_NAME = "contributions";

        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_FILENAME = "filename";
        public static final String COLUMN_LOCAL_URI = "local_uri";
        public static final String COLUMN_REMOTE_URI = "remote_uri";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_STATE = "state";
        public static final String COLUMN_LENGTH = "length";




        private static final String CREATE_TABLE_STATEMENT = "CREATE TABLE " + TABLE_NAME + " ("
                + "_id INTEGER PRIMARY KEY,"
                + "filename STRING,"
                + "local_uri STRING,"
                + "remote_uri STRING,"
                + "timestamp INTEGER,"
                + "state INTEGER,"
                + "length INTEGER"
        + ");";


        public static void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_STATEMENT);
        }

        public static void onUpdate(SQLiteDatabase db) {
            // Drop everything and recreate stuff
            // FIXME: Understatement
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }
}
