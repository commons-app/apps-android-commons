package fr.free.nrw.commons.bookmarks.locations;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.Label;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.Sitelinks;
import timber.log.Timber;

import static fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsContentProvider.BASE_URI;

public class BookmarkLocationsDao {

    private final Provider<ContentProviderClient> clientProvider;

    @Inject
    public BookmarkLocationsDao(@Named("bookmarksLocation") Provider<ContentProviderClient> clientProvider) {
        this.clientProvider = clientProvider;
    }

    /**
     *  Find all persisted locations bookmarks on database
     *
     * @return list of Place
     */
    @NonNull
    public List<Place> getAllBookmarksLocations() {
        List<Place> items = new ArrayList<>();
        Cursor cursor = null;
        ContentProviderClient db = clientProvider.get();
        try {
            cursor = db.query(
                    BookmarkLocationsContentProvider.BASE_URI,
                    Table.ALL_FIELDS,
                    null,
                    new String[]{},
                    null);
            while (cursor != null && cursor.moveToNext()) {
                items.add(fromCursor(cursor));
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.release();
        }
        return items;
    }

    /**
     * Look for a place in bookmarks table in order to insert or delete it
     *
     * @param bookmarkLocation : Place object
     * @return is Place now fav ?
     */
    public boolean updateBookmarkLocation(Place bookmarkLocation) {
        boolean bookmarkExists = findBookmarkLocation(bookmarkLocation);
        if (bookmarkExists) {
            deleteBookmarkLocation(bookmarkLocation);
        } else {
            addBookmarkLocation(bookmarkLocation);
        }
        return !bookmarkExists;
    }

    /**
     * Add a Place to bookmarks table
     *
     * @param bookmarkLocation : Place to add
     */
    private void addBookmarkLocation(Place bookmarkLocation) {
        ContentProviderClient db = clientProvider.get();
        try {
            db.insert(BASE_URI, toContentValues(bookmarkLocation));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
    }

    /**
     * Delete a Place from bookmarks table
     *
     * @param bookmarkLocation : Place to delete
     */
    private void deleteBookmarkLocation(Place bookmarkLocation) {
        ContentProviderClient db = clientProvider.get();
        try {
            db.delete(BookmarkLocationsContentProvider.uriForName(bookmarkLocation.name), null, null);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
    }

    /**
     * Find a Place from database based on its name
     *
     * @param bookmarkLocation : Place to find
     * @return boolean : is Place in database ?
     */
    public boolean findBookmarkLocation(Place bookmarkLocation) {
        Cursor cursor = null;
        ContentProviderClient db = clientProvider.get();
        try {
            cursor = db.query(
                    BookmarkLocationsContentProvider.BASE_URI,
                    Table.ALL_FIELDS,
                    Table.COLUMN_NAME + "=?",
                    new String[]{bookmarkLocation.name},
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                return true;
            }
        } catch (RemoteException e) {
            // This feels lazy, but to hell with checked exceptions. :)
            throw new RuntimeException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.release();
        }
        return false;
    }

    @NonNull
    Place fromCursor(Cursor cursor) {
        LatLng location = new LatLng(cursor.getDouble(cursor.getColumnIndex(Table.COLUMN_LAT)),
                cursor.getDouble(cursor.getColumnIndex(Table.COLUMN_LONG)), 1F);

        Sitelinks.Builder builder = new Sitelinks.Builder();
        builder.setWikipediaLink(cursor.getString(cursor.getColumnIndex(Table.COLUMN_WIKIPEDIA_LINK)));
        builder.setWikidataLink(cursor.getString(cursor.getColumnIndex(Table.COLUMN_WIKIDATA_LINK)));
        builder.setCommonsLink(cursor.getString(cursor.getColumnIndex(Table.COLUMN_COMMONS_LINK)));

        return new Place(
                cursor.getString(cursor.getColumnIndex(Table.COLUMN_NAME)),
                Label.fromText((cursor.getString(cursor.getColumnIndex(Table.COLUMN_LABEL_TEXT)))),
                cursor.getString(cursor.getColumnIndex(Table.COLUMN_DESCRIPTION)),
                location,
                cursor.getString(cursor.getColumnIndex(Table.COLUMN_CATEGORY)),
                builder.build(),
                null,
                null
        );
        // TODO: add pic and destroyed to bookmark location dao
    }

    private ContentValues toContentValues(Place bookmarkLocation) {
        ContentValues cv = new ContentValues();
        cv.put(BookmarkLocationsDao.Table.COLUMN_NAME, bookmarkLocation.getName());
        cv.put(BookmarkLocationsDao.Table.COLUMN_DESCRIPTION, bookmarkLocation.getLongDescription());
        cv.put(BookmarkLocationsDao.Table.COLUMN_CATEGORY, bookmarkLocation.getCategory());
        cv.put(BookmarkLocationsDao.Table.COLUMN_LABEL_TEXT, bookmarkLocation.getLabel().getText());
        cv.put(BookmarkLocationsDao.Table.COLUMN_LABEL_ICON, bookmarkLocation.getLabel().getIcon());
        cv.put(BookmarkLocationsDao.Table.COLUMN_WIKIPEDIA_LINK, bookmarkLocation.siteLinks.getWikipediaLink().toString());
        cv.put(BookmarkLocationsDao.Table.COLUMN_WIKIDATA_LINK, bookmarkLocation.siteLinks.getWikidataLink().toString());
        cv.put(BookmarkLocationsDao.Table.COLUMN_COMMONS_LINK, bookmarkLocation.siteLinks.getCommonsLink().toString());
        cv.put(BookmarkLocationsDao.Table.COLUMN_LAT, bookmarkLocation.location.getLatitude());
        cv.put(BookmarkLocationsDao.Table.COLUMN_LONG, bookmarkLocation.location.getLongitude());
        cv.put(BookmarkLocationsDao.Table.COLUMN_PIC, bookmarkLocation.pic);
        return cv;
    }

    public static class Table {
        public static final String TABLE_NAME = "bookmarksLocations";

        static final String COLUMN_NAME = "location_name";
        static final String COLUMN_DESCRIPTION = "location_description";
        static final String COLUMN_LAT = "location_lat";
        static final String COLUMN_LONG = "location_long";
        static final String COLUMN_CATEGORY = "location_category";
        static final String COLUMN_LABEL_TEXT = "location_label_text";
        static final String COLUMN_LABEL_ICON = "location_label_icon";
        static final String COLUMN_IMAGE_URL = "location_image_url";
        static final String COLUMN_WIKIPEDIA_LINK = "location_wikipedia_link";
        static final String COLUMN_WIKIDATA_LINK = "location_wikidata_link";
        static final String COLUMN_COMMONS_LINK = "location_commons_link";
        static final String COLUMN_PIC = "location_pic";

        // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
        public static final String[] ALL_FIELDS = {
                COLUMN_NAME,
                COLUMN_DESCRIPTION,
                COLUMN_CATEGORY,
                COLUMN_LABEL_TEXT,
                COLUMN_LABEL_ICON,
                COLUMN_LAT,
                COLUMN_LONG,
                COLUMN_IMAGE_URL,
                COLUMN_WIKIPEDIA_LINK,
                COLUMN_WIKIDATA_LINK,
                COLUMN_COMMONS_LINK,
                COLUMN_PIC
        };

        static final String DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS " + TABLE_NAME;

        static final String CREATE_TABLE_STATEMENT = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_NAME + " STRING PRIMARY KEY,"
                + COLUMN_DESCRIPTION + " STRING,"
                + COLUMN_CATEGORY + " STRING,"
                + COLUMN_LABEL_TEXT + " STRING,"
                + COLUMN_LABEL_ICON + " INTEGER,"
                + COLUMN_LAT + " DOUBLE,"
                + COLUMN_LONG + " DOUBLE,"
                + COLUMN_IMAGE_URL + " STRING,"
                + COLUMN_WIKIPEDIA_LINK + " STRING,"
                + COLUMN_WIKIDATA_LINK + " STRING,"
                + COLUMN_COMMONS_LINK + " STRING,"
                + COLUMN_PIC + " STRING"
                + ");";

        public static void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_STATEMENT);
        }

        public static void onDelete(SQLiteDatabase db) {
            db.execSQL(DROP_TABLE_STATEMENT);
            onCreate(db);
        }

        public static void onUpdate(SQLiteDatabase db, int from, int to) {
            Timber.d("bookmarksLocations db is updated from:"+from+", to:"+to);
            if (from == to) {
                return;
            }
            if (from < 7) {
                // doesn't exist yet
                from++;
                onUpdate(db, from, to);
                return;
            }
            if (from == 7) {
                // table added in version 8
                onCreate(db);
                from++;
                onUpdate(db, from, to);
                return;
            }
            if (from == 8) {
                from++;
                onUpdate(db, from, to);
                return;
            }
            if (from == 10 && to == 11) {
                from++;
                db.execSQL("ALTER TABLE bookmarksLocations ADD COLUMN location_pic STRING;");
                return;
            }
        }
    }
}
