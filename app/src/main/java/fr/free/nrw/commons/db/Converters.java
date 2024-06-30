package fr.free.nrw.commons.db;

import android.net.Uri;
import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.contributions.ChunkInfo;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.Sitelinks;
import fr.free.nrw.commons.upload.WikidataPlace;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This class supplies converters to write/read types to/from the database.
 */
public class Converters {

    public static Gson getGson() {
        return ApplicationlessInjection.getInstance(CommonsApplication.getInstance()).getCommonsApplicationComponent().gson();
    }

    /**
     * convert DepictedItem object to string
     * input Example -> DepictedItem depictedItem=new DepictedItem ()
     * output Example -> string
     */
    @TypeConverter
    public static String depictsItemToString(DepictedItem objects) {
        return writeObjectToString(objects);
    }

    /**
     * convert string to DepictedItem object
     * output Example -> DepictedItem depictedItem=new DepictedItem ()
     * input Example -> string 
     */
    @TypeConverter
    public static DepictedItem stringToDepicts(String objectList) {
        return readObjectWithTypeToken(objectList, new TypeToken<DepictedItem>() {
        });
    }

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static Uri fromString(String value) {
        return value == null ? null : Uri.parse(value);
    }

    @TypeConverter
    public static String uriToString(Uri uri) {
        return uri == null ? null : uri.toString();
    }

    @TypeConverter
    public static String listObjectToString(List<String> objectList) {
        return writeObjectToString(objectList);
    }

    @TypeConverter
    public static List<String> stringToListObject(String objectList) {
        return readObjectWithTypeToken(objectList, new TypeToken<List<String>>() {});
    }

    @TypeConverter
    public static String mapObjectToString(Map<String,String> objectList) {
        return writeObjectToString(objectList);
    }

    @TypeConverter
    public static String mapObjectToString2(Map<String,Boolean> objectList) {
        return writeObjectToString(objectList);
    }

    @TypeConverter
    public static Map<String,String> stringToMap(String objectList) {
        return readObjectWithTypeToken(objectList, new TypeToken<Map<String,String>>(){});
    }

    @TypeConverter
    public static Map<String,Boolean> stringToMap2(String objectList) {
        return readObjectWithTypeToken(objectList, new TypeToken<Map<String,Boolean>>(){});
    }

    @TypeConverter
    public static String latlngObjectToString(LatLng latlng) {
        return writeObjectToString(latlng);
    }

    @TypeConverter
    public static LatLng stringToLatLng(String objectList) {
        return readObjectFromString(objectList,LatLng.class);
    }

    @TypeConverter
    public static String wikidataPlaceToString(WikidataPlace wikidataPlace) {
        return writeObjectToString(wikidataPlace);
    }

    @TypeConverter
    public static WikidataPlace stringToWikidataPlace(String wikidataPlace) {
        return readObjectFromString(wikidataPlace, WikidataPlace.class);
    }

    @TypeConverter
    public static String chunkInfoToString(ChunkInfo chunkInfo) {
        return writeObjectToString(chunkInfo);
    }

    @TypeConverter
    public static ChunkInfo stringToChunkInfo(String chunkInfo) {
        return readObjectFromString(chunkInfo, ChunkInfo.class);
    }

    @TypeConverter
    public static String depictionListToString(List<DepictedItem> depictedItems) {
        return writeObjectToString(depictedItems);
    }

    @TypeConverter
    public static List<DepictedItem> stringToList(String depictedItems) {
        return readObjectWithTypeToken(depictedItems, new TypeToken<List<DepictedItem>>() {});
    }

    @TypeConverter
    public static Sitelinks sitelinksFromString(String value) {
        Type type = new TypeToken<Sitelinks>() {}.getType();
        return new Gson().fromJson(value, type);
    }

    @TypeConverter
    public static String fromSitelinks(Sitelinks sitelinks) {
        Gson gson = new Gson();
        return gson.toJson(sitelinks);
    }

    private static String writeObjectToString(Object object) {
        return object == null ? null : getGson().toJson(object);
    }

    private static<T> T readObjectFromString(String objectAsString, Class<T> clazz) {
        return objectAsString == null ? null : getGson().fromJson(objectAsString, clazz);
    }

    private static <T> T readObjectWithTypeToken(String objectList, TypeToken<T> typeToken) {
        return objectList == null ? null : getGson().fromJson(objectList, typeToken.getType());
    }
}
