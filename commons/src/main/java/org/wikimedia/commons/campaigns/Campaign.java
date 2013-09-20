package org.wikimedia.commons.campaigns;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Campaign {
    private boolean enabled;

    private String autoAddWikitext;
    private ArrayList<String> autoAddCategories;

    private String name;
    private String ownWorkLicenseDefault;

    private String defaultDescription;

    private JSONObject config;
    private String body;
    private boolean isParsed;
    private String trackingCategory;
    private String description;
    private String title;

    public boolean isEnabled() {
        return enabled;
    }

    public String getAutoAddWikitext() {
        if(!this.isParsed) {
            this.parseConfig();
        }
        return autoAddWikitext;
    }

    public ArrayList<String> getAutoAddCategories() {
        if(!this.isParsed) {
            this.parseConfig();
        }
        return autoAddCategories;
    }

    public String getName() {
        return name;
    }

    public String getOwnWorkLicenseDefault() {
        if(!this.isParsed) {
            this.parseConfig();
        }
        return ownWorkLicenseDefault;
    }

    public String getDefaultDescription() {
        if(!this.isParsed) {
            this.parseConfig();
        }
        return defaultDescription;
    }

    public JSONObject getConfig() {
        if(!this.isParsed) {
            this.parseConfig();
        }
        return config;
    }

    private void parseConfig() {
        try {
           this.config = new JSONObject(body);
        } catch (JSONException e) {
            throw new RuntimeException(e); // because what else are you gonna do?
        }
        if(config.has("autoAdd")) {
            this.autoAddWikitext = config.optJSONObject("autoAdd").optString("wikitext", null);
            if(config.optJSONObject("autoAdd").has("categories")) {
                this.autoAddCategories = new ArrayList<String>();
                JSONArray catsArray = config.optJSONObject("autoAdd").optJSONArray("categories");
                for(int i=0; i < catsArray.length(); i++) {
                    autoAddCategories.add(catsArray.optString(i));
                }
            }
        }
        this.title = config.optString("title", name);
        this.description = config.optString("description", "");
        this.isParsed = true;
    }
    private Campaign(String name, String body, String trackingCategory) {
        this.name = name;
        this.body = body;
        this.trackingCategory = trackingCategory;
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Table.COLUMN_NAME, this.getName());
        cv.put(Table.COLUMN_ENABLED, this.isEnabled() ? 1 : 0);
        cv.put(Table.COLUMN_TITLE, this.getTitle());
        cv.put(Table.COLUMN_DESCRIPTION, this.getDescription());
        cv.put(Table.COLUMN_TRACKING_CATEGORY, this.getTrackingCategory());
        cv.put(Table.COLUMN_BODY, this.body);
        return cv;
    }

    public static Campaign parse(String name, String body, String trackingCategory) {
        Campaign c = new Campaign(name, body, trackingCategory);
        c.parseConfig();
        return c;
    }

    public static Campaign fromCursor(Cursor cursor) {
        String name = cursor.getString(1);
        Boolean enabled = cursor.getInt(2) == 1;
        String title = cursor.getString(3);
        String description = cursor.getString(4);
        String trackingCategory = cursor.getString(5);
        String body = cursor.getString(6);
        Campaign c = new Campaign(name, body, trackingCategory);
        c.title = title;
        c.description = description;
        c.enabled = enabled;
        return c;
    }

    public String getTrackingCategory() {
        return trackingCategory;
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    public static class Table {
        public static final String TABLE_NAME = "campaigns";

        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_ENABLED = "enabled";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_TRACKING_CATEGORY = "tracking_category";
        public static final String COLUMN_BODY = "body";

        // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
        public static final String[] ALL_FIELDS = {
                COLUMN_ID,
                COLUMN_NAME,
                COLUMN_ENABLED,
                COLUMN_TITLE,
                COLUMN_DESCRIPTION,
                COLUMN_TRACKING_CATEGORY,
                COLUMN_BODY
        };


        private static final String CREATE_TABLE_STATEMENT = "CREATE TABLE " + TABLE_NAME + " ("
                + "_id INTEGER PRIMARY KEY,"
                + "name STRING,"
                + "enabled INTEGER,"
                + "title STRING,"
                + "description STRING,"
                + "tracking_category STRING,"
                + "body STRING"
                + ");";


        public static void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_STATEMENT);
        }

        public static void onUpdate(SQLiteDatabase db, int from, int to) {
            if(to <= 6) {
                onCreate(db);
                return;
            }
            return;
        }
    }
}
