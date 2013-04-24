package org.wikimedia.commons.category;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.text.TextUtils;
import org.wikimedia.commons.contributions.ContributionsContentProvider;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: brion
 * Date: 4/22/13
 * Time: 3:37 PM
 * To change this template use File | Settings | File Templates.
 */
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

    public void setName(String name_) {
        name = name_;
    }

    public Date getLastUsed() {
        // warning: Date objects are mutable.
        return (Date)lastUsed.clone();
    }

    public void setLastUsed(Date lastUsed_) {
        // warning: Date objects are mutable.
        lastUsed = (Date)lastUsed_.clone();
    }

    public void touch() {
        lastUsed = new Date();
    }

    public int getTimesUsed() {
        return timesUsed;
    }

    public void setTimesUsed(int timesUsed_) {
        timesUsed = timesUsed_;
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
                + COLUMN_LAST_USED + " INTEGER," // Will this roll over in 2038? :)
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
            }
        }
    }
}
