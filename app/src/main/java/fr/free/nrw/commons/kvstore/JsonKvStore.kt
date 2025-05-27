package fr.free.nrw.commons.kvstore

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

class JsonKvStore : BasicKvStore {
    val gson: Gson

    constructor(context: Context, storeName: String?, gson: Gson) : super(context, storeName) {
        this.gson = gson
    }

    constructor(context: Context, storeName: String?, version: Int, gson: Gson) : super(
        context, storeName, version
    ) {
        this.gson = gson
    }

    constructor(
        context: Context,
        storeName: String?,
        version: Int,
        clearAllOnUpgrade: Boolean,
        gson: Gson
    ) : super(context, storeName, version, clearAllOnUpgrade) {
        this.gson = gson
    }

    fun <T> putJson(key: String, value: T) = assertKeyNotReserved(key) {
        putString(key, gson.toJson(value))
    }

    @Deprecated(
        message = "Migrate to newer Kotlin syntax",
        replaceWith = ReplaceWith("getJson<T>(key)")
    )
    fun <T> getJson(key: String, clazz: Class<T>?): T? = try {
        gson.fromJson(getString(key), clazz)
    } catch (e: JsonSyntaxException) {
        null
    }

    // Later, when the calls are coming from Kotlin, this will allow us to
    // drop the "clazz" parameter, and just pick up the type at the call site.
    // The deprecation warning should help migration!
    inline fun <reified T> getJson(key: String): T? = try {
        gson.fromJson(getString(key), T::class.java)
    } catch (e: JsonSyntaxException) {
        null
    }
}