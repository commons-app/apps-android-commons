package fr.free.nrw.commons.explore.recentsearches;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;


public class RecentSearchesDao {

    private final Provider<ContentProviderClient> clientProvider;

    @Inject
    public RecentSearchesDao(@Named("recentsearch") Provider<ContentProviderClient> clientProvider) {
        this.clientProvider = clientProvider;
    }

    public void save(RecentSearch recentSearch) {
        ContentProviderClient db = clientProvider.get();
        try {
            if (recentSearch.getContentUri() == null) {
                recentSearch.setContentUri(db.insert(RecentSearchesContentProvider.BASE_URI, toContentValues(recentSearch)));
            } else {
                db.update(recentSearch.getContentUri(), toContentValues(recentSearch), null, null);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
    }

    /**
     * Find persisted search query in database, based on its name.
     *
     * @param name Search query  Ex- "butterfly"
     * @return recently searched query from database, or null if not found
     */
    @Nullable
    public RecentSearch find(String name) {
        Cursor cursor = null;
        ContentProviderClient db = clientProvider.get();
        try {
            cursor = db.query(
                    RecentSearchesContentProvider.BASE_URI,
                    Table.ALL_FIELDS,
                    Table.COLUMN_NAME + "=?",
                    new String[]{name},
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
        } catch (RemoteException e) {
            // This feels lazy, but to hell with checked exceptions. :)
            throw new RuntimeException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.release();
        }
        return null;
    }

    /**
     * Retrieve recently-searched queries, ordered by descending date.
     *
     * @return a list containing recent searches
     */
    @NonNull
    public List<String> recentSearches(int limit) {
        List<String> items = new ArrayList<>();
        Cursor cursor = null;
        ContentProviderClient db = clientProvider.get();
        try {
            cursor = db.query( RecentSearchesContentProvider.BASE_URI, Table.ALL_FIELDS,
                    null, new String[]{}, Table.COLUMN_LAST_USED + " DESC");
            // fixme add a limit on the original query instead of falling out of the loop?
            while (cursor != null && cursor.moveToNext() && cursor.getPosition() < limit) {
                items.add(fromCursor(cursor).getQuery());
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.release();
        }
        return items;
    }

    @NonNull
    RecentSearch fromCursor(Cursor cursor) {
        // Hardcoding column positions!
        return new RecentSearch(
                RecentSearchesContentProvider.uriForId(cursor.getInt(cursor.getColumnIndex(Table.COLUMN_ID))),
                cursor.getString(cursor.getColumnIndex(Table.COLUMN_NAME)),
                new Date(cursor.getLong(cursor.getColumnIndex(Table.COLUMN_LAST_USED)))
        );
    }

    private ContentValues toContentValues(RecentSearch recentSearch) {
        ContentValues cv = new ContentValues();
        cv.put(RecentSearchesDao.Table.COLUMN_NAME, recentSearch.getQuery());
        cv.put(RecentSearchesDao.Table.COLUMN_LAST_USED, recentSearch.getLastSearched().getTime());
        return cv;
    }

    public static class Table {
        public static final String TABLE_NAME = "recent_searches";
        public static final String COLUMN_ID = "_id";
        static final String COLUMN_NAME = "name";
        static final String COLUMN_LAST_USED = "last_used";

        // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
        public static final String[] ALL_FIELDS = {
                COLUMN_ID,
                COLUMN_NAME,
                COLUMN_LAST_USED,
        };

        static final String DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS " + TABLE_NAME;

        static final String CREATE_TABLE_STATEMENT = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_NAME + " STRING,"
                + COLUMN_LAST_USED + " INTEGER"
                + ");";

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
            if (from < 6) {
                // doesn't exist yet
                from++;
                onUpdate(db, from, to);
                return;
            }
            if (from == 6) {
                // table added in version 7
                onCreate(db);
                from++;
                onUpdate(db, from, to);
                return;
            }
            if (from == 7) {
                from++;
                onUpdate(db, from, to);
                return;
            }
        }
    }
}
