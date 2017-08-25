package fr.free.nrw.commons.contributions;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import fr.free.nrw.commons.CommonsApplication;
import timber.log.Timber;

public class ContributionsContentProvider extends ContentProvider{

    private static final int CONTRIBUTIONS = 1;
    private static final int CONTRIBUTIONS_ID = 2;

    public static final String AUTHORITY = "fr.free.nrw.commons.contributions.contentprovider";
    private static final String BASE_PATH = "contributions";

    public static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY, BASE_PATH, CONTRIBUTIONS);
        uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", CONTRIBUTIONS_ID);
    }

    public static Uri uriForId(int id) {
        return Uri.parse(BASE_URI.toString() + "/" + id);
    }

    @Inject CommonsApplication application;

    @Override
    public boolean onCreate() {
        AndroidInjection.inject(this);
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(Contribution.Table.TABLE_NAME);

        int uriType = uriMatcher.match(uri);

        SQLiteDatabase db = application.getDBOpenHelper().getReadableDatabase();
        Cursor cursor;

        switch(uriType) {
            case CONTRIBUTIONS:
                cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case CONTRIBUTIONS_ID:
                cursor = queryBuilder.query(db,
                        Contribution.Table.ALL_FIELDS,
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
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = application.getDBOpenHelper().getWritableDatabase();
        long id = 0;
        switch (uriType) {
            case CONTRIBUTIONS:
                id = sqlDB.insert(Contribution.Table.TABLE_NAME, null, contentValues);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_URI + "/" + id);
    }

    @Override
    public int delete(@NonNull Uri uri, String s, String[] strings) {
        int rows = 0;
        int uriType = uriMatcher.match(uri);

        SQLiteDatabase db = application.getDBOpenHelper().getReadableDatabase();

        switch(uriType) {
            case CONTRIBUTIONS_ID:
                Timber.d("Deleting contribution id %s", uri.getLastPathSegment());
                rows = db.delete(Contribution.Table.TABLE_NAME,
                        "_id = ?",
                        new String[] { uri.getLastPathSegment() }
                );
                break;
            default:
                throw new IllegalArgumentException("Unknown URI" + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rows;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        Timber.d("Hello, bulk insert! (ContributionsContentProvider)");
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = application.getDBOpenHelper().getWritableDatabase();
        sqlDB.beginTransaction();
        switch (uriType) {
            case CONTRIBUTIONS:
                for(ContentValues value: values) {
                    Timber.d("Inserting! %s", value);
                    sqlDB.insert(Contribution.Table.TABLE_NAME, null, value);
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
    public int update(@NonNull Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        /*
        SQL Injection warnings: First, note that we're not exposing this to the outside world (exported="false")
        Even then, we should make sure to sanitize all user input appropriately. Input that passes through ContentValues
        should be fine. So only issues are those that pass in via concating.

        In here, the only concat created argument is for id. It is cast to an int, and will error out otherwise.
         */
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = application.getDBOpenHelper().getWritableDatabase();
        int rowsUpdated = 0;
        switch (uriType) {
            case CONTRIBUTIONS:
                rowsUpdated = sqlDB.update(Contribution.Table.TABLE_NAME,
                        contentValues,
                        selection,
                        selectionArgs);
                break;
            case CONTRIBUTIONS_ID:
                int id = Integer.valueOf(uri.getLastPathSegment());

                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(Contribution.Table.TABLE_NAME,
                            contentValues,
                            Contribution.Table.COLUMN_ID + " = ?",
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
