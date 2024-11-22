package fr.free.nrw.commons.recentlanguages

import android.annotation.SuppressLint
import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.RemoteException
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton


/**
 * Handles database operations for recently used languages
 */
@Singleton
class RecentLanguagesDao @Inject constructor(
    @Named("recent_languages")
    private val clientProvider: Provider<ContentProviderClient>
) {

    /**
     * Find all persisted recently used languages on database
     * @return list of recently used languages
     */
    fun getRecentLanguages(): List<Language> {
        val languages = mutableListOf<Language>()
        val db = clientProvider.get()
        try {
            db.query(
                RecentLanguagesContentProvider.BASE_URI,
                Table.ALL_FIELDS,
                null,
                arrayOf(),
                null
            )?.use { cursor ->
                if (cursor.moveToLast()) {
                    do {
                        languages.add(fromCursor(cursor))
                    } while (cursor.moveToPrevious())
                }
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.release()
        }
        return languages
    }

    /**
     * Add a Language to database
     * @param language : Language to add
     */
    fun addRecentLanguage(language: Language) {
        val db = clientProvider.get()
        try {
            db.insert(
                RecentLanguagesContentProvider.BASE_URI,
                toContentValues(language)
            )
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.release()
        }
    }

    /**
     * Delete a language from database
     * @param languageCode : code of the Language to delete
     */
    fun deleteRecentLanguage(languageCode: String) {
        val db = clientProvider.get()
        try {
            db.delete(
                RecentLanguagesContentProvider.uriForCode(languageCode),
                null,
                null
            )
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.release()
        }
    }

    /**
     * Find a language from database based on its name
     * @param languageCode : code of the Language to find
     * @return boolean : is language in database ?
     */
    fun findRecentLanguage(languageCode: String?): Boolean {
        if (languageCode == null) { // Avoiding NPEs
            return false
        }
        val db = clientProvider.get()
        try {
            db.query(
                RecentLanguagesContentProvider.BASE_URI,
                Table.ALL_FIELDS,
                "${Table.COLUMN_CODE}=?",
                arrayOf(languageCode),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return true
                }
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.release()
        }
        return false
    }

    /**
     * It creates an Recent Language object from data stored in the SQLite DB by using cursor
     * @param cursor cursor
     * @return Language object
     */
    @SuppressLint("Range")
    fun fromCursor(cursor: Cursor): Language {
        // Hardcoding column positions!
        val languageName = cursor.getString(
            cursor.getColumnIndex(Table.COLUMN_NAME)
        )
        val languageCode = cursor.getString(
            cursor.getColumnIndex(Table.COLUMN_CODE)
        )
        return Language(languageName, languageCode)
    }

    /**
     * Takes data from Language and create a content value object
     * @param recentLanguage recently used language
     * @return ContentValues
     */
    private fun toContentValues(recentLanguage: Language): ContentValues {
        return ContentValues().apply {
            put(Table.COLUMN_NAME, recentLanguage.languageName)
            put(Table.COLUMN_CODE, recentLanguage.languageCode)
        }
    }

    /**
     * This class contains the database table architecture for recently used languages,
     * It also contains queries and logic necessary to the create, update, delete this table.
     */
    object Table {
        const val TABLE_NAME = "recent_languages"
        const val COLUMN_NAME = "language_name"
        const val COLUMN_CODE = "language_code"

        // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
        @JvmStatic
        val ALL_FIELDS = arrayOf(
            COLUMN_NAME,
            COLUMN_CODE
        )

        private const val DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS $TABLE_NAME"

        private const val CREATE_TABLE_STATEMENT = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_NAME STRING," +
                "$COLUMN_CODE STRING PRIMARY KEY" +
                ");"

        /**
         * This method creates a LanguagesTable in SQLiteDatabase
         * @param db SQLiteDatabase
         */
        @SuppressLint("SQLiteString")
        @JvmStatic
        fun onCreate(db: SQLiteDatabase) {
            db.execSQL(CREATE_TABLE_STATEMENT)
        }

        /**
         * This method deletes LanguagesTable from SQLiteDatabase
         * @param db SQLiteDatabase
         */
        @JvmStatic
        fun onDelete(db: SQLiteDatabase) {
            db.execSQL(DROP_TABLE_STATEMENT)
            onCreate(db)
        }

        /**
         * This method is called on migrating from a older version to a newer version
         * @param db SQLiteDatabase
         * @param from Version from which we are migrating
         * @param to Version to which we are migrating
         */
        @JvmStatic
        fun onUpdate(db: SQLiteDatabase, from: Int, to: Int) {
            if (from == to) {
                return
            }
            if (from < 19) {
                // doesn't exist yet
                onUpdate(db, from + 1, to)
                return
            }
            if (from == 19) {
                // table added in version 20
                onCreate(db)
                onUpdate(db, from + 1, to)
            }
        }
    }
}
