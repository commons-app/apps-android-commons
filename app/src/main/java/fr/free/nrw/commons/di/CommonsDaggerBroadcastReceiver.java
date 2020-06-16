package fr.free.nrw.commons.di;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import dagger.android.AndroidInjector;

/**
 * Receives broadcast then injects it's instance to the broadcastReceiverInjector method of
 * ApplicationlessInjection class
 */
public abstract class CommonsDaggerBroadcastReceiver extends BroadcastReceiver {

  public CommonsDaggerBroadcastReceiver() {
    super();
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    inject(context);
  }

  private void inject(Context context) {
    ApplicationlessInjection injection = ApplicationlessInjection
        .getInstance(context.getApplicationContext());

    AndroidInjector<BroadcastReceiver> serviceInjector = injection.broadcastReceiverInjector();

    if (serviceInjector == null) {
      throw new NullPointerException(
          "ApplicationlessInjection.broadcastReceiverInjector() returned null");
    }
    serviceInjector.inject(this);
  }

}
