package fr.nrw.free.commons.auth;

import android.app.*;
import android.content.*;
import android.os.*;

public class WikiAccountAuthenticatorService extends Service{

    private static WikiAccountAuthenticator wikiAccountAuthenticator = null;
    
    @Override
    public IBinder onBind(Intent intent) {
        if (!intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
           return null; 
        }

        if(wikiAccountAuthenticator == null) {
            wikiAccountAuthenticator = new WikiAccountAuthenticator(this);
        }
        return wikiAccountAuthenticator.getIBinder();
    }

}
