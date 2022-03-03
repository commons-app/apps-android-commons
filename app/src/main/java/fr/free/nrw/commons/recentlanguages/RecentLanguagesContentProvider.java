package fr.free.nrw.commons.recentlanguages;

import static fr.free.nrw.commons.recentlanguages.RecentLanguagesDao.Table.COLUMN_NAME;
import static fr.free.nrw.commons.recentlanguages.RecentLanguagesDao.Table.TABLE_NAME;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.data.DBOpenHelper;
import fr.free.nrw.commons.di.CommonsDaggerContentProvider;
import javax.inject.Inject;
import timber.log.Timber;

public class RecentLanguagesContentProvider extends CommonsDaggerContentProvider {

    private static final String BASE_PATH = "recent_languages";
    public static final Uri BASE_URI =
        Uri.parse("content://" + BuildConfig.RECENT_LANGUAGE_AUTHORITY + "/" + BASE_PATH);


    /**
     * Append bookmark items ID to the base uri
     */
    public static Uri uriForName(final String id) {
        return Uri.parse(BASE_URI + "/" + id);
    }

    @Inject
    DBOpenHelper dbOpenHelper;

    @Override
    public String getType(@NonNull final Uri uri) {
        return null;
    }

    /**
     * Queries the SQLite database for the bookmark items
     * @param uri : contains the uri for bookmark items
     * @param projection : contains the all fields of the table
     * @param selection : handles Where
     * @param selectionArgs : the condition of Where clause
     * @param sortOrder : ascending or descending
     */
    @SuppressWarnings("ConstantConditions")
    @Override
    public Cursor query(@NonNull final Uri uri, final String[] projection, final String selection,
        final String[] selectionArgs, final String sortOrder) {
        final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TABLE_NAME);
        final SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        final Cursor cursor = queryBuilder.query(db, projection, selection,
            selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    /**
     * Handles the update query of local SQLite Database
     * @param uri : contains the uri for bookmark items
     * @param contentValues : new values to be entered to db
     * @param selection : handles Where
     * @param selectionArgs : the condition of Where clause
     */
    @SuppressWarnings("ConstantConditions")
    @Override
    public int update(@NonNull final Uri uri, final ContentValues contentValues,
        final String selection, final String[] selectionArgs) {
        final SQLiteDatabase sqlDB = dbOpenHelper.getWritableDatabase();
        final int rowsUpdated;
        if (TextUtils.isEmpty(selection)) {
            final int id = Integer.parseInt(uri.getLastPathSegment());
            rowsUpdated = sqlDB.update(TABLE_NAME,
                contentValues,
                COLUMN_NAME + " = ?",
                new String[]{String.valueOf(id)});
        } else {
            throw new IllegalArgumentException(
                "Parameter `selection` should be empty when updating an ID");
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    /**
     * Handles the insertion of new bookmark items record to local SQLite Database
     * @param uri
     * @param contentValues
     * @return
     */
    @SuppressWarnings("ConstantConditions")
    @Override
    public Uri insert(@NonNull final Uri uri, final ContentValues contentValues) {
        final SQLiteDatabase sqlDB = dbOpenHelper.getWritableDatabase();
        final long id = sqlDB.insert(TABLE_NAME, null, contentValues);
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_URI + "/" + id);
    }

    /**
     * Handles the deletion of new bookmark items record to local SQLite Database
     * @param uri
     * @param s
     * @param strings
     * @return
     */
    @SuppressWarnings("ConstantConditions")
    @Override
    public int delete(@NonNull final Uri uri, final String s, final String[] strings) {
        final int rows;
        final SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        Timber.d("Deleting bookmark name %s", uri.getLastPathSegment());
        rows = db.delete(
            TABLE_NAME,
            "language_code = ?",
            new String[]{uri.getLastPathSegment()}
        );
        getContext().getContentResolver().notifyChange(uri, null);
        return rows;
    }
}