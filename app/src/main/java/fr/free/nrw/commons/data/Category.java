package fr.free.nrw.commons.data;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;

import fr.free.nrw.commons.category.CategoryContentProvider;

public class Category {
    private Uri contentUri;

    private String name;
    private Date lastUsed;
    private int timesUsed;

    // Getters/setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private Date getLastUsed() {
        // warning: Date objects are mutable.
        return (Date)lastUsed.clone();
    }

    public void setLastUsed(Date lastUsed) {
        // warning: Date objects are mutable.
        this.lastUsed = (Date)lastUsed.clone();
    }

    private void touch() {
        lastUsed = new Date();
    }

    private int getTimesUsed() {
        return timesUsed;
    }

    public void setTimesUsed(int timesUsed) {
        this.timesUsed = timesUsed;
    }

    public void incTimesUsed() {
        timesUsed++;
        touch();
    }

    //region Database/content-provider stuff

    /**
     * Persist category.
     * @param client ContentProviderClient to handle DB connection
     */
    public void save(ContentProviderClient client) {
        try {
            if (contentUri == null) {
                contentUri = client.insert(CategoryContentProvider.BASE_URI, this.toContentValues());
            } else {
                client.update(contentUri, toContentValues(), null, null);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Table.COLUMN_NAME, getName());
        cv.put(Table.COLUMN_LAST_USED, getLastUsed().getTime());
        cv.put(Table.COLUMN_TIMES_USED, getTimesUsed());
        return cv;
    }

    private static Category fromCursor(Cursor cursor) {
        // Hardcoding column positions!
        Category c = new Category();
        c.contentUri = CategoryContentProvider.uriForId(cursor.getInt(0));
        c.name = cursor.getString(1);
        c.lastUsed = new Date(cursor.getLong(2));
        c.timesUsed = cursor.getInt(3);
        return c;
    }

    /**
     * Find persisted category in database, based on its name.
     * @param client ContentProviderClient to handle DB connection
     * @param name Category's name
     * @return category from database, or null if not found
     */
    public static @Nullable Category find(ContentProviderClient client, String name) {
        Cursor cursor = null;
        try {
            cursor = client.query(
                    CategoryContentProvider.BASE_URI,
                    Category.Table.ALL_FIELDS,
                    Category.Table.COLUMN_NAME + "=?",
                    new String[]{name},
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                return Category.fromCursor(cursor);
            }
        } catch (RemoteException e) {
            // This feels lazy, but to hell with checked exceptions. :)
            throw new RuntimeException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * Retrieve recently-used categories, ordered by descending date.
     * @return a list containing recent categories
     */
    public static @NonNull ArrayList<String> recentCategories(ContentProviderClient client, int limit) {
        ArrayList<String> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = client.query(
                    CategoryContentProvider.BASE_URI,
                    Category.Table.ALL_FIELDS,
                    null,
                    new String[]{},
                    Category.Table.COLUMN_LAST_USED + " DESC");
            // fixme add a limit on the original query instead of falling out of the loop?
            while (cursor != null && cursor.moveToNext()
                    && cursor.getPosition() < limit) {
                Category cat = Category.fromCursor(cursor);
                items.add(cat.getName());
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return items;
    }

    public static class Table {
        public static final String TABLE_NAME = "categories";

        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_LAST_USED = "last_used";
        public static final String COLUMN_TIMES_USED = "times_used";

        // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
        public static final String[] ALL_FIELDS = {
                COLUMN_ID,
                COLUMN_NAME,
                COLUMN_LAST_USED,
                COLUMN_TIMES_USED
        };

        private static final String CREATE_TABLE_STATEMENT = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_NAME + " STRING,"
                + COLUMN_LAST_USED + " INTEGER,"
                + COLUMN_TIMES_USED + " INTEGER"
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
        }
    }
    //endregion
}
