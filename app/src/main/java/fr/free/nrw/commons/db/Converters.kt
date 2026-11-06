package fr.free.nrw.commons.db

import android.net.Uri
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.contributions.ChunkInfo
import fr.free.nrw.commons.di.ApplicationlessInjection
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Sitelinks
import fr.free.nrw.commons.upload.WikidataPlace
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import java.util.Date

/**
 * This object supplies converters to write/read types to/from the database.
 */
object Converters {
    fun getGson(): Gson =
        ApplicationlessInjection
            .getInstance(CommonsApplication.instance)
            .commonsApplicationComponent
            .gson()

    /**
     * convert DepictedItem object to string
     * input Example -> DepictedItem depictedItem=new DepictedItem ()
     * output Example -> string
     */
    @TypeConverter
    @JvmStatic
    fun depictsItemToString(objects: DepictedItem?): String? = writeObjectToString(objects)

    /**
     * convert string to DepictedItem object
     * output Example -> DepictedItem depictedItem=new DepictedItem ()
     * input Example -> string
     */
    @TypeConverter
    @JvmStatic
    fun stringToDepicts(objectList: String?): DepictedItem? =
        readObjectWithTypeToken(objectList, object : TypeToken<DepictedItem>() {})

    @TypeConverter
    @JvmStatic
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    @JvmStatic
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    @JvmStatic
    fun fromString(value: String?): Uri? = value?.let { Uri.parse(it) }

    @TypeConverter
    @JvmStatic
    fun uriToString(uri: Uri?): String? = uri?.toString()

    @TypeConverter
    @JvmStatic
    fun listObjectToString(objectList: List<String>?): String? = writeObjectToString(objectList)

    @TypeConverter
    @JvmStatic
    fun stringToListObject(objectList: String?): List<String>? =
        readObjectWithTypeToken(objectList, object : TypeToken<List<String>>() {})

    @TypeConverter
    @JvmStatic
    fun mapObjectToString(objectList: Map<String, String>?): String? = writeObjectToString(objectList)

    @TypeConverter
    @JvmStatic
    fun mapObjectToString2(objectList: Map<String, Boolean>?): String? = writeObjectToString(objectList)

    @TypeConverter
    @JvmStatic
    fun stringToMap(objectList: String?): Map<String, String>? =
        readObjectWithTypeToken(objectList, object : TypeToken<Map<String, String>>() {})

    @TypeConverter
    @JvmStatic
    fun stringToMap2(objectList: String?): Map<String, Boolean>? =
        readObjectWithTypeToken(objectList, object : TypeToken<Map<String, Boolean>>() {})

    @TypeConverter
    @JvmStatic
    fun latlngObjectToString(latlng: LatLng?): String? = writeObjectToString(latlng)

    @TypeConverter
    @JvmStatic
    fun stringToLatLng(objectList: String?): LatLng? = readObjectFromString(objectList, LatLng::class.java)

    @TypeConverter
    @JvmStatic
    fun wikidataPlaceToString(wikidataPlace: WikidataPlace?): String? = writeObjectToString(wikidataPlace)

    @TypeConverter
    @JvmStatic
    fun stringToWikidataPlace(wikidataPlace: String?): WikidataPlace? =
        readObjectFromString(wikidataPlace, WikidataPlace::class.java)

    @TypeConverter
    @JvmStatic
    fun chunkInfoToString(chunkInfo: ChunkInfo?): String? = writeObjectToString(chunkInfo)

    @TypeConverter
    @JvmStatic
    fun stringToChunkInfo(chunkInfo: String?): ChunkInfo? = readObjectFromString(chunkInfo, ChunkInfo::class.java)

    @TypeConverter
    @JvmStatic
    fun depictionListToString(depictedItems: List<DepictedItem>?): String? = writeObjectToString(depictedItems)

    @TypeConverter
    @JvmStatic
    fun stringToList(depictedItems: String?): List<DepictedItem>? =
        readObjectWithTypeToken(depictedItems, object : TypeToken<List<DepictedItem>>() {})

    @TypeConverter
    @JvmStatic
    fun sitelinksFromString(value: String?): Sitelinks? {
        val type = object : TypeToken<Sitelinks>() {}.type
        return Gson().fromJson(value, type)
    }

    @TypeConverter
    @JvmStatic
    fun fromSitelinks(sitelinks: Sitelinks?): String? = Gson().toJson(sitelinks)

    private fun writeObjectToString(obj: Any?): String? = obj?.let { getGson().toJson(it) }

    private fun <T> readObjectFromString(
        objectAsString: String?,
        clazz: Class<T>,
    ): T? =
        objectAsString?.let {
            getGson().fromJson(it, clazz)
        }

    private fun <T> readObjectWithTypeToken(
        objectList: String?,
        typeToken: TypeToken<T>,
    ): T? =
        objectList?.let {
            getGson().fromJson(it, typeToken.type)
        }
}
