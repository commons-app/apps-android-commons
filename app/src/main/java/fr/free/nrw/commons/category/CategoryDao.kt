package fr.free.nrw.commons.category

import android.annotation.SuppressLint
import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.RemoteException

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

    companion object Table {
        const val TABLE_NAME = "categories"

        const val COLUMN_ID = "_id"
        const val COLUMN_NAME = "name"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_THUMBNAIL = "thumbnail"
        const val COLUMN_LAST_USED = "last_used"
        const val COLUMN_TIMES_USED = "times_used"

        // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
        val ALL_FIELDS = arrayOf(
            COLUMN_ID,
            COLUMN_NAME,
            COLUMN_DESCRIPTION,
            COLUMN_THUMBNAIL,
            COLUMN_LAST_USED,
            COLUMN_TIMES_USED
        )

        const val DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS $TABLE_NAME"

        const val CREATE_TABLE_STATEMENT = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY," +
                "$COLUMN_NAME STRING," +
                "$COLUMN_DESCRIPTION STRING," +
                "$COLUMN_THUMBNAIL STRING," +
                "$COLUMN_LAST_USED INTEGER," +
                "$COLUMN_TIMES_USED INTEGER" +
                ");"

        @SuppressLint("SQLiteString")
        fun onCreate(db: SQLiteDatabase) {
            db.execSQL(CREATE_TABLE_STATEMENT)
        }

        fun onDelete(db: SQLiteDatabase) {
            db.execSQL(DROP_TABLE_STATEMENT)
            onCreate(db)
        }

        @SuppressLint("SQLiteString")
        fun onUpdate(db: SQLiteDatabase, from: Int, to: Int) {
            if (from == to) return
            if (from < 4) {
                // doesn't exist yet
                onUpdate(db, from + 1, to)
            } else if (from == 4) {
                // table added in version 5
                onCreate(db)
                onUpdate(db, from + 1, to)
            } else if (from == 5) {
                onUpdate(db, from + 1, to)
            } else if (from == 17) {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN description STRING;")
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN thumbnail STRING;")
                onUpdate(db, from + 1, to)
            }
        }
    }
}
