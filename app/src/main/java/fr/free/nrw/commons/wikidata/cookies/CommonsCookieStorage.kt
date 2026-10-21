package fr.free.nrw.commons.wikidata.cookies

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import fr.free.nrw.commons.crypto.CryptoUtils
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.wikidata.model.WikiSite
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private const val COOKIE_STORE = "cookie_store"
private const val TAG = "CommonsCookieStorage"

class CommonsCookieStorage(
    private val preferences: JsonKvStore? = null,
) {
    private val gson =
        GsonBuilder()
            .registerTypeAdapter(
                CommonsCookieStorage::class.java,
                CookieStorageTypeAdapter(),
            ).create()
    private val cookieMap: MutableMap<String, List<Cookie>> = mutableMapOf()

    val domains: Set<String> get() = cookieMap.keys.toSet()

    operator fun set(
        domainSpec: String,
        cookies: MutableList<Cookie>,
    ) = cookieMap.put(domainSpec, cookies.toList())

    operator fun get(domainSpec: String): MutableList<Cookie> = cookieMap[domainSpec]?.toMutableList() ?: mutableListOf()

    fun clear() {
        cookieMap.clear()
        save()
    }

    fun load() {
        cookieMap.clear()
        val encryptedJson = preferences!!.getString(COOKIE_STORE, null)
        val json = CryptoUtils.decrypt(encryptedJson)

        if (json.isNullOrBlank()) {
            if (encryptedJson != null) {
                discardUnreadableStore("Discarding empty cookie store")
            }
        } else {
            val failure = readStoredCookies(json)
            if (failure != null) {
                discardUnreadableStore("Discarding unreadable cookie store", failure)
            } else if (encryptedJson == json) {
                // Plaintext from an older app version. Re-save it encrypted.
                save()
            }
        }
    }

    /**
     * Loads [json] into [cookieMap].
     *
     * Returns the [JsonSyntaxException] when the saved value is not a cookie-store object.
     */
    private fun readStoredCookies(json: String): JsonSyntaxException? =
        try {
            val serializedData = gson.fromJson(json, CommonsCookieStorage::class.java)
            if (serializedData == null) {
                JsonSyntaxException("Cookie store must be a JSON object")
            } else {
                cookieMap.putAll(serializedData.cookieMap)
                null
            }
        } catch (exception: JsonSyntaxException) {
            cookieMap.clear()
            exception
        }

    /**
     * Forgets the saved cookie store and continues with no session.
     *
     * Uses [Log] because [load] runs during Dagger injection in application startup,
     * before Timber is planted.
     */
    private fun discardUnreadableStore(reason: String, exception: Exception? = null) {
        cookieMap.clear()
        if (exception == null) {
            Log.w(TAG, reason)
        } else {
            Log.w(TAG, reason, exception)
        }
        preferences!!.remove(COOKIE_STORE)
    }

    fun save() {
        val json = gson.toJson(this)
        val encryptedJson = CryptoUtils.encrypt(json)
        preferences!!.putString(COOKIE_STORE, encryptedJson ?: json)
    }

    fun contains(domainSpec: String): Boolean = cookieMap.containsKey(domainSpec)

    companion object {
        fun from(map: Map<String, List<Cookie>>) =
            CommonsCookieStorage().apply {
                cookieMap.clear()
                cookieMap.putAll(map)
            }
    }
}

private class CookieStorageTypeAdapter : TypeAdapter<CommonsCookieStorage>() {
    override fun write(
        out: JsonWriter,
        value: CommonsCookieStorage,
    ) {
        out.beginObject()
        value.domains.forEach { domain ->
            out.name(domain).beginArray()
            value[domain].forEach { out.value(it.toString()) }
            out.endArray()
        }
        out.endObject()
    }

    override fun read(input: JsonReader): CommonsCookieStorage {
        val map = mutableMapOf<String, List<Cookie>>()
        input.beginObject()
        while (input.hasNext()) {
            val key = input.nextName()
            map[key] = input.readCookies((WikiSite.DEFAULT_SCHEME + "://" + key).toHttpUrlOrNull())
        }
        input.endObject()
        return CommonsCookieStorage.from(map)
    }

    private fun JsonReader.readCookies(url: HttpUrl?): MutableList<Cookie> {
        val list = mutableListOf<Cookie>()
        beginArray()
        while (hasNext()) {
            val str = nextString()
            url?.let {
                val element: Cookie? = Cookie.parse(url, str)
                element?.let { list += element }
            }
        }
        endArray()
        return list
    }
}
