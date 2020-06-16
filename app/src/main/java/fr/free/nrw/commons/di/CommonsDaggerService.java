package fr.free.nrw.commons.di;

import android.app.Service;
import dagger.android.AndroidInjector;

public abstract class CommonsDaggerService extends Service {

  public CommonsDaggerService() {
    super();
  }

  @Override
  public void onCreate() {
    inject();
    super.onCreate();
  }

  private void inject() {
    ApplicationlessInjection injection = ApplicationlessInjection
        .getInstance(getApplicationContext());

    AndroidInjector<Service> serviceInjector = injection.serviceInjector();

    if (serviceInjector == null) {
      throw new NullPointerException("ApplicationlessInjection.serviceInjector() returned null");
    }

    serviceInjector.inject(this);
  }

}
