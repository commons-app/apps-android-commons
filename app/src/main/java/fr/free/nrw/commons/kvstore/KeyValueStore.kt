package fr.free.nrw.commons.kvstore

// Suppressed TooManyFunctions because this is the whole key-value storage API: a getter
// with and without a default and a setter for each supported type, plus contains,
// remove and clearAll.
@Suppress("TooManyFunctions")
interface KeyValueStore {
    fun getString(key: String): String?

    fun getBoolean(key: String): Boolean

    fun getLong(key: String): Long

    fun getInt(key: String): Int

    fun getString(
        key: String,
        defaultValue: String?,
    ): String?

    fun getBoolean(
        key: String,
        defaultValue: Boolean,
    ): Boolean

    fun getLong(
        key: String,
        defaultValue: Long,
    ): Long

    fun getInt(
        key: String,
        defaultValue: Int,
    ): Int

    fun putString(
        key: String,
        value: String,
    )

    fun putBoolean(
        key: String,
        value: Boolean,
    )

    fun putLong(
        key: String,
        value: Long,
    )

    fun putInt(
        key: String,
        value: Int,
    )

    fun contains(key: String): Boolean

    fun remove(key: String)

    fun clearAll()
}
