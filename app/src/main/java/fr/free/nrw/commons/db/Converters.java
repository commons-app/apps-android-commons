package fr.free.nrw.commons.db;

import android.net.Uri;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.wikipedia.json.GsonUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.location.LatLng;

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
    public static HashMap<String,String> stringToMap(String objectList) {
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

}
