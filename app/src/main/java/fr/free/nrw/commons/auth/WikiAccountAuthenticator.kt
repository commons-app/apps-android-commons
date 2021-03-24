package fr.free.nrw.commons.auth

import android.accounts.*
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.auth.AccountUtil.AUTH_TOKEN_TYPE

/**
 * Handles WikiMedia commons account Authentication
 */
class WikiAccountAuthenticator(private val context: Context) : AbstractAccountAuthenticator(context) {
    private val SYNC_AUTHORITIES = arrayOf(
        BuildConfig.CONTRIBUTION_AUTHORITY,
        BuildConfig.MODIFICATION_AUTHORITY
    )

    /**
     * Provides Bundle with edited Account Properties
     */
    override fun editProperties(response: AccountAuthenticatorResponse, accountType: String): Bundle {
        val bundle = Bundle()
        bundle.putString("test", "editProperties")
        return bundle
    }

    @Throws(NetworkErrorException::class)
    override fun addAccount(
        response: AccountAuthenticatorResponse,
        accountType: String,
        authTokenType: String?,
        requiredFeatures: Array<String>?,
        options: Bundle?
    ): Bundle {
        // account type not supported returns bundle without loginActivity Intent, it just contains "test" key 
        if (!supportedAccountType(accountType)) {
            val bundle = Bundle()
            bundle.putString("test", "addAccount")
            return bundle
        }
        return addAccount(response)
    }

    @Throws(NetworkErrorException::class)
    override fun confirmCredentials(response: AccountAuthenticatorResponse, account: Account, options: Bundle?): Bundle {
        val bundle = Bundle()
        bundle.putString("test", "confirmCredentials")
        return bundle
    }

    @Throws(NetworkErrorException::class)
    override fun getAuthToken(
        response: AccountAuthenticatorResponse,
        account: Account,
        authTokenType: String,
        options: Bundle?
    ): Bundle {
        val bundle = Bundle()
        bundle.putString("test", "getAuthToken")
        return bundle
    }

    override fun getAuthTokenLabel(authTokenType: String): String? {
        return if (supportedAccountType(authTokenType)) AUTH_TOKEN_TYPE else null
    }

    @Throws(NetworkErrorException::class)
    override fun updateCredentials(
        response: AccountAuthenticatorResponse,
        account: Account,
        authTokenType: String?,
        options: Bundle?
    ): Bundle? {
        val bundle = Bundle()
        bundle.putString("test", "updateCredentials")
        return bundle
    }

    @Throws(NetworkErrorException::class)
    override fun hasFeatures(response: AccountAuthenticatorResponse, account: Account, features: Array<String>): Bundle? {
        val bundle = Bundle()
        bundle.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false)
        return bundle
    }

    private fun supportedAccountType(type: String?): Boolean {
        return BuildConfig.ACCOUNT_TYPE == type
    }

    /**
     * Provides a bundle containing a Parcel
     * the Parcel packs an Intent with LoginActivity and Authenticator response (requires valid account type)
     */
    private fun addAccount(response: AccountAuthenticatorResponse): Bundle {
        val intent = Intent(context, LoginActivity::class.java)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)

        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)

        return bundle
    }

    @Throws(NetworkErrorException::class)
    override fun getAccountRemovalAllowed(response: AccountAuthenticatorResponse, account: Account): Bundle {
        val result = super.getAccountRemovalAllowed(response, account)

        if (result.containsKey(AccountManager.KEY_BOOLEAN_RESULT)
            && !result.containsKey(AccountManager.KEY_INTENT)) {
            val allowed = result.getBoolean(AccountManager.KEY_BOOLEAN_RESULT)

            if (allowed) {
                for (auth in SYNC_AUTHORITIES) {
                    ContentResolver.cancelSync(account, auth)
                }
            }
        }

        return result
    }
}