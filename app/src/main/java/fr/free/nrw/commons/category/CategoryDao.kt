package fr.free.nrw.commons.category

import android.annotation.SuppressLint
import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.os.RemoteException
import fr.free.nrw.commons.category.CategoryTable.ALL_FIELDS
import fr.free.nrw.commons.category.CategoryTable.COLUMN_DESCRIPTION
import fr.free.nrw.commons.category.CategoryTable.COLUMN_ID
import fr.free.nrw.commons.category.CategoryTable.COLUMN_LAST_USED
import fr.free.nrw.commons.category.CategoryTable.COLUMN_NAME
import fr.free.nrw.commons.category.CategoryTable.COLUMN_THUMBNAIL
import fr.free.nrw.commons.category.CategoryTable.COLUMN_TIMES_USED

import java.util.ArrayList
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider

class CategoryDao @Inject constructor(
    @Named("category") private val clientProvider: Provider<ContentProviderClient>
) {

    fun save(category: Category) {
        val db = clientProvider.get()
        try {
            if (category.contentUri == null) {
                category.contentUri = db.insert(
                    CategoryContentProvider.BASE_URI,
                    toContentValues(category)
                )
            } else {
                db.update(
                    category.contentUri!!,
                    toContentValues(category),
                    null,
                    null
                )
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.release()
        }
    }

    /**
     * Find persisted category in database, based on its name.
     *
     * @param name Category's name
     * @return category from database, or null if not found
     */
    fun find(name: String): Category? {
        var cursor: Cursor? = null
        val db = clientProvider.get()
        try {
            cursor = db.query(
                CategoryContentProvider.BASE_URI,
                ALL_FIELDS,
                "${COLUMN_NAME}=?",
                arrayOf(name),
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                return fromCursor(cursor)
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            cursor?.close()
            db.release()
        }
        return null
    }

    /**
     * Retrieve recently-used categories, ordered by descending date.
     *
     * @return a list containing recent categories
     */
    fun recentCategories(limit: Int): List<CategoryItem> {
        val items = ArrayList<CategoryItem>()
        var cursor: Cursor? = null
        val db = clientProvider.get()
        try {
            cursor = db.query(
                CategoryContentProvider.BASE_URI,
                ALL_FIELDS,
                null,
                emptyArray(),
                "$COLUMN_LAST_USED DESC"
            )
            while (cursor != null && cursor.moveToNext() && cursor.position < limit) {
                val category = fromCursor(cursor)
                if (category.name != null) {
                    items.add(
                        CategoryItem(
                            category.name,
                            category.description,
                            category.thumbnail,
                            false
                        )
                    )
                }
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            cursor?.close()
            db.release()
        }
        return items
    }

    @SuppressLint("Range")
    fun fromCursor(cursor: Cursor): Category {
        // Hardcoding column positions!
        return Category(
            CategoryContentProvider.uriForId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID))),
            cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
            cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)),
            cursor.getString(cursor.getColumnIndex(COLUMN_THUMBNAIL)),
            Date(cursor.getLong(cursor.getColumnIndex(COLUMN_LAST_USED))),
            cursor.getInt(cursor.getColumnIndex(COLUMN_TIMES_USED))
        )
    }

    private fun toContentValues(category: Category): ContentValues {
        return ContentValues().apply {
            put(COLUMN_NAME, category.name)
            put(COLUMN_DESCRIPTION, category.description)
            put(COLUMN_THUMBNAIL, category.thumbnail)
            put(COLUMN_LAST_USED, category.lastUsed?.time)
            put(COLUMN_TIMES_USED, category.timesUsed)
        }
    }
}
