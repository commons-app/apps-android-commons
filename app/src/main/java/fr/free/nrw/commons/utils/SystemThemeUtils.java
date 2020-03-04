package fr.free.nrw.commons.utils;

import android.content.Context;
import android.content.res.Configuration;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.settings.Prefs;

public class SystemThemeUtils {

    private Context context;

    @Inject
    @Named("default_preferences")
    JsonKvStore applicationKvStore;

    @Inject
    public SystemThemeUtils(Context context) {
        this.context = context;
    }

    // Return true is system wide dark theme is enabled else false
    public boolean getSystemDefaultThemeBool(String theme) {
        if (context.getResources().getString(R.string.theme_dark_name).equals(theme)) {
            return true;
        } else if (context.getResources().getString(R.string.theme_default_name).equals(theme)) {
            return getSystemDefaultThemeBool(getSystemDefaultTheme());
        }
        return false;
    }

    // Returns the default system wide theme
    public String getSystemDefaultTheme() {
        if ((context.getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            return context.getResources().getString(R.string.theme_dark_name);
        }
        return context.getResources().getString(R.string.theme_light_name);
    }

    // Returns true if the device is in night mode or false otherwise
    public boolean isDeviceInNightMode() {
        return getSystemDefaultThemeBool(
                applicationKvStore.getString(Prefs.KEY_THEME_VALUE, getSystemDefaultTheme()));
    }

}
