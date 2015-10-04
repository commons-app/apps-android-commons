package free.nrw.commons.data;

import android.content.*;
import android.database.sqlite.*;

import free.nrw.commons.campaigns.Campaign;
import free.nrw.commons.category.Category;
import free.nrw.commons.modifications.ModifierSequence;
import free.nrw.commons.contributions.*;

public class DBOpenHelper  extends SQLiteOpenHelper{

    private static final String DATABASE_NAME = "commons.db";
    private static final int DATABASE_VERSION = 6;

    public DBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Contribution.Table.onCreate(sqLiteDatabase);
        ModifierSequence.Table.onCreate(sqLiteDatabase);
        Category.Table.onCreate(sqLiteDatabase);
        Campaign.Table.onCreate(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int from, int to) {
        Contribution.Table.onUpdate(sqLiteDatabase, from, to);
        ModifierSequence.Table.onUpdate(sqLiteDatabase, from, to);
        Category.Table.onUpdate(sqLiteDatabase, from, to);
        Campaign.Table.onUpdate(sqLiteDatabase, from, to);
    }
}
