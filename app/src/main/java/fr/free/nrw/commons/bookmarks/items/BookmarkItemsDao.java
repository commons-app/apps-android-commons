package fr.free.nrw.commons.bookmarks.items;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;
import fr.free.nrw.commons.category.CategoryItem;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.apache.commons.lang3.StringUtils;

/**
 * Handles database operations for bookmarked items
 */
@Singleton
public class BookmarkItemsDao {

    private final Provider<ContentProviderClient> clientProvider;

    @Inject
    public BookmarkItemsDao(
        @Named("bookmarksItem") final Provider<ContentProviderClient> clientProvider) {
        this.clientProvider = clientProvider;
    }


    /**
     * Find all persisted items bookmarks on database
     * @return list of bookmarks
     */
    public List<DepictedItem> getAllBookmarksItems() {
        final List<DepictedItem> items = new ArrayList<>();
        final ContentProviderClient db = clientProvider.get();
        try (final Cursor cursor = db.query(
            BookmarkItemsContentProvider.BASE_URI,
            Table.ALL_FIELDS,
            null,
            new String[]{},
            null)) {
            while (cursor != null && cursor.moveToNext()) {
                items.add(fromCursor(cursor));
            }
        } catch (final RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
        return items;
    }


    /**
     * Look for a bookmark in database and in order to insert or delete it
     * @param depictedItem : Bookmark object
     * @return boolean : is bookmark now favorite ?
     */
    public boolean updateBookmarkItem(final DepictedItem depictedItem) {
        final boolean bookmarkExists = findBookmarkItem(depictedItem.getId());
        if (bookmarkExists) {
            deleteBookmarkItem(depictedItem);
        } else {
            addBookmarkItem(depictedItem);
        }
        return !bookmarkExists;
    }

    /**
     * Add a Bookmark to database
     * @param depictedItem : Bookmark to add
     */
    private void addBookmarkItem(final DepictedItem depictedItem) {
        final ContentProviderClient db = clientProvider.get();
        try {
            db.insert(BookmarkItemsContentProvider.BASE_URI, toContentValues(depictedItem));
        } catch (final RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
    }

    /**
     * Delete a bookmark from database
     * @param depictedItem : Bookmark to delete
     */
    private void deleteBookmarkItem(final DepictedItem depictedItem) {
        final ContentProviderClient db = clientProvider.get();
        try {
            db.delete(BookmarkItemsContentProvider.uriForName(depictedItem.getId()), null, null);
        } catch (final RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
    }

    /**
     * Find a bookmark from database based on its name
     * @param depictedItemID : Bookmark to find
     * @return boolean : is bookmark in database ?
     */
    public boolean findBookmarkItem(final String depictedItemID) {
        if (depictedItemID == null) { //Avoiding NPE's
            return false;
        }
        final ContentProviderClient db = clientProvider.get();
        try (final Cursor cursor = db.query(
            BookmarkItemsContentProvider.BASE_URI,
            Table.ALL_FIELDS,
            Table.COLUMN_ID + "=?",
            new String[]{depictedItemID},
            null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                return true;
            }
        } catch (final RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
        return false;
    }

    /**
     * Recives real data from cursor
     * @param cursor : Object for storing database data
     * @return DepictedItem
     */
    DepictedItem fromCursor(final Cursor cursor) {
        final String fileName = cursor.getString(cursor.getColumnIndex(Table.COLUMN_NAME));
        final String description
            = cursor.getString(cursor.getColumnIndex(Table.COLUMN_DESCRIPTION));
        final String imageUrl = cursor.getString(cursor.getColumnIndex(Table.COLUMN_IMAGE));
        final String instanceListString
            = cursor.getString(cursor.getColumnIndex(Table.COLUMN_INSTANCE_LIST));
        final List<String> instanceList = StringToArray(instanceListString);
        final String categoryNameListString = cursor.getString(cursor
            .getColumnIndex(Table.COLUMN_CATEGORIES_NAME_LIST));
        final List<String> categoryNameList = StringToArray(categoryNameListString);
        final String categoryDescriptionListString = cursor.getString(cursor
            .getColumnIndex(Table.COLUMN_CATEGORIES_DESCRIPTION_LIST));
        final List<String> categoryDescriptionList = StringToArray(categoryDescriptionListString);
        final String categoryThumbnailListString = cursor.getString(cursor
            .getColumnIndex(Table.COLUMN_CATEGORIES_THUMBNAIL_LIST));
        final List<String> categoryThumbnailList = StringToArray(categoryThumbnailListString);
        final List<CategoryItem> categoryList = convertToCategoryItems(categoryNameList,
            categoryDescriptionList, categoryThumbnailList);
        final boolean isSelected
            = Boolean.parseBoolean(cursor.getString(cursor
            .getColumnIndex(Table.COLUMN_IS_SELECTED)));
        final String id = cursor.getString(cursor.getColumnIndex(Table.COLUMN_ID));

        return new DepictedItem(
            fileName,
            description,
            imageUrl,
            instanceList,
            categoryList,
            isSelected,
            id
        );
    }

    private List<CategoryItem> convertToCategoryItems(List<String> categoryNameList,
        List<String> categoryDescriptionList, List<String> categoryThumbnailList) {
        List<CategoryItem> categoryItems = new ArrayList<>();
        for(int i=0; i<categoryNameList.size(); i++){
            categoryItems.add(new CategoryItem(categoryNameList.get(i),
                categoryDescriptionList.get(i),
                categoryThumbnailList.get(i), false));
        }
        return categoryItems;
    }

    /**
     * Converts string to List
     * @param listString comma separated single string from of list items
     * @return List of string
     */
    private List<String> StringToArray(final String listString) {
        final String[] elements = listString.split(",");
        return Arrays.asList(elements);
    }

    /**
     * Converts string to List
     * @param list list of items
     * @return string comma separated single string of items
     */
    private String ArrayToString(final List<String> list) {
        if (list != null) {
            return StringUtils.join(list, ',');
        }
        return null;
    }

    /**
     * Takes data from DepictedItem and create a content value object
     * @param depictedItem depicted item
     * @return ContentValues
     */
    private ContentValues toContentValues(final DepictedItem depictedItem) {

        final List<String> namesOfCommonsCategories = new ArrayList<>();
        for (final CategoryItem category :
            depictedItem.getCommonsCategories()) {
            namesOfCommonsCategories.add(category.getName());
        }

        final List<String> descriptionsOfCommonsCategories = new ArrayList<>();
        for (final CategoryItem category :
            depictedItem.getCommonsCategories()) {
            descriptionsOfCommonsCategories.add(category.getDescription());
        }

        final List<String> thumbnailsOfCommonsCategories = new ArrayList<>();
        for (final CategoryItem category :
            depictedItem.getCommonsCategories()) {
            thumbnailsOfCommonsCategories.add(category.getThumbnail());
        }

        final ContentValues cv = new ContentValues();
        cv.put(Table.COLUMN_NAME, depictedItem.getName());
        cv.put(Table.COLUMN_DESCRIPTION, depictedItem.getDescription());
        cv.put(Table.COLUMN_IMAGE, depictedItem.getImageUrl());
        cv.put(Table.COLUMN_INSTANCE_LIST, ArrayToString(depictedItem.getInstanceOfs()));
        cv.put(Table.COLUMN_CATEGORIES_NAME_LIST, ArrayToString(namesOfCommonsCategories));
        cv.put(Table.COLUMN_CATEGORIES_DESCRIPTION_LIST,
            ArrayToString(descriptionsOfCommonsCategories));
        cv.put(Table.COLUMN_CATEGORIES_THUMBNAIL_LIST,
            ArrayToString(thumbnailsOfCommonsCategories));
        cv.put(Table.COLUMN_IS_SELECTED, depictedItem.isSelected());
        cv.put(Table.COLUMN_ID, depictedItem.getId());
        return cv;
    }

    /**
     * Table of bookmarksItems data
     */
    public static final class Table {
        public static final String TABLE_NAME = "bookmarksItems";
        public static final String COLUMN_NAME = "item_name";
        public static final String COLUMN_DESCRIPTION = "item_description";
        public static final String COLUMN_IMAGE = "item_image_url";
        public static final String COLUMN_INSTANCE_LIST = "item_instance_of";
        public static final String COLUMN_CATEGORIES_NAME_LIST = "item_name_categories";
        public static final String COLUMN_CATEGORIES_DESCRIPTION_LIST = "item_description_categories";
        public static final String COLUMN_CATEGORIES_THUMBNAIL_LIST = "item_thumbnail_categories";
        public static final String COLUMN_IS_SELECTED = "item_is_selected";
        public static final String COLUMN_ID = "item_id";

        public static final String[] ALL_FIELDS = {
            COLUMN_NAME,
            COLUMN_DESCRIPTION,
            COLUMN_IMAGE,
            COLUMN_INSTANCE_LIST,
            COLUMN_CATEGORIES_NAME_LIST,
            COLUMN_CATEGORIES_DESCRIPTION_LIST,
            COLUMN_CATEGORIES_THUMBNAIL_LIST,
            COLUMN_IS_SELECTED,
            COLUMN_ID
        };

        static final String DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS " + TABLE_NAME;
        static final String CREATE_TABLE_STATEMENT = "CREATE TABLE " + TABLE_NAME + " ("
            + COLUMN_NAME + " STRING,"
            + COLUMN_DESCRIPTION + " STRING,"
            + COLUMN_IMAGE + " STRING,"
            + COLUMN_INSTANCE_LIST + " STRING,"
            + COLUMN_CATEGORIES_NAME_LIST + " STRING,"
            + COLUMN_CATEGORIES_DESCRIPTION_LIST + " STRING,"
            + COLUMN_CATEGORIES_THUMBNAIL_LIST + " STRING,"
            + COLUMN_IS_SELECTED + " STRING,"
            + COLUMN_ID + " STRING PRIMARY KEY"
            + ");";

        /**
         * Creates table
         * @param db SQLiteDatabase
         */
        public static void onCreate(final SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_STATEMENT);
        }

        /**
         * Deletes database
         * @param db SQLiteDatabase
         */
        public static void onDelete(final SQLiteDatabase db) {
            db.execSQL(DROP_TABLE_STATEMENT);
            onCreate(db);
        }

        /**
         * Updates database
         * @param db SQLiteDatabase
         * @param from starting
         * @param to end
         */
        public static void onUpdate(final SQLiteDatabase db, int from, final int to) {
            if (from == to) {
                return;
            }
            if (from < 7) {
                from++;
                onUpdate(db, from, to);
                return;
            }

            if (from == 7) {
                onCreate(db);
                from++;
                onUpdate(db, from, to);
                return;
            }

            if (from == 8) {
                from++;
                onUpdate(db, from, to);
            }
        }
    }
}
