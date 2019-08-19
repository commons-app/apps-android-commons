package fr.free.nrw.commons.upload.structure.depictions;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import fr.free.nrw.commons.data.DBOpenHelper;
import fr.free.nrw.commons.di.CommonsDaggerContentProvider;
import timber.log.Timber;

import static fr.free.nrw.commons.BuildConfig.DEPICTION_AUTHORITY;
import static fr.free.nrw.commons.upload.structure.depictions.DepictionDao.Table.ALL_FIELDS;
import static fr.free.nrw.commons.upload.structure.depictions.DepictionDao.Table.COLUMN_ID;
import static fr.free.nrw.commons.upload.structure.depictions.DepictionDao.Table.TABLE_NAME;


@SuppressLint("Registered")
public class DepictsContentProvider extends CommonsDaggerContentProvider {

    private static final int DEPICTS = 1;
    private static final int DEPICTS_ID = 2;
    private static final String BASE_PATH = "depictions";
    public static final Uri BASE_URI = Uri.parse("content://" + DEPICTION_AUTHORITY + "/" + BASE_PATH);

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(DEPICTION_AUTHORITY, BASE_PATH, DEPICTS);
        uriMatcher.addURI(DEPICTION_AUTHORITY, BASE_PATH + "/#", DEPICTS_ID);
    }

    @Inject
    DBOpenHelper dbOpenHelper;

    public static Uri uriForId(int id) {
        return Uri.parse(BASE_URI.toString() + "/" + id);
    }

    @Override
    public Cursor query(@NotNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TABLE_NAME);

        int uriType = uriMatcher.match(uri);

        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        Cursor cursor;

        switch (uriType) {
            case DEPICTS:
                cursor = queryBuilder.query(db, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case DEPICTS_ID:
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
    public String getType(@NotNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NotNull Uri uri, ContentValues values) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = dbOpenHelper.getWritableDatabase();
        long id;
        switch (uriType) {
            case DEPICTS:
                id = sqlDB.insert(TABLE_NAME, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_URI + "/" + id);
    }

    @Override
    public int delete(@NotNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int bulkInsert(@NotNull Uri uri, @NotNull ContentValues[] values) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = dbOpenHelper.getWritableDatabase();
        sqlDB.beginTransaction();
        switch (uriType) {
            case DEPICTS:
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

    @Override
    public int update(@NotNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = dbOpenHelper.getWritableDatabase();
        int rowsUpdated;
        switch (uriType) {
            case DEPICTS:
                if (TextUtils.isEmpty(selection)) {
                    int id = Integer.valueOf(uri.getLastPathSegment());
                    rowsUpdated = sqlDB.update(TABLE_NAME,
                            values,
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