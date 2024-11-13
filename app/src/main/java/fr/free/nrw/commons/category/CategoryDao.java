package fr.free.nrw.commons.category;

import android.annotation.SuppressLint;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

public class CategoryDao {

    private final Provider<ContentProviderClient> clientProvider;

    @Inject
    public CategoryDao(@Named("category") Provider<ContentProviderClient> clientProvider) {
        this.clientProvider = clientProvider;
    }

    public void save(Category category) {
        ContentProviderClient db = clientProvider.get();
        try {
            if (category.getContentUri() == null) {
                category.setContentUri(db.insert(CategoryContentProvider.BASE_URI, toContentValues(category)));
            } else {
                db.update(category.getContentUri(), toContentValues(category), null, null);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
    }

    /**
     * Find persisted category in database, based on its name.
     *
     * @param name Category's name
     * @return category from database, or null if not found
     */
    @Nullable
    Category find(String name) {
        Cursor cursor = null;
        ContentProviderClient db = clientProvider.get();
        try {
            cursor = db.query(
                    CategoryContentProvider.BASE_URI,
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
     * Retrieve recently-used categories, ordered by descending date.
     *
     * @return a list containing recent categories
     */
    @NonNull
    List<CategoryItem> recentCategories(int limit) {
        List<CategoryItem> items = new ArrayList<>();
        Cursor cursor = null;
        ContentProviderClient db = clientProvider.get();
        try {
            cursor = db.query(
                    CategoryContentProvider.BASE_URI,
                    Table.ALL_FIELDS,
                    null,
                    new String[]{},
                    Table.COLUMN_LAST_USED + " DESC");
            // fixme add a limit on the original query instead of falling out of the loop?
            while (cursor != null && cursor.moveToNext()
                    && cursor.getPosition() < limit) {
                if (fromCursor(cursor).getName() != null ) {
                    items.add(new CategoryItem(fromCursor(cursor).getName(),
                        fromCursor(cursor).getDescription(), fromCursor(cursor).getThumbnail(),
                        false));
                }
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
    @SuppressLint("Range")
    Category fromCursor(Cursor cursor) {
        // Hardcoding column positions!
        return new Category(
                CategoryContentProvider.uriForId(cursor.getInt(cursor.getColumnIndex(Table.COLUMN_ID))),
                cursor.getString(cursor.getColumnIndex(Table.COLUMN_NAME)),
                cursor.getString(cursor.getColumnIndex(Table.COLUMN_DESCRIPTION)),
                cursor.getString(cursor.getColumnIndex(Table.COLUMN_THUMBNAIL)),
                new Date(cursor.getLong(cursor.getColumnIndex(Table.COLUMN_LAST_USED))),
                cursor.getInt(cursor.getColumnIndex(Table.COLUMN_TIMES_USED))
        );
    }

    private ContentValues toContentValues(Category category) {
        ContentValues cv = new ContentValues();
        cv.put(CategoryDao.Table.COLUMN_NAME, category.getName());
        cv.put(Table.COLUMN_DESCRIPTION, category.getDescription());
        cv.put(Table.COLUMN_THUMBNAIL, category.getThumbnail());
        cv.put(CategoryDao.Table.COLUMN_LAST_USED, category.getLastUsed().getTime());
        cv.put(CategoryDao.Table.COLUMN_TIMES_USED, category.getTimesUsed());
        return cv;
    }

    public static class Table {
        public static final String TABLE_NAME = "categories";

        public static final String COLUMN_ID = "_id";
        static final String COLUMN_NAME = "name";
        static final String COLUMN_DESCRIPTION = "description";
        static final String COLUMN_THUMBNAIL = "thumbnail";
        static final String COLUMN_LAST_USED = "last_used";
        static final String COLUMN_TIMES_USED = "times_used";

        // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
        public static final String[] ALL_FIELDS = {
                COLUMN_ID,
                COLUMN_NAME,
                COLUMN_DESCRIPTION,
                COLUMN_THUMBNAIL,
                COLUMN_LAST_USED,
                COLUMN_TIMES_USED
        };

        static final String DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS " + TABLE_NAME;

        static final String CREATE_TABLE_STATEMENT = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_NAME + " STRING,"
                + COLUMN_DESCRIPTION + " STRING,"
                + COLUMN_THUMBNAIL + " STRING,"
                + COLUMN_LAST_USED + " INTEGER,"
                + COLUMN_TIMES_USED + " INTEGER"
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
            if (from < 4) {
                // doesn't exist yet
                from++;
                onUpdate(db, from, to);
                return;
            }
            if (from == 4) {
                // table added in version 5
                onCreate(db);
                from++;
                onUpdate(db, from, to);
                return;
            }
            if (from == 5) {
                from++;
                onUpdate(db, from, to);
                return;
            }
            if (from == 17) {
                db.execSQL("ALTER TABLE categories ADD COLUMN description STRING;");
                db.execSQL("ALTER TABLE categories ADD COLUMN thumbnail STRING;");
                from++;
                onUpdate(db, from, to);
                return;
            }
        }
    }
}
