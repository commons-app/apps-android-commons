package org.wikimedia.commons.category;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import org.wikimedia.commons.CommonsApplication;
import org.wikimedia.commons.data.DBOpenHelper;

/**
 * Created with IntelliJ IDEA.
 * User: brion
 * Date: 4/22/13
 * Time: 4:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class CategoryContentProvider extends ContentProvider {

    // ????
    private static final int CATEGORIES = 1;
    private static final int CATEGORIES_ID = 2;

    public static final String AUTHORITY = "org.wikimedia.commons.categories.contentprovider";
    private static final String BASE_PATH = "categories";

    public static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY, BASE_PATH, CATEGORIES);
        uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", CATEGORIES_ID);
    }


    public static Uri uriForId(int id) {
        return Uri.parse(BASE_URI.toString() + "/" + id);
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
        queryBuilder.setTables(Category.Table.TABLE_NAME);

        int uriType = uriMatcher.match(uri);

        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        Cursor cursor;

        switch(uriType) {
            case CATEGORIES:
                cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case CATEGORIES_ID:
                cursor = queryBuilder.query(db,
                        Category.Table.ALL_FIELDS,
                        "_id = ?",
                        new String[] { uri.getLastPathSegment() },
                        null,
                        null,
                        sortOrder
                );
                break;
            default:
                throw new IllegalArgumentException("Unknown URI" + uri);
        }

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
        long id = 0;
        switch (uriType) {
            case CATEGORIES:
                id = sqlDB.insert(Category.Table.TABLE_NAME, null, contentValues);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_URI + "/" + id);
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        Log.d("Commons", "Hello, bulk insert!");
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = dbOpenHelper.getWritableDatabase();
        sqlDB.beginTransaction();
        switch (uriType) {
            case CATEGORIES:
                for(ContentValues value: values) {
                    Log.d("Commons", "Inserting! " + value.toString());
                    sqlDB.insert(Category.Table.TABLE_NAME, null, value);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        sqlDB.setTransactionSuccessful();
        sqlDB.endTransaction();
        getContext().getContentResolver().notifyChange(uri, null);
        return values.length;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        /*
        SQL Injection warnings: First, note that we're not exposing this to the outside world (exported="false")
        Even then, we should make sure to sanitize all user input appropriately. Input that passes through ContentValues
        should be fine. So only issues are those that pass in via concating.

        In here, the only concat created argument is for id. It is cast to an int, and will error out otherwise.
         */
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = dbOpenHelper.getWritableDatabase();
        int rowsUpdated = 0;
        switch (uriType) {
            case CATEGORIES:
                rowsUpdated = sqlDB.update(Category.Table.TABLE_NAME,
                        contentValues,
                        selection,
                        selectionArgs);
                break;
            case CATEGORIES_ID:
                int id = Integer.valueOf(uri.getLastPathSegment());

                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(Category.Table.TABLE_NAME,
                            contentValues,
                            Category.Table.COLUMN_ID + " = ?",
                            new String[] { String.valueOf(id) } );
                } else {
                    throw new IllegalArgumentException("Parameter `selection` should be empty when updating an ID");
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri + " with type " + uriType);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }
}

