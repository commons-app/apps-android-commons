package fr.free.nrw.commons.di;

import android.content.ContentProvider;
import dagger.android.AndroidInjector;


public abstract class CommonsDaggerContentProvider extends ContentProvider {

  public CommonsDaggerContentProvider() {
    super();
  }

  @Override
  public boolean onCreate() {
    inject();
    return true;
  }

  private void inject() {
    ApplicationlessInjection injection = ApplicationlessInjection.getInstance(getContext());

    AndroidInjector<ContentProvider> serviceInjector = injection.contentProviderInjector();

    if (serviceInjector == null) {
      throw new NullPointerException(
          "ApplicationlessInjection.contentProviderInjector() returned null");
    }

    serviceInjector.inject(this);
  }

}
