package fr.free.nrw.commons.bookmarks;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;


import static fr.free.nrw.commons.bookmarks.BookmarkContentProvider.BASE_URI;

public class BookmarkDao {

    private final Provider<ContentProviderClient> clientProvider;

    @Inject
    public BookmarkDao(@Named("bookmark") Provider<ContentProviderClient> clientProvider) {
        this.clientProvider = clientProvider;
    }


    @NonNull
    public List<Bookmark> getAllBookmarks() {
        List<Bookmark> items = new ArrayList<>();
        Cursor cursor = null;
        ContentProviderClient db = clientProvider.get();
        try {
            cursor = db.query(
                    BookmarkContentProvider.BASE_URI,
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

    public boolean updateBookmark(Bookmark bookmark) {
        boolean bookmarkExists = findBookmark(bookmark.getMediaName());
        if(bookmarkExists) {
            deleteBookmark(bookmark);
        }
        else {
            addBookmark(bookmark);
        }
        return !bookmarkExists;
    }

    private void addBookmark(Bookmark bookmark) {
        ContentProviderClient db = clientProvider.get();
        try {
            if (bookmark.getContentUri() == null) {
                bookmark.setContentUri(db.insert(BASE_URI, toContentValues(bookmark)));
            } else {
                db.update(bookmark.getContentUri(), toContentValues(bookmark), null, null);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
    }

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

    @Nullable
    public boolean findBookmark(String bookmarkName) {
            Cursor cursor = null;
            ContentProviderClient db = clientProvider.get();
            try {
                cursor = db.query(
                        BookmarkContentProvider.BASE_URI,
                        Table.ALL_FIELDS,
                        Table.COLUMN_MEDIA_NAME + "=?",
                        new String[]{bookmarkName},
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
                BookmarkContentProvider.uriForName(cursor.getString(cursor.getColumnIndex(Table.COLUMN_MEDIA_NAME))),
                cursor.getString(cursor.getColumnIndex(Table.COLUMN_MEDIA_NAME)),
                cursor.getString(cursor.getColumnIndex(Table.COLUMN_CREATOR)),
                cursor.getString(cursor.getColumnIndex(Table.COLUMN_CREATION_DATE))
        );
    }

    private ContentValues toContentValues(Bookmark bookmark) {
        ContentValues cv = new ContentValues();
        cv.put(BookmarkDao.Table.COLUMN_MEDIA_NAME, bookmark.getMediaName());
        cv.put(BookmarkDao.Table.COLUMN_CREATOR, bookmark.getMediaCreator());
        cv.put(BookmarkDao.Table.COLUMN_CREATION_DATE, bookmark.getMediaCreationDate());
        return cv;
    }


    public static class Table {
        public static final String TABLE_NAME = "bookmarks";

        static final String COLUMN_MEDIA_NAME = "media_name";
        static final String COLUMN_CREATOR = "media_creator";
        static final String COLUMN_CREATION_DATE = "creation_date";

        // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
        public static final String[] ALL_FIELDS = {
                COLUMN_MEDIA_NAME,
                COLUMN_CREATOR,
                COLUMN_CREATION_DATE,
        };

        static final String DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS " + TABLE_NAME;

        static final String CREATE_TABLE_STATEMENT = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_MEDIA_NAME + " STRING PRIMARY KEY,"
                + COLUMN_CREATOR + " STRING,"
                + COLUMN_CREATION_DATE + " STRING"
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
            if (from == 1) {
                //db.execSQL(ADD_DESCRIPTION_FIELD);
                //db.execSQL(ADD_CREATOR_FIELD);
                from++;
                onUpdate(db, from, to);
                return;
            }
            if (from == 2) {
                //db.execSQL(ADD_MULTIPLE_FIELD);
                //db.execSQL(SET_DEFAULT_MULTIPLE);
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
                //db.execSQL(ADD_WIDTH_FIELD);
                //db.execSQL(SET_DEFAULT_WIDTH);
                //db.execSQL(ADD_HEIGHT_FIELD);
                //db.execSQL(SET_DEFAULT_HEIGHT);
                //db.execSQL(ADD_LICENSE_FIELD);
                //db.execSQL(SET_DEFAULT_LICENSE);
                from++;
                onUpdate(db, from, to);
                return;
            }
        }
    }
}
