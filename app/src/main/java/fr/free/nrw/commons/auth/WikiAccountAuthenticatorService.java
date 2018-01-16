package fr.free.nrw.commons.auth;

import android.content.Intent;
import android.os.IBinder;

import javax.inject.Inject;

import fr.free.nrw.commons.di.CommonsDaggerService;
import fr.free.nrw.commons.mwapi.MediaWikiApi;

import static android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT;

public class WikiAccountAuthenticatorService extends CommonsDaggerService {

    @Inject MediaWikiApi mwApi;
    private WikiAccountAuthenticator wikiAccountAuthenticator = null;

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
