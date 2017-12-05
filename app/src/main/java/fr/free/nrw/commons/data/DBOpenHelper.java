package fr.free.nrw.commons.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.modifications.ModifierSequence;

public class DBOpenHelper  extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "commons.db";
    private static final int DATABASE_VERSION = 6;

    /**
     * Do not use, please call CommonsApplication.getDBOpenHelper()
     */
    public DBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Contribution.Table.onCreate(sqLiteDatabase);
        ModifierSequence.Table.onCreate(sqLiteDatabase);
        Category.Table.onCreate(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int from, int to) {
        Contribution.Table.onUpdate(sqLiteDatabase, from, to);
        ModifierSequence.Table.onUpdate(sqLiteDatabase, from, to);
        Category.Table.onUpdate(sqLiteDatabase, from, to);
    }
}
