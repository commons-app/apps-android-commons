package org.wikimedia.commons.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.wikimedia.commons.contributions.Contribution;

public class DBOpenHelper  extends SQLiteOpenHelper{

    private static final String DATABASE_NAME = "commons.db";
    private static final int DATABASE_VERSION = 2;

    public DBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Contribution.Table.onCreate(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int from, int to) {
        Contribution.Table.onUpdate(sqLiteDatabase, from, to);
    }
}
