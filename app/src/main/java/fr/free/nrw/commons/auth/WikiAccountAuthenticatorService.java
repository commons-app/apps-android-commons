package fr.free.nrw.commons.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import fr.free.nrw.commons.di.CommonsDaggerService;

public class WikiAccountAuthenticatorService extends CommonsDaggerService {

    @Nullable
    private AbstractAccountAuthenticator authenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        authenticator = new WikiAccountAuthenticator(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return authenticator == null ? null : authenticator.getIBinder();
    }
}
