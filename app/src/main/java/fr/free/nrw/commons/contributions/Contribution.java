package fr.free.nrw.commons.contributions;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Parcel;
import android.os.RemoteException;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.settings.Prefs;

public class Contribution extends Media {

    public static Creator<Contribution> CREATOR = new Creator<Contribution>() {
        @Override
        public Contribution createFromParcel(Parcel parcel) {
            return new Contribution(parcel);
        }

        @Override
        public Contribution[] newArray(int i) {
            return new Contribution[0];
        }
    };

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
    private String editSummary;
    private Date timestamp;
    private int state;
    private long transferred;
    private String decimalCoords;

    private boolean isMultiple;

    public boolean getMultiple() {
        return isMultiple;
    }

    public void setMultiple(boolean multiple) {
        isMultiple = multiple;
    }

    public Contribution(Uri localUri, String remoteUri, String filename, String description, long dataLength, Date dateCreated, Date dateUploaded, String creator, String editSummary, String decimalCoords) {
        super(localUri, remoteUri, filename, description, dataLength, dateCreated, dateUploaded, creator);
        this.decimalCoords = decimalCoords;
        this.editSummary = editSummary;
        timestamp = new Date(System.currentTimeMillis());
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeParcelable(contentUri, flags);
        parcel.writeString(source);
        parcel.writeSerializable(timestamp);
        parcel.writeInt(state);
        parcel.writeLong(transferred);
        parcel.writeInt(isMultiple ? 1 : 0);
    }

    public Contribution(Parcel in) {
        super(in);
        contentUri = in.readParcelable(Uri.class.getClassLoader());
        source = in.readString();
        timestamp = (Date) in.readSerializable();
        state = in.readInt();
        transferred = in.readLong();
        isMultiple = in.readInt() == 1;
    }

    public long getTransferred() {
        return transferred;
    }

    public void setTransferred(long transferred) {
        this.transferred = transferred;
    }

    public String getEditSummary() {
        return editSummary != null ? editSummary : CommonsApplication.DEFAULT_EDIT_SUMMARY;
    }

    public Uri getContentUri() {
        return contentUri;
    }

