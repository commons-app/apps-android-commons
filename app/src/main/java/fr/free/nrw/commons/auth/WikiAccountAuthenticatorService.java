package fr.free.nrw.commons.auth;

import android.accounts.AccountManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import javax.inject.Inject;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.mwapi.MediaWikiApi;

public class WikiAccountAuthenticatorService extends Service {

    @Inject MediaWikiApi mwApi;
    @Inject AccountUtil accountUtil;
    private WikiAccountAuthenticator wikiAccountAuthenticator = null;

    @Override
    public IBinder onBind(Intent intent) {
        if (!intent.getAction().equals(AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
            return null;
        }

        ((CommonsApplication)getApplication()).injector().inject(this);
        if (wikiAccountAuthenticator == null) {
            wikiAccountAuthenticator = new WikiAccountAuthenticator(this, accountUtil, mwApi);
        }
        return wikiAccountAuthenticator.getIBinder();
    }

}
