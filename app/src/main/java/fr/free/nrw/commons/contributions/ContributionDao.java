package fr.free.nrw.commons.contributions;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import fr.free.nrw.commons.settings.Prefs;
import timber.log.Timber;

import static fr.free.nrw.commons.contributions.ContributionDao.Table.ALL_FIELDS;
import static fr.free.nrw.commons.contributions.ContributionDao.Table.COLUMN_WIKI_DATA_ENTITY_ID;
import static fr.free.nrw.commons.contributions.ContributionsContentProvider.BASE_URI;
import static fr.free.nrw.commons.contributions.ContributionsContentProvider.uriForId;

public class ContributionDao {
    /*
        This sorts in the following order:
        Currently Uploading
        Failed (Sorted in ascending order of time added - FIFO)
        Queued to Upload (Sorted in ascending order of time added - FIFO)
        Completed (Sorted in descending order of time added)

        This is why Contribution.STATE_COMPLETED is -1.
     */
    static final String CONTRIBUTION_SORT = Table.COLUMN_STATE + " DESC, "
            + Table.COLUMN_UPLOADED + " DESC , ("
            + Table.COLUMN_TIMESTAMP + " * "
            + Table.COLUMN_STATE + ")";

    private final Provider<ContentProviderClient> clientProvider;

    @Inject
    public ContributionDao(@Named("contribution") Provider<ContentProviderClient> clientProvider) {
        this.clientProvider = clientProvider;
    }

    Cursor loadAllContributions() {
        ContentProviderClient db = clientProvider.get();
        try {
            return db.query(BASE_URI, ALL_FIELDS, "", null, CONTRIBUTION_SORT);
        } catch (RemoteException e) {
            return null;
        } finally {
            db.release();
        }
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

    ContentValues toContentValues(Contribution contribution) {
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
        //This was always meant to store the date created..If somehow date created is not fetched while actually saving the contribution, lets saveValue today's date
        cv.put(Table.COLUMN_TIMESTAMP, contribution.getDateCreated()==null?System.currentTimeMillis():contribution.getDateCreated().getTime());
        cv.put(Table.COLUMN_STATE, contribution.getState());
        cv.put(Table.COLUMN_TRANSFERRED, contribution.getTransferred());
        cv.put(Table.COLUMN_SOURCE, contribution.getSource());
        cv.put(Table.COLUMN_DESCRIPTION, contribution.getDescription());
        cv.put(Table.COLUMN_CREATOR, contribution.getCreator());
        cv.put(Table.COLUMN_MULTIPLE, contribution.getMultiple() ? 1 : 0);
        cv.put(Table.COLUMN_WIDTH, contribution.getWidth());
        cv.put(Table.COLUMN_HEIGHT, contribution.getHeight());
        cv.put(Table.COLUMN_LICENSE, contribution.getLicense());
        cv.put(Table.COLUMN_WIKI_DATA_ENTITY_ID, contribution.getWikiDataEntityId());
        return cv;
    }

    public Contribution fromCursor(Cursor cursor) {
        // Hardcoding column positions!
        //Check that cursor has a value to avoid CursorIndexOutOfBoundsException
        if (cursor.getCount() > 0) {
            int index;
            if (cursor.getColumnIndex(Table.COLUMN_LICENSE) == -1){
                index = 15;
            } else {
                index = cursor.getColumnIndex(Table.COLUMN_LICENSE);
            }
            Contribution contribution = new Contribution(
                    uriForId(cursor.getInt(cursor.getColumnIndex(Table.COLUMN_ID))),
                    cursor.getString(cursor.getColumnIndex(Table.COLUMN_FILENAME)),
                    parseUri(cursor.getString(cursor.getColumnIndex(Table.COLUMN_LOCAL_URI))),
                    cursor.getString(cursor.getColumnIndex(Table.COLUMN_IMAGE_URL)),
                    parseTimestamp(cursor.getLong(cursor.getColumnIndex(Table.COLUMN_TIMESTAMP))),
                    cursor.getInt(cursor.getColumnIndex(Table.COLUMN_STATE)),
                    cursor.getLong(cursor.getColumnIndex(Table.COLUMN_LENGTH)),
                    parseTimestamp(cursor.getLong(cursor.getColumnIndex(Table.COLUMN_UPLOADED))),
                    cursor.getLong(cursor.getColumnIndex(Table.COLUMN_TRANSFERRED)),
                    cursor.getString(cursor.getColumnIndex(Table.COLUMN_SOURCE)),
                    cursor.getString(cursor.getColumnIndex(Table.COLUMN_DESCRIPTION)),
                    cursor.getString(cursor.getColumnIndex(Table.COLUMN_CREATOR)),
                    cursor.getInt(cursor.getColumnIndex(Table.COLUMN_MULTIPLE)) == 1,
                    cursor.getInt(cursor.getColumnIndex(Table.COLUMN_WIDTH)),
                    cursor.getInt(cursor.getColumnIndex(Table.COLUMN_HEIGHT)),
                    cursor.getString(index)
            );

            String wikidataEntityId = cursor.getString(cursor.getColumnIndex(COLUMN_WIKI_DATA_ENTITY_ID));
            if (!StringUtils.isBlank(wikidataEntityId)) {
                contribution.setWikiDataEntityId(wikidataEntityId);
            }

            return contribution;
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
        public static final String COLUMN_WIKI_DATA_ENTITY_ID = "wikidataEntityID";

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
                COLUMN_LICENSE,
                COLUMN_WIKI_DATA_ENTITY_ID
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
                + "LICENSE STRING,"
                + "wikidataEntityID STRING"
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

        // Upgrade from version 8 ->
        static final String ADD_WIKI_DATA_ENTITY_ID_FIELD = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN wikidataEntityID STRING;";


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

            //Considering the crashes we have been facing recently, lets blindly add this column to any table which has ever existed
            runQuery(db,ADD_WIKI_DATA_ENTITY_ID_FIELD);

            if (from == 1) {
                runQuery(db,ADD_DESCRIPTION_FIELD);
                runQuery(db,ADD_CREATOR_FIELD);
                from++;
                onUpdate(db, from, to);
                return;
            }
            if (from == 2) {
                runQuery(db, ADD_MULTIPLE_FIELD);
                runQuery(db, SET_DEFAULT_MULTIPLE);
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
                runQuery(db, ADD_WIDTH_FIELD);
                runQuery(db, SET_DEFAULT_WIDTH);
                runQuery(db, ADD_HEIGHT_FIELD);
                runQuery(db, SET_DEFAULT_HEIGHT);
                runQuery(db, ADD_LICENSE_FIELD);
                runQuery(db, SET_DEFAULT_LICENSE);
                from++;
                onUpdate(db, from, to);
                return;
            }
            if (from > 5) {
                // Added place field
                from=to;
                onUpdate(db, from, to);
                return;
            }
        }

        /**
         * perform the db.execSQl with handled exceptions
         */
        private static void runQuery(SQLiteDatabase db, String query) {
            try {
                db.execSQL(query);
            } catch (SQLiteException e) {
                Timber.e("Exception performing query: " + query + " message: " + e.getMessage());
            }
        }

    }
}
