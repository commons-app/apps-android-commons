package fr.free.nrw.commons.recentlanguages;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsContentProvider;
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsDao;
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsDao.Table;
import fr.free.nrw.commons.explore.recentsearches.RecentSearch;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesContentProvider;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton
public class RecentLanguagesDao {

    private final Provider<ContentProviderClient> clientProvider;

    @Inject
    public RecentLanguagesDao(@Named("recent_languages") Provider<ContentProviderClient> clientProvider) {
        this.clientProvider = clientProvider;
    }

    /**
     * Find all persisted items bookmarks on database
     * @return list of bookmarks
     */
    public List<String> getRecentLanguages() {
        final List<String> items = new ArrayList<>();
        final ContentProviderClient db = clientProvider.get();
        try (final Cursor cursor = db.query(
            RecentLanguagesContentProvider.BASE_URI,
            RecentLanguagesDao.Table.ALL_FIELDS,
            null,
            new String[]{},
            null)) {
            while (cursor != null && cursor.moveToNext()) {
                items.add(fromCursor(cursor));
            }
        } catch (final RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
        return items;
    }

    /**
     * Add a Bookmark to database
     * @param language : Bookmark to add
     */
    private void addRecentLanguage(final String language) {
        final ContentProviderClient db = clientProvider.get();
        try {
            db.insert(BookmarkItemsContentProvider.BASE_URI, toContentValues(language));
        } catch (final RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
    }

    /**
     * Delete a bookmark from database
     * @param language : Bookmark to delete
     */
    private void deleteRecentLanguage(final String language) {
        final ContentProviderClient db = clientProvider.get();
        try {
            db.delete(RecentLanguagesContentProvider.uriForName(language), null, null);
        } catch (final RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
    }

    /**
     * Find a bookmark from database based on its name
     * @param language : Bookmark to find
     * @return boolean : is bookmark in database ?
     */
    public boolean findRecentLanguage(final String language) {
        if (language == null) { //Avoiding NPE's
            return false;
        }
        final ContentProviderClient db = clientProvider.get();
        try (final Cursor cursor = db.query(
            RecentLanguagesContentProvider.BASE_URI,
            RecentLanguagesDao.Table.ALL_FIELDS,
            Table.COLUMN_NAME + "=?",
            new String[]{language},
            null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                return true;
            }
        } catch (final RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
        return false;
    }

    /**
     * It creates an Recent Searches object from data stored in the SQLite DB by using cursor
     * @param cursor
     * @return RecentSearch object
     */
    @NonNull
    String fromCursor(Cursor cursor) {
        // Hardcoding column positions!
        return cursor.getString(cursor.getColumnIndex(RecentLanguagesDao.Table.COLUMN_NAME));
    }

    /**
     * This class contains the database table architechture for recent searches,
     * It also contains queries and logic necessary to the create, update, delete this table.
     */
    private ContentValues toContentValues(String recentLanguage) {
        ContentValues cv = new ContentValues();
        cv.put(RecentLanguagesDao.Table.COLUMN_NAME, recentLanguage);
        return cv;
    }

    /**
     * This class contains the database table architechture for recent searches,
     * It also contains queries and logic necessary to the create, update, delete this table.
     */
    public static class Table {
        public static final String TABLE_NAME = "recent_languages";
        static final String COLUMN_NAME = "language_name";

        // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
        public static final String[] ALL_FIELDS = {
            COLUMN_NAME,
        };

        static final String DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS " + TABLE_NAME;

        static final String CREATE_TABLE_STATEMENT = "CREATE TABLE " + TABLE_NAME + " ("
            + COLUMN_NAME + " STRING PRIMARY KEY"
            + ");";

        /**
         * This method creates a RecentSearchesTable in SQLiteDatabase
         * @param db SQLiteDatabase
         */
        public static void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_STATEMENT);
        }

        /**
         * This method deletes RecentSearchesTable from SQLiteDatabase
         * @param db SQLiteDatabase
         */
        public static void onDelete(SQLiteDatabase db) {
            db.execSQL(DROP_TABLE_STATEMENT);
            onCreate(db);
        }

        /**
         * This method is called on migrating from a older version to a newer version
         * @param db SQLiteDatabase
         * @param from Version from which we are migrating
         * @param to Version to which we are migrating
         */
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
