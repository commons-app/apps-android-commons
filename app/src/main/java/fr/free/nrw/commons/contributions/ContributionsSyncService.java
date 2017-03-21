package fr.free.nrw.commons.contributions;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ContributionsSyncService extends Service {

    private static final Object sSyncAdapterLock = new Object();

    private static ContributionsSyncAdapter sSyncAdapter = null;

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new ContributionsSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
