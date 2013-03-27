package org.wikimedia.commons.modifications;

import android.app.*;
import android.content.*;
import android.os.*;

public class ModificationsSyncService extends Service {

    private static final Object sSyncAdapterLock = new Object();

    private static ModificationsSyncAdapter sSyncAdapter = null;

    @Override
    public void onCreate() {
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new ModificationsSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