    public Date getTimestamp() {
        return timestamp;
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

    public String getTrackingTemplates() {
        return "{{subst:unc}}";  // Remove when we have categorization
    }

    public String getPageContents() {
        StringBuilder buffer = new StringBuilder();
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

        buffer
                .append("== {{int:filedesc}} ==\n")
                .append("{{Information\n")
                .append("|description=").append(getDescription()).append("\n")
                .append("|source=").append("{{own}}\n")
                .append("|author=[[User:").append(creator).append("|").append(creator).append("]]\n");
        if (dateCreated != null) {
            buffer
                    .append("|date={{According to EXIF data|").append(isoFormat.format(dateCreated)).append("}}\n");
        }
        buffer
                .append("}}").append("\n");

        //Only add Location template (e.g. {{Location|37.51136|-77.602615}} ) if coords is not null
        if (decimalCoords != null) {
            buffer.append("{{Location|").append(decimalCoords).append("}}").append("\n");
        }

        buffer.append("== {{int:license-header}} ==\n")
                .append(Utils.licenseTemplateFor(getLicense())).append("\n\n")
                .append("{{Uploaded from Mobile|platform=Android|version=").append(BuildConfig.VERSION_NAME).append("}}\n")
                .append(getTrackingTemplates());
        return buffer.toString();
    }

    public void setContentProviderClient(ContentProviderClient client) {
        this.client = client;
    }

    public void save() {
        try {
            if (contentUri == null) {
                contentUri = client.insert(ContributionsContentProvider.BASE_URI, this.toContentValues());
            } else {
                client.update(contentUri, toContentValues(), null, null);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete() {
        try {
            if (contentUri == null) {
                // noooo
                throw new RuntimeException("tried to delete item with no content URI");
            } else {
                client.delete(contentUri, null, null);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }


    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Table.COLUMN_FILENAME, getFilename());
        if (getLocalUri() != null) {
            cv.put(Table.COLUMN_LOCAL_URI, getLocalUri().toString());
        }
        if (getImageUrl() != null) {
            cv.put(Table.COLUMN_IMAGE_URL, getImageUrl());
        }
        if (getDateUploaded() != null) {
            cv.put(Table.COLUMN_UPLOADED, getDateUploaded().getTime());
        }
        cv.put(Table.COLUMN_LENGTH, getDataLength());
        cv.put(Table.COLUMN_TIMESTAMP, getTimestamp().getTime());
        cv.put(Table.COLUMN_STATE, getState());
        cv.put(Table.COLUMN_TRANSFERRED, transferred);
        cv.put(Table.COLUMN_SOURCE, source);
        cv.put(Table.COLUMN_DESCRIPTION, description);
        cv.put(Table.COLUMN_CREATOR, creator);
        cv.put(Table.COLUMN_MULTIPLE, isMultiple ? 1 : 0);
        cv.put(Table.COLUMN_WIDTH, width);
        cv.put(Table.COLUMN_HEIGHT, height);
        cv.put(Table.COLUMN_LICENSE, license);
        return cv;
    }

    @Override
    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Contribution() {
        timestamp = new Date(System.currentTimeMillis());
    }

    public static Contribution fromCursor(Cursor cursor) {
        // Hardcoding column positions!
        Contribution c = new Contribution();

        //Check that cursor has a value to avoid CursorIndexOutOfBoundsException
        if (cursor.getCount() > 0) {
            c.contentUri = ContributionsContentProvider.uriForId(cursor.getInt(0));
            c.filename = cursor.getString(1);
            c.localUri = TextUtils.isEmpty(cursor.getString(2)) ? null : Uri.parse(cursor.getString(2));
            c.imageUrl = cursor.getString(3);
            c.timestamp = cursor.getLong(4) == 0 ? null : new Date(cursor.getLong(4));
            c.state = cursor.getInt(5);
            c.dataLength = cursor.getLong(6);
            c.dateUploaded = cursor.getLong(7) == 0 ? null : new Date(cursor.getLong(7));
            c.transferred = cursor.getLong(8);
            c.source = cursor.getString(9);
            c.description = cursor.getString(10);
            c.creator = cursor.getString(11);
            c.isMultiple = cursor.getInt(12) == 1;
            c.width = cursor.getInt(13);
            c.height = cursor.getInt(14);
            c.license = cursor.getString(15);
        }

        return c;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setLocalUri(Uri localUri) {
        this.localUri = localUri;
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
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_CREATOR = "creator"; // Initial uploader
        public static final String COLUMN_MULTIPLE = "multiple";
        public static final String COLUMN_WIDTH = "width";
        public static final String COLUMN_HEIGHT = "height";
        public static final String COLUMN_LICENSE = "license";

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
                COLUMN_SOURCE,
                COLUMN_DESCRIPTION,
                COLUMN_CREATOR,
                COLUMN_MULTIPLE,
                COLUMN_WIDTH,
                COLUMN_HEIGHT,
                COLUMN_LICENSE
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
                + "source STRING,"
                + "description STRING,"
                + "creator STRING,"
                + "multiple INTEGER,"
                + "width INTEGER,"
                + "height INTEGER,"
                + "LICENSE STRING"
                + ");";


        public static void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_STATEMENT);
        }

        public static void onDelete(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }

        public static void onUpdate(SQLiteDatabase db, int from, int to) {
            if (from == to) {
                return;
            }
            if (from == 1) {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN description STRING;");
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN creator STRING;");
                from++;
                onUpdate(db, from, to);
                return;
            }
            if (from == 2) {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN multiple INTEGER;");
                db.execSQL("UPDATE " + TABLE_NAME + " SET multiple = 0");
                from++;
                onUpdate(db, from, to);
                return;
            }
            if (from == 3) {
                // Do nothing
                from++;
                onUpdate(db, from, to);
                return;
            }
            if (from == 4) {
                // Do nothing -- added Category
                from++;
                onUpdate(db, from, to);
                return;
            }
            if (from == 5) {
                // Added width and height fields
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN width INTEGER;");
                db.execSQL("UPDATE " + TABLE_NAME + " SET width = 0");
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN height INTEGER;");
                db.execSQL("UPDATE " + TABLE_NAME + " SET height = 0");
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN license STRING;");
                db.execSQL("UPDATE " + TABLE_NAME + " SET license='" + Prefs.Licenses.CC_BY_SA_3 + "';");
                from++;
                onUpdate(db, from, to);
                return;
            }
        }
    }
}
