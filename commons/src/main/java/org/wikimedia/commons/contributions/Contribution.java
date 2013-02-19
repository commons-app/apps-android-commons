package org.wikimedia.commons.contributions;

import java.text.SimpleDateFormat;
import java.util.*;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.*;
import android.os.RemoteException;
import android.text.TextUtils;
import org.wikimedia.commons.CommonsApplication;
import org.wikimedia.commons.EventLog;
import org.wikimedia.commons.Media;

public class Contribution extends Media {

    // No need to be bitwise - they're mutually exclusive
    public static final int STATE_COMPLETED = -1;
    public static final int STATE_FAILED = 1;
    public static final int STATE_QUEUED = 2;
    public static final int STATE_IN_PROGRESS = 3;

    public static final String SOURCE_CAMERA = "camera";
    public static final String SOURCE_GALLERY = "gallery";
    public static final String SOURCE_EXTERNAL = "external";

    private ContentProviderClient client;
    private Uri contentUri;
    private String source;

    public EventLog.LogBuilder event;

    public long getTransferred() {
        return transferred;
    }

    public void setTransferred(long transferred) {
        this.transferred = transferred;
    }

    private long transferred;

    public String getEditSummary() {
        return editSummary != null ? editSummary : CommonsApplication.DEFAULT_EDIT_SUMMARY;
    }

    public Uri getContentUri() {
        return contentUri;
    }
    private String editSummary;

    public Date getTimestamp() {
        return timestamp;
    }

    private Date timestamp;
    private int state;

    public Contribution(Uri localUri, String remoteUri, String filename, String description, long dataLength, Date dateCreated, Date dateUploaded, String creator, String editSummary) {
        super(localUri, remoteUri, filename, description, dataLength, dateCreated, dateUploaded, creator);
        this.editSummary = editSummary;
        timestamp = new Date(System.currentTimeMillis());
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setDateUploaded(Date date) {
        this.dateUploaded = date;
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
        buffer.append("\n{{Uploaded from Mobile|platform=Android|version=").append(CommonsApplication.APPLICATION_VERSION).append("}}");
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


    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Table.COLUMN_FILENAME, getFilename());
        if(getLocalUri() != null) {
            cv.put(Table.COLUMN_LOCAL_URI, getLocalUri().toString());
        }
        if(getImageUrl() != null) {
            cv.put(Table.COLUMN_IMAGE_URL, getImageUrl().toString());
        }
        if(getDateUploaded() != null) {
            cv.put(Table.COLUMN_UPLOADED, getDateUploaded().getTime());
        }
        cv.put(Table.COLUMN_LENGTH, getDataLength());
        cv.put(Table.COLUMN_TIMESTAMP, getTimestamp().getTime());
        cv.put(Table.COLUMN_STATE, getState());
        cv.put(Table.COLUMN_TRANSFERRED, transferred);
        cv.put(Table.COLUMN_SOURCE,  source);
        return cv;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    private Contribution() {
        // Empty constructor for being constructed by our static methods
    }

    public static Contribution fromCursor(Cursor cursor) {
        // Hardcoding column positions!
        Contribution c = new Contribution();
        c.contentUri = ContributionsContentProvider.uriForId(cursor.getInt(0));
        c.filename = cursor.getString(1);
        c.localUri = TextUtils.isEmpty(cursor.getString(2)) ? null : Uri.parse(cursor.getString(2));
        c.imageUrl = cursor.getString(3);
        c.timestamp = cursor.getLong(4) == 0 ? null : new Date(cursor.getLong(4));
        c.state = cursor.getInt(5);
        c.dataLength = cursor.getLong(6);
        c.dateUploaded =  cursor.getLong(7) == 0 ? null : new Date(cursor.getLong(7));
        c.transferred = cursor.getLong(8);
        c.source = cursor.getString(9);
        return c;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }


    public static class Table {
        public static final String TABLE_NAME = "contributions";

        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_FILENAME = "filename";
        public static final String COLUMN_LOCAL_URI = "local_uri";
        public static final String COLUMN_IMAGE_URL = "image_url";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_STATE = "state";
        public static final String COLUMN_LENGTH = "length";
        public static final String COLUMN_UPLOADED = "uploaded";
        public static final String COLUMN_TRANSFERRED = "transferred"; // Currently transferred number of bytes
        public static final String COLUMN_SOURCE = "source";

        // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
        public static final String[] ALL_FIELDS = {
                COLUMN_ID,
                COLUMN_FILENAME,
                COLUMN_LOCAL_URI,
                COLUMN_IMAGE_URL,
                COLUMN_TIMESTAMP,
                COLUMN_STATE,
                COLUMN_LENGTH,
                COLUMN_UPLOADED,
                COLUMN_TRANSFERRED,
                COLUMN_SOURCE
        };


        private static final String CREATE_TABLE_STATEMENT = "CREATE TABLE " + TABLE_NAME + " ("
                + "_id INTEGER PRIMARY KEY,"
                + "filename STRING,"
                + "local_uri STRING,"
                + "image_url STRING,"
                + "uploaded INTEGER,"
                + "timestamp INTEGER,"
                + "state INTEGER,"
                + "length INTEGER,"
                + "transferred INTEGER,"
                + "source STRING"
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
