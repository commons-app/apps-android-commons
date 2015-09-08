package fr.nrw.free.commons.contributions;

import android.app.*;
import android.content.*;
import android.os.*;

public class ContributionsSyncService extends Service {

    private static final Object sSyncAdapterLock = new Object();

    private static ContributionsSyncAdapter sSyncAdapter = null;

    @Override
    public void onCreate() {
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
