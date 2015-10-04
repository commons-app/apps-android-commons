package free.nrw.commons.campaigns;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class CampaignsSyncService extends Service {

    private static final Object sSyncAdapterLock = new Object();

    private static CampaignsSyncAdapter sSyncAdapter = null;

    @Override
    public void onCreate() {
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new CampaignsSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
