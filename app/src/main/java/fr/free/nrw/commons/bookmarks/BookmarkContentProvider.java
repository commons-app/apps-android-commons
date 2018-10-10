package fr.free.nrw.commons.bookmarks;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import javax.inject.Inject;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.category.CategoryDao;
import fr.free.nrw.commons.data.DBOpenHelper;
import fr.free.nrw.commons.di.CommonsDaggerContentProvider;
import timber.log.Timber;

import static android.content.UriMatcher.NO_MATCH;
import static fr.free.nrw.commons.bookmarks.BookmarkDao.Table.ALL_FIELDS;
import static fr.free.nrw.commons.bookmarks.BookmarkDao.Table.COLUMN_MEDIA_NAME;
import static fr.free.nrw.commons.bookmarks.BookmarkDao.Table.TABLE_NAME;

public class BookmarkContentProvider extends CommonsDaggerContentProvider {

    private static final String BASE_PATH = "bookmarks";
    public static final Uri BASE_URI = Uri.parse("content://" + BuildConfig.BOOKMARK_AUTHORITY + "/" + BASE_PATH);

    public static Uri uriForName(String name) {
        return Uri.parse(BASE_URI.toString() + "/" + name);
    }

    @Inject DBOpenHelper dbOpenHelper;

    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TABLE_NAME);

        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        SQLiteDatabase sqlDB = dbOpenHelper.getWritableDatabase();
        int rowsUpdated;
        if (TextUtils.isEmpty(selection)) {
            int id = Integer.valueOf(uri.getLastPathSegment());
            rowsUpdated = sqlDB.update(TABLE_NAME,
                    contentValues,
                    COLUMN_MEDIA_NAME + " = ?",
                    new String[]{String.valueOf(id)});
        } else {
            throw new IllegalArgumentException(
                    "Parameter `selection` should be empty when updating an ID");
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        SQLiteDatabase sqlDB = dbOpenHelper.getWritableDatabase();
        long id = sqlDB.insert(BookmarkDao.Table.TABLE_NAME, null, contentValues);
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_URI + "/" + id);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public int delete(@NonNull Uri uri, String s, String[] strings) {
        int rows;
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        Timber.d("Deleting bookmark name %s", uri.getLastPathSegment());
        rows = db.delete(TABLE_NAME,
                "media_name = ?",
                new String[]{uri.getLastPathSegment()}
        );
        getContext().getContentResolver().notifyChange(uri, null);
        return rows;
    }
}
