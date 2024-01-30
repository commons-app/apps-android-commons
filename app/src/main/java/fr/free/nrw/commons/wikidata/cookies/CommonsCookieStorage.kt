package fr.free.nrw.commons.wikidata.cookies

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import fr.free.nrw.commons.kvstore.JsonKvStore
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.wikipedia.dataclient.WikiSite

private const val COOKIE_STORE = "cookie_store"

class CommonsCookieStorage(private val preferences: JsonKvStore? = null) {
    private val gson = GsonBuilder().registerTypeAdapter(
            CommonsCookieStorage::class.java,
            CookieStorageTypeAdapter()
        ).create()
    private val cookieMap: MutableMap<String, List<Cookie>> = mutableMapOf()

    val domains : Set<String> get() = cookieMap.keys.toSet()

    operator fun set(domainSpec: String, cookies: MutableList<Cookie>) =
        cookieMap.put(domainSpec, cookies.toList())

    operator fun get(domainSpec: String): MutableList<Cookie> =
        cookieMap[domainSpec]?.toMutableList() ?: mutableListOf()

    fun clear() {
        cookieMap.clear()
        save()
    }

    fun load() {
        cookieMap.clear()
        val json = preferences!!.getString(COOKIE_STORE, null)
        if (!json.isNullOrEmpty()) {
            val serializedData = gson.fromJson(json, CommonsCookieStorage::class.java)
            cookieMap.putAll(serializedData.cookieMap)
        }
    }

    fun save() =
        preferences!!.putString(COOKIE_STORE, gson.toJson(this))

    fun contains(domainSpec: String): Boolean =
        cookieMap.containsKey(domainSpec)

    companion object {
        fun from(map: Map<String, List<Cookie>>) = CommonsCookieStorage().apply {
            cookieMap.clear()
            cookieMap.putAll(map)
        }
    }
}

private class CookieStorageTypeAdapter : TypeAdapter<CommonsCookieStorage>() {
    override fun write(out: JsonWriter, value: CommonsCookieStorage) {
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