package fr.free.nrw.commons.di

import android.content.ContentProvider
import androidx.sqlite.db.SupportSQLiteDatabase
import fr.free.nrw.commons.data.DBOpenHelper
import fr.free.nrw.commons.di.ApplicationlessInjection.Companion.getInstance
import javax.inject.Inject

abstract class CommonsDaggerContentProvider : ContentProvider() {
    @JvmField
    @Inject
    var dbOpenHelper: DBOpenHelper? = null

    override fun onCreate(): Boolean {
        inject()
        return true
    }

    fun requireDbOpenHelper(): DBOpenHelper = dbOpenHelper!!

    fun requireDb(): SupportSQLiteDatabase = requireDbOpenHelper().writableDatabase!!

    private fun inject() {
        val injection = getInstance(context!!)

        val serviceInjector = injection.contentProviderInjector()
            ?: throw NullPointerException("ApplicationlessInjection.contentProviderInjector() returned null")

        serviceInjector.inject(this)
    }
}
