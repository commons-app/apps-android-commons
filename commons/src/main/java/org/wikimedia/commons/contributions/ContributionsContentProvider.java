package org.wikimedia.commons.contributions;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import org.wikimedia.commons.CommonsApplication;
import org.wikimedia.commons.data.DBOpenHelper;

public class ContributionsContentProvider extends ContentProvider{

    private static final int CONTRIBUTIONS = 1;

    public static final String AUTHORITY = "org.wikimedia.commons.contributions.contentprovider";
    private static final String BASE_PATH = "contributions";

    public static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY, BASE_PATH, CONTRIBUTIONS);
    }


    private DBOpenHelper dbOpenHelper;
    @Override
    public boolean onCreate() {
        dbOpenHelper = ((CommonsApplication)this.getContext().getApplicationContext()).getDbOpenHelper();
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(Contribution.Table.TABLE_NAME);

        int uriType = uriMatcher.match(uri);

        switch(uriType) {
            case CONTRIBUTIONS:
                break;
            default:
                throw new IllegalArgumentException("Unknown URI" + uri);
        }

        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();

        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = dbOpenHelper.getWritableDatabase();
        int rowsDeleted = 0;
        long id = 0;
        switch (uriType) {
            case CONTRIBUTIONS:
                id = sqlDB.insert(Contribution.Table.TABLE_NAME, null, contentValues);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }
}
