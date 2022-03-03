package fr.free.nrw.commons.recentlanguages;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

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
    public List<Language> getRecentLanguages() {
        final List<Language> items = new ArrayList<>();
        final ContentProviderClient db = clientProvider.get();
        try (final Cursor cursor = db.query(
            RecentLanguagesContentProvider.BASE_URI,
            RecentLanguagesDao.Table.ALL_FIELDS,
            null,
            new String[]{},
            null)) {
            if(cursor != null ) {
                cursor.moveToLast();
                do {
                    items.add(fromCursor(cursor));
                } while (cursor.moveToPrevious());
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
    public void addRecentLanguage(final Language language) {
        final ContentProviderClient db = clientProvider.get();
        try {
            db.insert(RecentLanguagesContentProvider.BASE_URI, toContentValues(language));
        } catch (final RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
    }

    /**
     * Delete a bookmark from database
     * @param languageCode : Bookmark to delete
     */
    public void deleteRecentLanguage(final String languageCode) {
        final ContentProviderClient db = clientProvider.get();
        try {
            db.delete(RecentLanguagesContentProvider.uriForName(languageCode), null, null);
        } catch (final RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
    }

    /**
     * Find a bookmark from database based on its name
     * @param languageCode : Bookmark to find
     * @return boolean : is bookmark in database ?
     */
    public boolean findRecentLanguage(final String languageCode) {
        if (languageCode == null) { //Avoiding NPE's
            return false;
        }
        final ContentProviderClient db = clientProvider.get();
        try (final Cursor cursor = db.query(
            RecentLanguagesContentProvider.BASE_URI,
            RecentLanguagesDao.Table.ALL_FIELDS,
            Table.COLUMN_CODE + "=?",
            new String[]{languageCode},
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
    Language fromCursor(final Cursor cursor) {
        // Hardcoding column positions!
        final String languageName = cursor.getString(cursor.getColumnIndex(Table.COLUMN_NAME));
        final String languageCode = cursor.getString(cursor.getColumnIndex(Table.COLUMN_CODE));
        return new Language(languageName, languageCode);
    }

    /**
     * This class contains the database table architechture for recent searches,
     * It also contains queries and logic necessary to the create, update, delete this table.
     */
    private ContentValues toContentValues(final Language recentLanguage) {
        final ContentValues cv = new ContentValues();
        cv.put(Table.COLUMN_NAME, recentLanguage.getLanguageName());
        cv.put(Table.COLUMN_CODE, recentLanguage.getLanguageCode());
        return cv;
    }

    /**
     * This class contains the database table architechture for recent searches,
     * It also contains queries and logic necessary to the create, update, delete this table.
     */
    public static class Table {
        public static final String TABLE_NAME = "recent_languages";
        static final String COLUMN_NAME = "language_name";
        static final String COLUMN_CODE = "language_code";

        // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
        public static final String[] ALL_FIELDS = {
            COLUMN_NAME,
            COLUMN_CODE
        };

        static final String DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS " + TABLE_NAME;

        static final String CREATE_TABLE_STATEMENT = "CREATE TABLE " + TABLE_NAME + " ("
            + COLUMN_NAME + " STRING,"
            + COLUMN_CODE + " STRING PRIMARY KEY"
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
