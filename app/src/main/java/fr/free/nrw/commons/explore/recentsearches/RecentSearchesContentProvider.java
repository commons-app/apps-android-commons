package fr.free.nrw.commons.explore.recentsearches;

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
import static fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao.Table.ALL_FIELDS;
import static fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao.Table.COLUMN_ID;
import static fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao.Table.TABLE_NAME;


/**
 * This class contains functions for executing queries for
 * inserting, searching, deleting, editing recent searches in SqLite DB
 **/
public class RecentSearchesContentProvider extends CommonsDaggerContentProvider {

    // For URI matcher
    private static final int RECENT_SEARCHES = 1;
    private static final int RECENT_SEARCHES_ID = 2;
    private static final String BASE_PATH = "recent_searches";
    public static final Uri BASE_URI = Uri.parse("content://" + BuildConfig.RECENT_SEARCH_AUTHORITY + "/" + BASE_PATH);
    private static final UriMatcher uriMatcher = new UriMatcher(NO_MATCH);

    static {
        uriMatcher.addURI(BuildConfig.RECENT_SEARCH_AUTHORITY, BASE_PATH, RECENT_SEARCHES);
        uriMatcher.addURI(BuildConfig.RECENT_SEARCH_AUTHORITY, BASE_PATH + "/#", RECENT_SEARCHES_ID);
    }

    public static Uri uriForId(int id) {
        return Uri.parse(BASE_URI.toString() + "/" + id);
    }

    @Inject DBOpenHelper dbOpenHelper;

    /**
     * This functions executes query for searching recent searches in SqLite DB
     **/
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
            case RECENT_SEARCHES:
                cursor = queryBuilder.query(db, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case RECENT_SEARCHES_ID:
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

    /**
     * This functions executes query for inserting a recentSearch object in SqLite DB
     **/
    @SuppressWarnings("ConstantConditions")
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = dbOpenHelper.getWritableDatabase();
        long id;
        switch (uriType) {
            case RECENT_SEARCHES:
                id = sqlDB.insert(TABLE_NAME, null, contentValues);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_URI + "/" + id);
    }

    /**
     * This functions executes query for deleting a recentSearch object in SqLite DB
     **/
    @Override
    public int delete(@NonNull Uri uri, String s, String[] strings) {
        int rows;
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        switch (uriType) {
            case RECENT_SEARCHES_ID:
                Timber.d("Deleting recent searches id %s", uri.getLastPathSegment());
                rows = db.delete(RecentSearchesDao.Table.TABLE_NAME,
                        "_id = ?",
                        new String[]{uri.getLastPathSegment()}
                );
                break;
            default:
                throw new IllegalArgumentException("Unknown URI" + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rows;
    }

    /**
     * This functions executes query for inserting multiple recentSearch objects in SqLite DB
     **/
    @SuppressWarnings("ConstantConditions")
    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        Timber.d("Hello, bulk insert! (RecentSearchesContentProvider)");
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = dbOpenHelper.getWritableDatabase();
        sqlDB.beginTransaction();
        switch (uriType) {
            case RECENT_SEARCHES:
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

    /**
     * This functions executes query for updating a particular recentSearch object in SqLite DB
     **/
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
            case RECENT_SEARCHES_ID:
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

