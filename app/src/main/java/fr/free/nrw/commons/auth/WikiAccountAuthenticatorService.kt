package fr.free.nrw.commons.auth

import android.accounts.AbstractAccountAuthenticator
import android.content.Intent
import android.os.IBinder
import fr.free.nrw.commons.di.CommonsDaggerService

/**
 * Handles the Auth service of the App, see AndroidManifests for details
 * (Uses Dagger 2 as injector)
 */
class WikiAccountAuthenticatorService : CommonsDaggerService() {

    private var authenticator: AbstractAccountAuthenticator? = null

    override fun onCreate() {
        super.onCreate()
        authenticator = WikiAccountAuthenticator(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        return if (authenticator == null) null else authenticator!!.iBinder
    }
}