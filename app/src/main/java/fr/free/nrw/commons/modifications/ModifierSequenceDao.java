package fr.free.nrw.commons.modifications;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.RemoteException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

public class ModifierSequenceDao {

    private final Provider<ContentProviderClient> clientProvider;

    @Inject
    public ModifierSequenceDao(@Named("modification") Provider<ContentProviderClient> clientProvider) {
        this.clientProvider = clientProvider;
    }

    public void save(ModifierSequence sequence) {
        ContentProviderClient db = clientProvider.get();
        try {
            if (sequence.getContentUri() == null) {
                sequence.setContentUri(db.insert(ModificationsContentProvider.BASE_URI, toContentValues(sequence)));
            } else {
                db.update(sequence.getContentUri(), toContentValues(sequence), null, null);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
    }

    public void delete(ModifierSequence sequence) {
        ContentProviderClient db = clientProvider.get();
        try {
            db.delete(sequence.getContentUri(), null, null);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
    }

    ModifierSequence fromCursor(Cursor cursor) {
        // Hardcoding column positions!
        ModifierSequence ms;
        try {
            ms = new ModifierSequence(Uri.parse(cursor.getString(cursor.getColumnIndex(Table.COLUMN_MEDIA_URI))),
                    new JSONObject(cursor.getString(cursor.getColumnIndex(Table.COLUMN_DATA))));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        ms.setContentUri( ModificationsContentProvider.uriForId(cursor.getInt(cursor.getColumnIndex(Table.COLUMN_ID))));

        return ms;
    }

    private JSONObject toJSON(ModifierSequence sequence) {
        JSONObject data = new JSONObject();
        try {
            JSONArray modifiersJSON = new JSONArray();
            for (PageModifier modifier: sequence.getModifiers()) {
                modifiersJSON.put(modifier.toJSON());
            }
            data.put("modifiers", modifiersJSON);
            return data;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private ContentValues toContentValues(ModifierSequence sequence) {
        ContentValues cv = new ContentValues();
        cv.put(Table.COLUMN_MEDIA_URI, sequence.getMediaUri().toString());
        cv.put(Table.COLUMN_DATA, toJSON(sequence).toString());
        return cv;
    }

    public static class Table {
        static final String TABLE_NAME = "modifications";

        static final String COLUMN_ID = "_id";
        static final String COLUMN_MEDIA_URI = "mediauri";
        static final String COLUMN_DATA = "data";

        // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
        public static final String[] ALL_FIELDS = {
                COLUMN_ID,
                COLUMN_MEDIA_URI,
                COLUMN_DATA
        };

        static final String DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS " + TABLE_NAME;

        static final String CREATE_TABLE_STATEMENT = "CREATE TABLE " + TABLE_NAME + " ("
                + "_id INTEGER PRIMARY KEY,"
                + "mediauri STRING,"
                + "data STRING"
                + ");";

        public static void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_STATEMENT);
        }

        public static void onUpdate(SQLiteDatabase db, int from, int to) {
            db.execSQL(DROP_TABLE_STATEMENT);
            onCreate(db);
        }

        public static void onDelete(SQLiteDatabase db) {
            db.execSQL(DROP_TABLE_STATEMENT);
            onCreate(db);
        }
    }
}
