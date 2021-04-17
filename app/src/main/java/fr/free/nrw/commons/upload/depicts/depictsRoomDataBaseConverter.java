package fr.free.nrw.commons.upload.depicts;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import java.util.Date;
import java.util.List;

public class depictsRoomDataBaseConverter {

  public static Gson getGson() {
    return ApplicationlessInjection.getInstance(CommonsApplication.getInstance())
        .getCommonsApplicationComponent().gson();
  }


  @TypeConverter
  public static String depictsItemToString(DepictedItem objects) {
    return writeObjectToString(objects);
  }

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


  private static String writeObjectToString(Object object) {
    return object == null ? null : getGson().toJson(object);
  }

  private static <T> T readObjectWithTypeToken(String objectList, TypeToken<T> typeToken) {
    return objectList == null ? null : getGson().fromJson(objectList, typeToken.getType());
  }

}
