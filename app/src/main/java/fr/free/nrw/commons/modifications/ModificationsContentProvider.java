package fr.free.nrw.commons.modifications;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import fr.free.nrw.commons.CommonsApplication;
import timber.log.Timber;

public class ModificationsContentProvider extends ContentProvider {

    private static final int MODIFICATIONS = 1;
    private static final int MODIFICATIONS_ID = 2;

    public static final String AUTHORITY = "fr.free.nrw.commons.modifications.contentprovider";
    private static final String BASE_PATH = "modifications";

    public static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY, BASE_PATH, MODIFICATIONS);
        uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", MODIFICATIONS_ID);
    }

    public static Uri uriForId(int id) {
        return Uri.parse(BASE_URI.toString() + "/" + id);
    }


    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(ModifierSequence.Table.TABLE_NAME);

        int uriType = uriMatcher.match(uri);

        switch (uriType) {
            case MODIFICATIONS:
                break;
            default:
                throw new IllegalArgumentException("Unknown URI" + uri);
        }

        SQLiteDatabase db = CommonsApplication.getInstance().getDBOpenHelper().getReadableDatabase();

        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
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
        SQLiteDatabase sqlDB = CommonsApplication.getInstance().getDBOpenHelper().getWritableDatabase();
        long id = 0;
        switch (uriType) {
            case MODIFICATIONS:
                id = sqlDB.insert(ModifierSequence.Table.TABLE_NAME, null, contentValues);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_URI + "/" + id);
    }

    @Override
    public int delete(@NonNull Uri uri, String s, String[] strings) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = CommonsApplication.getInstance().getDBOpenHelper().getWritableDatabase();
        switch (uriType) {
            case MODIFICATIONS_ID:
                String id = uri.getLastPathSegment();
                sqlDB.delete(ModifierSequence.Table.TABLE_NAME,
                        "_id = ?",
                        new String[] { id }
                        );
                return 1;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        Timber.d("Hello, bulk insert! (ModificationsContentProvider)");
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = CommonsApplication.getInstance().getDBOpenHelper().getWritableDatabase();
        sqlDB.beginTransaction();
        switch (uriType) {
            case MODIFICATIONS:
                for (ContentValues value: values) {
                    Timber.d("Inserting! %s", value);
                    sqlDB.insert(ModifierSequence.Table.TABLE_NAME, null, value);
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
        SQLiteDatabase sqlDB = CommonsApplication.getInstance().getDBOpenHelper().getWritableDatabase();
        int rowsUpdated = 0;
        switch (uriType) {
            case MODIFICATIONS:
                rowsUpdated = sqlDB.update(ModifierSequence.Table.TABLE_NAME,
                        contentValues,
                        selection,
                        selectionArgs);
                break;
            case MODIFICATIONS_ID:
                int id = Integer.valueOf(uri.getLastPathSegment());

                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(ModifierSequence.Table.TABLE_NAME,
                            contentValues,
                            ModifierSequence.Table.COLUMN_ID + " = ?",
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
