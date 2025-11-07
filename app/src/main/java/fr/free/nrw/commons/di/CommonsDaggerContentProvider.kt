package fr.free.nrw.commons.di

import android.content.ContentProvider
import android.database.sqlite.SQLiteDatabase
import fr.free.nrw.commons.data.DBOpenHelper
import javax.inject.Inject

/**
 * Base class for content providers that previously used Dagger Android injection.
 *
 * NOTE: This class is DEPRECATED with Hilt. ContentProviders should use @AndroidEntryPoint annotation instead.
 * This class is kept as a simple ContentProvider extension for backward compatibility,
 * but all injection functionality has been removed.
 *
 * ContentProviders extending this class should add @AndroidEntryPoint annotation to enable Hilt injection.
 */
abstract class CommonsDaggerContentProvider : ContentProvider() {
    @JvmField
    @Inject
    var dbOpenHelper: DBOpenHelper? = null

    fun requireDbOpenHelper(): DBOpenHelper = dbOpenHelper!!

    fun requireDb(): SQLiteDatabase = requireDbOpenHelper().writableDatabase!!
}

