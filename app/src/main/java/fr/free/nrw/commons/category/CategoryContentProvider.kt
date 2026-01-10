package fr.free.nrw.commons.category


import android.content.ContentValues
import android.content.UriMatcher
import android.content.UriMatcher.NO_MATCH
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.text.TextUtils
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.di.CommonsDaggerContentProvider
import androidx.core.net.toUri

class CategoryContentProvider : CommonsDaggerContentProvider() {

    private val uriMatcher = UriMatcher(NO_MATCH).apply {
        addURI(BuildConfig.CATEGORY_AUTHORITY, BASE_PATH, CATEGORIES)
        addURI(BuildConfig.CATEGORY_AUTHORITY, "${BASE_PATH}/#", CATEGORIES_ID)
    }

    @SuppressWarnings("ConstantConditions")
    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val queryBuilder = SQLiteQueryBuilder().apply {
            tables = TABLE_NAME
        }

        val uriType = uriMatcher.match(uri)
        val db = requireDb()

        val cursor: Cursor? = when (uriType) {
            CATEGORIES -> queryBuilder.query(
                db,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
            )
            CATEGORIES_ID -> queryBuilder.query(
                db,
                ALL_FIELDS,
                "_id = ?",
                arrayOf(uri.lastPathSegment),
                null,
                null,
                sortOrder
            )
            else -> throw IllegalArgumentException("Unknown URI $uri")
        }

        cursor?.setNotificationUri(requireContext().contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String? = null

    @SuppressWarnings("ConstantConditions")
    override fun insert(uri: Uri, contentValues: ContentValues?): Uri {
        val uriType = uriMatcher.match(uri)
        val id: Long
        when (uriType) {
            CATEGORIES -> {
                id = requireDb().insert(TABLE_NAME, null, contentValues)
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        requireContext().contentResolver?.notifyChange(uri, null)
        return "${BASE_URI}/$id".toUri()
    }

    @SuppressWarnings("ConstantConditions")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    @SuppressWarnings("ConstantConditions")
    override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int {
        val uriType = uriMatcher.match(uri)
        val sqlDB = requireDb()
        sqlDB.beginTransaction()
        when (uriType) {
            CATEGORIES -> {
                for (value in values) {
                    sqlDB.insert(TABLE_NAME, null, value)
                }
                sqlDB.setTransactionSuccessful()
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        sqlDB.endTransaction()
        requireContext().contentResolver?.notifyChange(uri, null)
        return values.size
    }

    @SuppressWarnings("ConstantConditions")
    override fun update(uri: Uri, contentValues: ContentValues?, selection: String?,
                        selectionArgs: Array<String>?): Int {
        val uriType = uriMatcher.match(uri)
        val rowsUpdated: Int
        when (uriType) {
            CATEGORIES_ID -> {
                if (TextUtils.isEmpty(selection)) {
                    val id = uri.lastPathSegment?.toInt()
                        ?: throw IllegalArgumentException("Invalid ID")
                    rowsUpdated = requireDb().update(
                        TABLE_NAME,
                        contentValues,
                        "$COLUMN_ID = ?",
                        arrayOf(id.toString())
                    )
                } else {
                    throw IllegalArgumentException(
                        "Parameter `selection` should be empty when updating an ID")
                }
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri with type $uriType")
        }
        requireContext().contentResolver?.notifyChange(uri, null)
        return rowsUpdated
    }

    companion object {
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
                "$COLUMN_NAME TEXT," +
                "$COLUMN_DESCRIPTION TEXT," +
                "$COLUMN_THUMBNAIL TEXT," +
                "$COLUMN_LAST_USED INTEGER," +
                "$COLUMN_TIMES_USED INTEGER" +
                ");"

        fun uriForId(id: Int): Uri = Uri.parse("${BASE_URI}/$id")

        fun onCreate(db: SQLiteDatabase) = db.execSQL(CREATE_TABLE_STATEMENT)

        fun onDelete(db: SQLiteDatabase) {
            db.execSQL(DROP_TABLE_STATEMENT)
            onCreate(db)
        }

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
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN description TEXT;")
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN thumbnail TEXT;")
                onUpdate(db, from + 1, to)
            }
        }

        // For URI matcher
        private const val CATEGORIES = 1
        private const val CATEGORIES_ID = 2
        private const val BASE_PATH = "categories"
        val  BASE_URI: Uri = "content://${BuildConfig.CATEGORY_AUTHORITY}/${BASE_PATH}".toUri()
    }
}
