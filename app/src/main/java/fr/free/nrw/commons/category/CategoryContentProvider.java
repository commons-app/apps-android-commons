package fr.free.nrw.commons.category;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import javax.inject.Inject;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.data.DBOpenHelper;
import fr.free.nrw.commons.di.CommonsDaggerContentProvider;
import timber.log.Timber;

import static android.content.UriMatcher.NO_MATCH;
import static fr.free.nrw.commons.category.CategoryDao.Table.ALL_FIELDS;
import static fr.free.nrw.commons.category.CategoryDao.Table.COLUMN_ID;
import static fr.free.nrw.commons.category.CategoryDao.Table.TABLE_NAME;

public class CategoryContentProvider extends CommonsDaggerContentProvider {

    // For URI matcher
    private static final int CATEGORIES = 1;
    private static final int CATEGORIES_ID = 2;
    private static final String BASE_PATH = "categories";

    public static final Uri BASE_URI = Uri.parse("content://" + BuildConfig.CATEGORY_AUTHORITY + "/" + BASE_PATH);

    private static final UriMatcher uriMatcher = new UriMatcher(NO_MATCH);

    static {
        uriMatcher.addURI(BuildConfig.CATEGORY_AUTHORITY, BASE_PATH, CATEGORIES);
        uriMatcher.addURI(BuildConfig.CATEGORY_AUTHORITY, BASE_PATH + "/#", CATEGORIES_ID);
    }

    public static Uri uriForId(int id) {
        return Uri.parse(BASE_URI.toString() + "/" + id);
    }

    @Inject DBOpenHelper dbOpenHelper;

    @SuppressWarnings("ConstantConditions")
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TABLE_NAME);

        int uriType = uriMatcher.match(uri);

        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        Cursor cursor;

        switch (uriType) {
            case CATEGORIES:
                cursor = queryBuilder.query(db, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case CATEGORIES_ID:
                cursor = queryBuilder.query(db,
                        ALL_FIELDS,
                        "_id = ?",
                        new String[]{uri.getLastPathSegment()},
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
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = dbOpenHelper.getWritableDatabase();
        long id;
        switch (uriType) {
            case CATEGORIES:
                id = sqlDB.insert(TABLE_NAME, null, contentValues);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_URI + "/" + id);
    }

    @Override
    public int delete(@NonNull Uri uri, String s, String[] strings) {
        return 0;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        Timber.d("Hello, bulk insert! (CategoryContentProvider)");
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = dbOpenHelper.getWritableDatabase();
        sqlDB.beginTransaction();
        switch (uriType) {
            case CATEGORIES:
                for (ContentValues value : values) {
                    Timber.d("Inserting! %s", value);
                    sqlDB.insert(TABLE_NAME, null, value);
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

    @SuppressWarnings("ConstantConditions")
    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        /*
        SQL Injection warnings: First, note that we're not exposing this to the
        outside world (exported="false"). Even then, we should make sure to sanitize
        all user input appropriately. Input that passes through ContentValues
        should be fine. So only issues are those that pass in via concating.

        In here, the only concat created argument is for id. It is cast to an int,
        and will error out otherwise.
         */
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = dbOpenHelper.getWritableDatabase();
        int rowsUpdated;
        switch (uriType) {
            case CATEGORIES_ID:
                if (TextUtils.isEmpty(selection)) {
                    int id = Integer.valueOf(uri.getLastPathSegment());
                    rowsUpdated = sqlDB.update(TABLE_NAME,
                            contentValues,
                            COLUMN_ID + " = ?",
                            new String[]{String.valueOf(id)});
                } else {
                    throw new IllegalArgumentException(
                            "Parameter `selection` should be empty when updating an ID");
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri + " with type " + uriType);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }
}

