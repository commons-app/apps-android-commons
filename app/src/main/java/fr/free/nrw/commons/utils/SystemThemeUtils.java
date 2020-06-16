package fr.free.nrw.commons.utils;

import android.content.Context;
import android.content.res.Configuration;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.settings.Prefs;
import javax.inject.Inject;
import javax.inject.Named;

public class SystemThemeUtils {

  private Context context;
  private JsonKvStore applicationKvStore;

  public static final String THEME_MODE_DEFAULT = "0";
  public static final String THEME_MODE_DARK = "1";
  public static final String THEME_MODE_LIGHT = "2";

  @Inject
  public SystemThemeUtils(Context context,
      @Named("default_preferences") JsonKvStore applicationKvStore) {
    this.context = context;
    this.applicationKvStore = applicationKvStore;
  }

  // Return true is system wide dark theme is enabled else false
  public boolean getSystemDefaultThemeBool(String theme) {
    if (theme.equals(THEME_MODE_DARK)) {
      return true;
    } else if (theme.equals(THEME_MODE_DEFAULT)) {
      return getSystemDefaultThemeBool(getSystemDefaultTheme());
    }
    return false;
  }

  // Returns the default system wide theme
  public String getSystemDefaultTheme() {
    return (context.getResources().getConfiguration().uiMode &
        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES ? THEME_MODE_DARK
        : THEME_MODE_LIGHT;
  }

  // Returns true if the device is in night mode or false otherwise
  public boolean isDeviceInNightMode() {
    return getSystemDefaultThemeBool(
        applicationKvStore.getString(Prefs.KEY_THEME_VALUE, getSystemDefaultTheme()));
  }

}
