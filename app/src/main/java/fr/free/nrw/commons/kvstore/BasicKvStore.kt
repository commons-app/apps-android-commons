package fr.free.nrw.commons.kvstore

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import timber.log.Timber

open class BasicKvStore : KeyValueStore {
    /*
    This class only performs puts, sets and clears.
    A commit returns a boolean indicating whether it has succeeded, we are not throwing an exception as it will
    require the dev to handle it in every usage - instead we will pass on this boolean so it can be evaluated if needed.
    */
    private val _store: SharedPreferences

    constructor(context: Context, storeName: String?) {
        _store = context.getSharedPreferences(storeName, Context.MODE_PRIVATE)
    }

    /**
     * If you don't want onVersionUpdate to be called on a fresh creation, the first version supplied for the kvstore should be set to 0.
     */
    @JvmOverloads
    constructor(
        context: Context,
        storeName: String?,
        version: Int,
        clearAllOnUpgrade: Boolean = false
    ) {
        _store = context.getSharedPreferences(storeName, Context.MODE_PRIVATE)
        val oldVersion = _store.getInt(KEY_VERSION, 0)

        require(version >= oldVersion) {
            "kvstore downgrade not allowed, old version:" + oldVersion + ", new version: " +
                    version
        }

        if (version > oldVersion) {
            Timber.i(
                "version updated from %s to %s, with clearFlag %b",
                oldVersion,
                version,
                clearAllOnUpgrade
            )
            onVersionUpdate(oldVersion, version, clearAllOnUpgrade)
        }

        //Keep this statement at the end so that clearing of store does not cause version also to get removed.
        _store.edit { putInt(KEY_VERSION, version) }
    }

    val all: Map<String, *>?
        get() {
            val allContents = _store.all
            if (allContents == null || allContents.isEmpty()) {
                return null
            }
            allContents.remove(KEY_VERSION)
            return HashMap(allContents)
        }

    override fun getString(key: String): String? =
        getString(key, null)

    override fun getBoolean(key: String): Boolean =
        getBoolean(key, false)

    override fun getLong(key: String): Long =
        getLong(key, 0)

    override fun getInt(key: String): Int =
        getInt(key, 0)

    fun getStringSet(key: String?): MutableSet<String> =
        _store.getStringSet(key, HashSet())!!

    override fun getString(key: String, defaultValue: String?): String? =
        _store.getString(key, defaultValue)

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        _store.getBoolean(key, defaultValue)

    override fun getLong(key: String, defaultValue: Long): Long =
        _store.getLong(key, defaultValue)

    override fun getInt(key: String, defaultValue: Int): Int =
        _store.getInt(key, defaultValue)

    fun putAllStrings(kvData: Map<String, String>) = assertKeyNotReserved(kvData.keys) {
        for ((key, value) in kvData) {
            putString(key, value)
        }
    }

    override fun putString(key: String, value: String) = assertKeyNotReserved(key) {
        putString(key, value)
    }

    override fun putBoolean(key: String, value: Boolean) = assertKeyNotReserved(key) {
        putBoolean(key, value)
    }

    override fun putLong(key: String, value: Long) = assertKeyNotReserved(key) {
        putLong(key, value)
    }

    override fun putInt(key: String, value: Int) = assertKeyNotReserved(key) {
        putInt(key, value)
    }

    fun putStringSet(key: String?, value: Set<String?>?) =
        _store.edit{ putStringSet(key, value) }

    override fun remove(key: String) = assertKeyNotReserved(key) {
        remove(key)
    }

    override fun contains(key: String): Boolean {
        if (key == KEY_VERSION) return false
        return _store.contains(key)
    }

    override fun clearAll() {
        val version = _store.getInt(KEY_VERSION, 0)
        _store.edit {
            clear()
            putInt(KEY_VERSION, version)
        }
    }

    private fun onVersionUpdate(oldVersion: Int, version: Int, clearAllFlag: Boolean) {
        if (clearAllFlag) {
            clearAll()
        }
    }

    private fun assertKeyNotReserved(key: Set<String>, block: SharedPreferences.Editor.() -> Unit) {
        key.forEach { require(it != KEY_VERSION) { "$it is a reserved key" } }
        _store.edit { block(this) }
    }

    private fun assertKeyNotReserved(key: String, block: SharedPreferences.Editor.() -> Unit) {
        require(key != KEY_VERSION) { "$key is a reserved key" }
        _store.edit { block(this) }
    }

    companion object {
        @VisibleForTesting
        const val KEY_VERSION: String = "__version__"
    }
}