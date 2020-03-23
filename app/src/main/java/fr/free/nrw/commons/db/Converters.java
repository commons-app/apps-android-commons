package fr.free.nrw.commons.db;

import android.net.Uri;
import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.location.LatLng;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This class supplies converters to write/read types to/from the database.
 */
public class Converters {

    public static Gson getGson() {
        return ApplicationlessInjection.getInstance(CommonsApplication.getInstance()).getCommonsApplicationComponent().gson();
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
    public static String listObjectToString(ArrayList<String> objectList) {
        return objectList == null ? null : getGson().toJson(objectList);
    }

    @TypeConverter
    public static ArrayList<String> stringToArrayListObject(String objectList) {
        return objectList == null ? null : getGson().fromJson(objectList,new TypeToken<ArrayList<String>>(){}.getType());
    }

    @TypeConverter
    public static String mapObjectToString(HashMap<String,String> objectList) {
        return objectList == null ? null : getGson().toJson(objectList);
    }

    @TypeConverter
    public static HashMap<String,String> stringToHashMap(String objectList) {
        return objectList == null ? null : getGson().fromJson(objectList,new TypeToken<HashMap<String,String>>(){}.getType());
    }

    @TypeConverter
    public static String latlngObjectToString(LatLng latlng) {
        return latlng == null ? null : getGson().toJson(latlng);
    }

    @TypeConverter
    public static LatLng stringToLatLng(String objectList) {
        return objectList == null ? null : getGson().fromJson(objectList,LatLng.class);
    }

    @TypeConverter
    public static String listOfMapToString(ArrayList<Map<String,String>> listOfMaps) {
        return listOfMaps == null ? null : getGson().toJson(listOfMaps);
    }

    @TypeConverter
    public static ArrayList<Map<String,String>> stringToListOfMap(String listOfMaps) {
        return listOfMaps == null ? null :getGson().fromJson(listOfMaps,new TypeToken<ArrayList<Map<String,String>>>(){}.getType());
    }

    @TypeConverter
    public static String mapToString(Map<String,String> map) {
        return map == null ? null : getGson().toJson(map);
    }

    @TypeConverter
    public static Map<String,String> stringToMap(String map) {
        return map == null ? null :getGson().fromJson(map,new TypeToken<Map<String,String>>(){}.getType());
    }

}
