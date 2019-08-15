package fr.free.nrw.commons.modifications;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ModificationsSyncService extends Service {

    private static final Object sSyncAdapterLock = new Object();

    private static ModificationsSyncAdapter sSyncAdapter = null;

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new ModificationsSyncAdapter(this, true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
