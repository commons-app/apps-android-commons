package free.nrw.commons.category;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.RemoteException;

import java.util.Date;

public class Category {
    private ContentProviderClient client;
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

    public Date getLastUsed() {
        // warning: Date objects are mutable.
        return (Date)lastUsed.clone();
    }

    public void setLastUsed(Date lastUsed) {
        // warning: Date objects are mutable.
        this.lastUsed = (Date)lastUsed.clone();
    }

    public void touch() {
        lastUsed = new Date();
    }

    public int getTimesUsed() {
        return timesUsed;
    }

    public void setTimesUsed(int timesUsed) {
        this.timesUsed = timesUsed;
    }

    public void incTimesUsed() {
        timesUsed++;
        touch();
    }

    // Database/content-provider stuff
    public void setContentProviderClient(ContentProviderClient client) {
        this.client = client;
    }

    public void save() {
        try {
            if(contentUri == null) {
                contentUri = client.insert(CategoryContentProvider.BASE_URI, this.toContentValues());
            } else {
                client.update(contentUri, toContentValues(), null, null);
            }
        } catch(RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Table.COLUMN_NAME, getName());
        cv.put(Table.COLUMN_LAST_USED, getLastUsed().getTime());
        cv.put(Table.COLUMN_TIMES_USED, getTimesUsed());
        return cv;
    }

    public static Category fromCursor(Cursor cursor) {
        // Hardcoding column positions!
        Category c = new Category();
        c.contentUri = CategoryContentProvider.uriForId(cursor.getInt(0));
        c.name = cursor.getString(1);
        c.lastUsed = new Date(cursor.getLong(2));
        c.timesUsed = cursor.getInt(3);
        return c;
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

        public static void onUpdate(SQLiteDatabase db, int from, int to) {
            if(from == to) {
                return;
            }
            if(from < 4) {
                // doesn't exist yet
                from++;
                onUpdate(db, from, to);
                return;
            }
            if(from == 4) {
                // table added in version 5
                onCreate(db);
                from++;
                onUpdate(db, from, to);
                return;
            }
            if(from == 5) {
                from++;
                onUpdate(db, from, to);
                return;
            }
        }
    }
}
