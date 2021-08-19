package fr.free.nrw.commons.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesDao;
import fr.free.nrw.commons.category.CategoryDao;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao;

public class DBOpenHelper  extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "commons.db";
    private static final int DATABASE_VERSION = 18;
    public static final String CONTRIBUTIONS_TABLE = "contributions";
    private final String DROP_TABLE_STATEMENT="DROP TABLE IF EXISTS %s";

    /**
     * Do not use directly - @Inject an instance where it's needed and let
     * dependency injection take care of managing this as a singleton.
     */
    public DBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        CategoryDao.Table.onCreate(sqLiteDatabase);
        BookmarkPicturesDao.Table.onCreate(sqLiteDatabase);
        BookmarkLocationsDao.Table.onCreate(sqLiteDatabase);
        RecentSearchesDao.Table.onCreate(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int from, int to) {
        CategoryDao.Table.onUpdate(sqLiteDatabase, from, to);
        BookmarkPicturesDao.Table.onUpdate(sqLiteDatabase, from, to);
        BookmarkLocationsDao.Table.onUpdate(sqLiteDatabase, from, to);
        RecentSearchesDao.Table.onUpdate(sqLiteDatabase, from, to);
        deleteTable(sqLiteDatabase,CONTRIBUTIONS_TABLE);
    }

    /**
     * Delete table in the given db
     * @param db
     * @param tableName
     */
    public void deleteTable(SQLiteDatabase db, String tableName) {
        try {
            db.execSQL(String.format(DROP_TABLE_STATEMENT, tableName));
            onCreate(db);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }
}
