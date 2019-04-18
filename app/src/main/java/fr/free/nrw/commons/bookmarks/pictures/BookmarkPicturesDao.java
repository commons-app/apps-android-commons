package fr.free.nrw.commons.bookmarks.pictures;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import fr.free.nrw.commons.bookmarks.Bookmark;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import static fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesContentProvider.BASE_URI;

@Singleton
public class BookmarkPicturesDao {

    private final Provider<ContentProviderClient> clientProvider;

    @Inject
    public BookmarkPicturesDao(@Named("bookmarks") Provider<ContentProviderClient> clientProvider) {
        this.clientProvider = clientProvider;
    }


    /**
     * Find all persisted pictures bookmarks on database
     *
     * @return list of bookmarks
     */
    @NonNull
    public List<Bookmark> getAllBookmarks() {
        List<Bookmark> items = new ArrayList<>();
        Cursor cursor = null;
        ContentProviderClient db = clientProvider.get();
        try {
            cursor = db.query(
                    BookmarkPicturesContentProvider.BASE_URI,
                    Table.ALL_FIELDS,
                    null,
                    new String[]{},
                    null);
            while (cursor != null && cursor.moveToNext()) {
                items.add(fromCursor(cursor));
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


    /**
     * Look for a bookmark in database and in order to insert or delete it
     *
     * @param bookmark : Bookmark object
     * @return boolean : is bookmark now fav ?
     */
    public boolean updateBookmark(Bookmark bookmark) {
        boolean bookmarkExists = findBookmark(bookmark);
        if (bookmarkExists) {
            deleteBookmark(bookmark);
        } else {
            addBookmark(bookmark);
        }
        return !bookmarkExists;
    }

    /**
     * Add a Bookmark to database
     *
     * @param bookmark : Bookmark to add
     */
    private void addBookmark(Bookmark bookmark) {
        ContentProviderClient db = clientProvider.get();
        try {
            db.insert(BASE_URI, toContentValues(bookmark));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
    }

    /**
     * Delete a bookmark from database
     *
     * @param bookmark : Bookmark to delete
     */
    private void deleteBookmark(Bookmark bookmark) {
        ContentProviderClient db = clientProvider.get();
        try {
            if (bookmark.getContentUri() == null) {
                throw new RuntimeException("tried to delete item with no content URI");
            } else {
                db.delete(bookmark.getContentUri(), null, null);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
    }

    /**
     * Find a bookmark from database based on its name
     *
     * @param bookmark : Bookmark to find
     * @return boolean : is bookmark in database ?
     */
    public boolean findBookmark(Bookmark bookmark) {
        if (bookmark == null) {//Avoiding NPE's
            return false;
        }

        Cursor cursor = null;
        ContentProviderClient db = clientProvider.get();
        try {
            cursor = db.query(
                    BookmarkPicturesContentProvider.BASE_URI,
                    Table.ALL_FIELDS,
                    Table.COLUMN_MEDIA_NAME + "=?",
                    new String[]{bookmark.getMediaName()},
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                return true;
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
        return false;
    }

    @NonNull
    Bookmark fromCursor(Cursor cursor) {
        return new Bookmark(
                cursor.getString(cursor.getColumnIndex(Table.COLUMN_MEDIA_NAME)),
                cursor.getString(cursor.getColumnIndex(Table.COLUMN_CREATOR))
        );
    }

    private ContentValues toContentValues(Bookmark bookmark) {
        ContentValues cv = new ContentValues();
        cv.put(BookmarkPicturesDao.Table.COLUMN_MEDIA_NAME, bookmark.getMediaName());
        cv.put(BookmarkPicturesDao.Table.COLUMN_CREATOR, bookmark.getMediaCreator());
        return cv;
    }


    public static class Table {
        public static final String TABLE_NAME = "bookmarks";

        public static final String COLUMN_MEDIA_NAME = "media_name";
        public static final String COLUMN_CREATOR = "media_creator";

        // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
        public static final String[] ALL_FIELDS = {
                COLUMN_MEDIA_NAME,
                COLUMN_CREATOR
        };

        public static final String DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS " + TABLE_NAME;

        public static final String CREATE_TABLE_STATEMENT = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_MEDIA_NAME + " STRING PRIMARY KEY,"
                + COLUMN_CREATOR + " STRING"
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
            if (from < 7) {
                // doesn't exist yet
                from++;
                onUpdate(db, from, to);
                return;
            }

            if (from == 7) {
                // table added in version 8
                onCreate(db);
                from++;
                onUpdate(db, from, to);
                return;
            }

            if (from == 8) {
                from++;
                onUpdate(db, from, to);
                return;
            }
        }
    }
}
