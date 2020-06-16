package fr.free.nrw.commons.theme;

import android.os.Bundle;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerAppCompatActivity;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.utils.SystemThemeUtils;
import io.reactivex.disposables.CompositeDisposable;
import javax.inject.Inject;
import javax.inject.Named;

public abstract class BaseActivity extends CommonsDaggerAppCompatActivity {

  @Inject
  @Named("default_preferences")
  public JsonKvStore defaultKvStore;

  @Inject
  SystemThemeUtils systemThemeUtils;

  protected CompositeDisposable compositeDisposable = new CompositeDisposable();
  protected boolean wasPreviouslyDarkTheme;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    wasPreviouslyDarkTheme = systemThemeUtils.isDeviceInNightMode();
    setTheme(wasPreviouslyDarkTheme ? R.style.DarkAppTheme : R.style.LightAppTheme);
  }

  @Override
  protected void onResume() {
    // Restart activity if theme is changed
    if (wasPreviouslyDarkTheme != systemThemeUtils.isDeviceInNightMode()) {
      recreate();
    }

    super.onResume();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    compositeDisposable.clear();
  }
}
