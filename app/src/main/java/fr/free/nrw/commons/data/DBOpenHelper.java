package fr.free.nrw.commons.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import fr.free.nrw.commons.category.CategoryDao;
import fr.free.nrw.commons.contributions.ContributionDao;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao;
import fr.free.nrw.commons.modifications.ModifierSequenceDao;

public class DBOpenHelper  extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "commons.db";
    private static final int DATABASE_VERSION = 7;

    /**
     * Do not use directly - @Inject an instance where it's needed and let
     * dependency injection take care of managing this as a singleton.
     */
    public DBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        ContributionDao.Table.onCreate(sqLiteDatabase);
        ModifierSequenceDao.Table.onCreate(sqLiteDatabase);
        CategoryDao.Table.onCreate(sqLiteDatabase);
        RecentSearchesDao.Table.onCreate(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int from, int to) {
        ContributionDao.Table.onUpdate(sqLiteDatabase, from, to);
        ModifierSequenceDao.Table.onUpdate(sqLiteDatabase, from, to);
        CategoryDao.Table.onUpdate(sqLiteDatabase, from, to);
        RecentSearchesDao.Table.onUpdate(sqLiteDatabase, from, to);
    }
}
