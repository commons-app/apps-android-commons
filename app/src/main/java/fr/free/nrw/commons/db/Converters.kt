package fr.free.nrw.commons.db

import android.net.Uri
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.contributions.ChunkInfo
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Sitelinks
import fr.free.nrw.commons.upload.WikidataPlace
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import java.util.Date

/**
 * Entry point for accessing Gson from Room converters
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ConvertersEntryPoint {
    fun gson(): Gson
}

/**
 * This object supplies converters to write/read types to/from the database.
 */
object Converters {

    fun getGson(): Gson {
        val appContext = CommonsApplication.instance
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            ConvertersEntryPoint::class.java
        )
        return entryPoint.gson()
    }

    /**
     * convert DepictedItem object to string
     * input Example -> DepictedItem depictedItem=new DepictedItem ()
     * output Example -> string
     */
    @TypeConverter
    @JvmStatic
    fun depictsItemToString(objects: DepictedItem?): String? {
        return writeObjectToString(objects)
    }

    /**
     * convert string to DepictedItem object
     * output Example -> DepictedItem depictedItem=new DepictedItem ()
     * input Example -> string
     */
    @TypeConverter
    @JvmStatic
    fun stringToDepicts(objectList: String?): DepictedItem? {
        return readObjectWithTypeToken(objectList, object : TypeToken<DepictedItem>() {})
    }

    @TypeConverter
    @JvmStatic
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    @JvmStatic
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    @JvmStatic
    fun fromString(value: String?): Uri? {
        return value?.let { Uri.parse(it) }
    }

    @TypeConverter
    @JvmStatic
    fun uriToString(uri: Uri?): String? {
        return uri?.toString()
    }

    @TypeConverter
    @JvmStatic
    fun listObjectToString(objectList: List<String>?): String? {
        return writeObjectToString(objectList)
    }

    @TypeConverter
    @JvmStatic
    fun stringToListObject(objectList: String?): List<String>? {
        return readObjectWithTypeToken(objectList, object : TypeToken<List<String>>() {})
    }

    @TypeConverter
    @JvmStatic
    fun mapObjectToString(objectList: Map<String, String>?): String? {
        return writeObjectToString(objectList)
    }

    @TypeConverter
    @JvmStatic
    fun mapObjectToString2(objectList: Map<String, Boolean>?): String? {
        return writeObjectToString(objectList)
    }

    @TypeConverter
    @JvmStatic
    fun stringToMap(objectList: String?): Map<String, String>? {
        return readObjectWithTypeToken(objectList, object : TypeToken<Map<String, String>>() {})
    }

    @TypeConverter
    @JvmStatic
    fun stringToMap2(objectList: String?): Map<String, Boolean>? {
        return readObjectWithTypeToken(objectList, object : TypeToken<Map<String, Boolean>>() {})
    }

    @TypeConverter
    @JvmStatic
    fun latlngObjectToString(latlng: LatLng?): String? {
        return writeObjectToString(latlng)
    }

    @TypeConverter
    @JvmStatic
    fun stringToLatLng(objectList: String?): LatLng? {
        return readObjectFromString(objectList, LatLng::class.java)
    }

    @TypeConverter
    @JvmStatic
    fun wikidataPlaceToString(wikidataPlace: WikidataPlace?): String? {
        return writeObjectToString(wikidataPlace)
    }

    @TypeConverter
    @JvmStatic
    fun stringToWikidataPlace(wikidataPlace: String?): WikidataPlace? {
        return readObjectFromString(wikidataPlace, WikidataPlace::class.java)
    }

    @TypeConverter
    @JvmStatic
    fun chunkInfoToString(chunkInfo: ChunkInfo?): String? {
        return writeObjectToString(chunkInfo)
    }

    @TypeConverter
    @JvmStatic
    fun stringToChunkInfo(chunkInfo: String?): ChunkInfo? {
        return readObjectFromString(chunkInfo, ChunkInfo::class.java)
    }

    @TypeConverter
    @JvmStatic
    fun depictionListToString(depictedItems: List<DepictedItem>?): String? {
        return writeObjectToString(depictedItems)
    }

    @TypeConverter
    @JvmStatic
    fun stringToList(depictedItems: String?): List<DepictedItem>? {
        return readObjectWithTypeToken(depictedItems, object : TypeToken<List<DepictedItem>>() {})
    }

    @TypeConverter
    @JvmStatic
    fun sitelinksFromString(value: String?): Sitelinks? {
        val type = object : TypeToken<Sitelinks>() {}.type
        return Gson().fromJson(value, type)
    }

    @TypeConverter
    @JvmStatic
    fun fromSitelinks(sitelinks: Sitelinks?): String? {
        return Gson().toJson(sitelinks)
    }

    private fun writeObjectToString(`object`: Any?): String? {
        return `object`?.let { getGson().toJson(it) }
    }

    private fun <T> readObjectFromString(objectAsString: String?, clazz: Class<T>): T? {
        return objectAsString?.let { getGson().fromJson(it, clazz) }
    }

    private fun <T> readObjectWithTypeToken(objectList: String?, typeToken: TypeToken<T>): T? {
        return objectList?.let { getGson().fromJson(it, typeToken.type) }
    }
}
