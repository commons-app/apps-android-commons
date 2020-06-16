package fr.free.nrw.commons.di;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import javax.inject.Inject;

public abstract class CommonsDaggerAppCompatActivity extends AppCompatActivity implements
    HasSupportFragmentInjector {

  @Inject
  DispatchingAndroidInjector<Fragment> supportFragmentInjector;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    inject();
    super.onCreate(savedInstanceState);
  }

  @Override
  public AndroidInjector<Fragment> supportFragmentInjector() {
    return supportFragmentInjector;
  }

  /**
   * when this Activity is created it injects an instance of this class inside activityInjector
   * method of ApplicationlessInjection
   */
  private void inject() {
    ApplicationlessInjection injection = ApplicationlessInjection
        .getInstance(getApplicationContext());

    AndroidInjector<Activity> activityInjector = injection.activityInjector();

    if (activityInjector == null) {
      throw new NullPointerException("ApplicationlessInjection.activityInjector() returned null");
    }

    activityInjector.inject(this);
  }

}
