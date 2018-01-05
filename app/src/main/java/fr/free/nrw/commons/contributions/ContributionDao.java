package fr.free.nrw.commons.contributions;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import fr.free.nrw.commons.settings.Prefs;

import static fr.free.nrw.commons.contributions.ContributionsContentProvider.BASE_URI;
import static fr.free.nrw.commons.contributions.ContributionsContentProvider.uriForId;

public class ContributionDao {
    private final Provider<ContentProviderClient> clientProvider;

    @Inject
    public ContributionDao(@Named("contribution") Provider<ContentProviderClient> clientProvider) {
        this.clientProvider = clientProvider;
    }

    public void save(Contribution contribution) {
        ContentProviderClient db = clientProvider.get();
        try {
            if (contribution.getContentUri() == null) {
                contribution.setContentUri(db.insert(BASE_URI, toContentValues(contribution)));
            } else {
                db.update(contribution.getContentUri(), toContentValues(contribution), null, null);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
    }

    public void delete(Contribution contribution) {
        ContentProviderClient db = clientProvider.get();
        try {
            if (contribution.getContentUri() == null) {
                // noooo
                throw new RuntimeException("tried to delete item with no content URI");
            } else {
                db.delete(contribution.getContentUri(), null, null);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
    }

    public static ContentValues toContentValues(Contribution contribution) {
        ContentValues cv = new ContentValues();
        cv.put(Table.COLUMN_FILENAME, contribution.getFilename());
        if (contribution.getLocalUri() != null) {
            cv.put(Table.COLUMN_LOCAL_URI, contribution.getLocalUri().toString());
        }
        if (contribution.getImageUrl() != null) {
            cv.put(Table.COLUMN_IMAGE_URL, contribution.getImageUrl());
        }
        if (contribution.getDateUploaded() != null) {
            cv.put(Table.COLUMN_UPLOADED, contribution.getDateUploaded().getTime());
        }
        cv.put(Table.COLUMN_LENGTH, contribution.getDataLength());
        cv.put(Table.COLUMN_TIMESTAMP, contribution.getTimestamp().getTime());
        cv.put(Table.COLUMN_STATE, contribution.getState());
        cv.put(Table.COLUMN_TRANSFERRED, contribution.getTransferred());
        cv.put(Table.COLUMN_SOURCE, contribution.getSource());
        cv.put(Table.COLUMN_DESCRIPTION, contribution.getDescription());
        cv.put(Table.COLUMN_CREATOR, contribution.getCreator());
        cv.put(Table.COLUMN_MULTIPLE, contribution.getMultiple() ? 1 : 0);
        cv.put(Table.COLUMN_WIDTH, contribution.getWidth());
        cv.put(Table.COLUMN_HEIGHT, contribution.getHeight());
        cv.put(Table.COLUMN_LICENSE, contribution.getLicense());
        return cv;
    }

    public Contribution fromCursor(Cursor cursor) {
        // Hardcoding column positions!
        //Check that cursor has a value to avoid CursorIndexOutOfBoundsException
        if (cursor.getCount() > 0) {
            return new Contribution(
                    uriForId(cursor.getInt(0)),
                    cursor.getString(1),
                    parseUri(cursor.getString(2)),
                    cursor.getString(3),
                    parseTimestamp(cursor.getLong(4)),
                    cursor.getInt(5),
                    cursor.getLong(6),
                    parseTimestamp(cursor.getLong(7)),
                    cursor.getLong(8),
                    cursor.getString(9),
                    cursor.getString(10),
                    cursor.getString(11),
                    cursor.getInt(12) == 1,
                    cursor.getInt(13),
                    cursor.getInt(14),
                    cursor.getString(15));
        }

        return null;
    }

    @Nullable
    private static Date parseTimestamp(long timestamp) {
        return timestamp == 0 ? null : new Date(timestamp);
    }

    @Nullable
    private static Uri parseUri(String uriString) {
        return TextUtils.isEmpty(uriString) ? null : Uri.parse(uriString);
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

        public static final String DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS " + TABLE_NAME;

        public static final String CREATE_TABLE_STATEMENT = "CREATE TABLE " + TABLE_NAME + " ("
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

        // Upgrade from version 1 ->
        static final String ADD_CREATOR_FIELD = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN creator STRING;";
        static final String ADD_DESCRIPTION_FIELD = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN description STRING;";

        // Upgrade from version 2 ->
        static final String ADD_MULTIPLE_FIELD = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN multiple INTEGER;";
        static final String SET_DEFAULT_MULTIPLE = "UPDATE " + TABLE_NAME + " SET multiple = 0";

        // Upgrade from version 5 ->
        static final String ADD_WIDTH_FIELD = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN width INTEGER;";
        static final String SET_DEFAULT_WIDTH = "UPDATE " + TABLE_NAME + " SET width = 0";
        static final String ADD_HEIGHT_FIELD = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN height INTEGER;";
        static final String SET_DEFAULT_HEIGHT = "UPDATE " + TABLE_NAME + " SET height = 0";
        static final String ADD_LICENSE_FIELD = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN license STRING;";
        static final String SET_DEFAULT_LICENSE = "UPDATE " + TABLE_NAME + " SET license='" + Prefs.Licenses.CC_BY_SA_3 + "';";


        public static void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_STATEMENT);
        }

        public static void onDelete(SQLiteDatabase db) {
            db.execSQL(DROP_TABLE_STATEMENT);
            onCreate(db);
        }

        public static void onUpdate(SQLiteDatabase db, int from, int to) {
            if (from == to) {
                return;
            }
            if (from == 1) {
                db.execSQL(ADD_DESCRIPTION_FIELD);
                db.execSQL(ADD_CREATOR_FIELD);
                from++;
                onUpdate(db, from, to);
                return;
            }
            if (from == 2) {
                db.execSQL(ADD_MULTIPLE_FIELD);
                db.execSQL(SET_DEFAULT_MULTIPLE);
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
                db.execSQL(ADD_WIDTH_FIELD);
                db.execSQL(SET_DEFAULT_WIDTH);
                db.execSQL(ADD_HEIGHT_FIELD);
                db.execSQL(SET_DEFAULT_HEIGHT);
                db.execSQL(ADD_LICENSE_FIELD);
                db.execSQL(SET_DEFAULT_LICENSE);
                from++;
                onUpdate(db, from, to);
                return;
            }
        }
    }
}
