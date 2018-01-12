package fr.free.nrw.commons.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dagger.android.DaggerService;
import fr.free.nrw.commons.di.FixedDaggerService;
import fr.free.nrw.commons.mwapi.MediaWikiApi;

import static android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT;

public class WikiAccountAuthenticatorService extends FixedDaggerService {

    @Inject MediaWikiApi mwApi;
    private WikiAccountAuthenticator wikiAccountAuthenticator = null;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (!intent.getAction().equals(ACTION_AUTHENTICATOR_INTENT)) {
            return null;
        }

        if (wikiAccountAuthenticator == null) {
            wikiAccountAuthenticator = new WikiAccountAuthenticator(this, mwApi);
        }
        return wikiAccountAuthenticator.getIBinder();
    }

}
