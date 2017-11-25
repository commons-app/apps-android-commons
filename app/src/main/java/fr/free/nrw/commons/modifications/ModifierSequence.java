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

import java.util.ArrayList;

public class ModifierSequence {
    private Uri mediaUri;
    private ArrayList<PageModifier> modifiers;
    private Uri contentUri;
    private ContentProviderClient client;

    public ModifierSequence(Uri mediaUri) {
        this.mediaUri = mediaUri;
        modifiers = new ArrayList<>();
    }

    public ModifierSequence(Uri mediaUri, JSONObject data) {
        this(mediaUri);
        JSONArray modifiersJSON = data.optJSONArray("modifiers");
        for (int i = 0; i < modifiersJSON.length(); i++) {
            modifiers.add(PageModifier.fromJSON(modifiersJSON.optJSONObject(i)));
        }
    }

    public Uri getMediaUri() {
        return mediaUri;
    }

    public void queueModifier(PageModifier modifier) {
        modifiers.add(modifier);
    }

    public String executeModifications(String pageName, String pageContents) {
        for (PageModifier modifier: modifiers) {
            pageContents = modifier.doModification(pageName,  pageContents);
        }
        return pageContents;
    }

    public String getEditSummary() {
        StringBuilder editSummary = new StringBuilder();
        for (PageModifier modifier: modifiers) {
            editSummary.append(modifier.getEditSumary()).append(" ");
        }
        editSummary.append("Via Commons Mobile App");
        return editSummary.toString();
    }

    public JSONObject toJSON() {
        JSONObject data = new JSONObject();
        try {
            JSONArray modifiersJSON = new JSONArray();
            for (PageModifier modifier: modifiers) {
                modifiersJSON.put(modifier.toJSON());
            }
            data.put("modifiers", modifiersJSON);
            return data;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Table.COLUMN_MEDIA_URI, mediaUri.toString());
        cv.put(Table.COLUMN_DATA, toJSON().toString());
        return cv;
    }

    public static ModifierSequence fromCursor(Cursor cursor) {
        // Hardcoding column positions!
        ModifierSequence ms = null;
        try {
            ms = new ModifierSequence(Uri.parse(cursor.getString(1)),
                new JSONObject(cursor.getString(2)));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        ms.contentUri = ModificationsContentProvider.uriForId(cursor.getInt(0));

        return ms;
    }

    public void save() {
        try {
            if (contentUri == null) {
                contentUri = client.insert(ModificationsContentProvider.BASE_URI, this.toContentValues());
            } else {
                client.update(contentUri, toContentValues(), null, null);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete() {
        try {
            client.delete(contentUri, null, null);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void setContentProviderClient(ContentProviderClient client) {
        this.client = client;
    }

    public static class Table {
        public static final String TABLE_NAME = "modifications";

        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_MEDIA_URI = "mediauri";
        public static final String COLUMN_DATA = "data";

        // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
        public static final String[] ALL_FIELDS = {
                COLUMN_ID,
                COLUMN_MEDIA_URI,
                COLUMN_DATA
        };

        private static final String CREATE_TABLE_STATEMENT = "CREATE TABLE " + TABLE_NAME + " ("
                + "_id INTEGER PRIMARY KEY,"
                + "mediauri STRING,"
                + "data STRING"
                + ");";

        public static void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_STATEMENT);
        }

        public static void onUpdate(SQLiteDatabase db, int from, int to) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }

        public static void onDelete(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }
}
