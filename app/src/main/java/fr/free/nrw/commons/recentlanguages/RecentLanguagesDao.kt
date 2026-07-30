package fr.free.nrw.commons.recentlanguages

import android.annotation.SuppressLint
import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.os.RemoteException
import fr.free.nrw.commons.recentlanguages.RecentLanguagesContentProvider.Companion.BASE_URI
import fr.free.nrw.commons.recentlanguages.RecentLanguagesTable.ALL_FIELDS
import fr.free.nrw.commons.recentlanguages.RecentLanguagesTable.COLUMN_CODE
import fr.free.nrw.commons.recentlanguages.RecentLanguagesTable.COLUMN_NAME
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
                BASE_URI,
                ALL_FIELDS,
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
                BASE_URI,
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
                BASE_URI,
                ALL_FIELDS,
                "${COLUMN_CODE}=?",
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
            cursor.getColumnIndex(COLUMN_NAME)
        )
        val languageCode = cursor.getString(
            cursor.getColumnIndex(COLUMN_CODE)
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
            put(COLUMN_NAME, recentLanguage.languageName)
            put(COLUMN_CODE, recentLanguage.languageCode)
        }
    }
}
