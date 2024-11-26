package fr.free.nrw.commons.auth

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.accounts.NetworkErrorException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import fr.free.nrw.commons.BuildConfig

private val SYNC_AUTHORITIES = arrayOf(
    BuildConfig.CONTRIBUTION_AUTHORITY, BuildConfig.MODIFICATION_AUTHORITY
)

/**
 * Handles WikiMedia commons account Authentication
 */
class WikiAccountAuthenticator(
    private val context: Context
) : AbstractAccountAuthenticator(context) {
    /**
     * Provides Bundle with edited Account Properties
     */
    override fun editProperties(
        response: AccountAuthenticatorResponse,
        accountType: String
    ) = bundleOf("test" to "editProperties")

    // account type not supported returns bundle without loginActivity Intent, it just contains "test" key
    @Throws(NetworkErrorException::class)
    override fun addAccount(
        response: AccountAuthenticatorResponse,
        accountType: String,
        authTokenType: String?,
        requiredFeatures: Array<String>?,
        options: Bundle?
    ) = if (BuildConfig.ACCOUNT_TYPE == accountType) {
        addAccount(response)
    } else {
        bundleOf("test" to "addAccount")
    }

    @Throws(NetworkErrorException::class)
    override fun confirmCredentials(
        response: AccountAuthenticatorResponse, account: Account, options: Bundle?
    ) = bundleOf("test" to "confirmCredentials")

    @Throws(NetworkErrorException::class)
    override fun getAuthToken(
        response: AccountAuthenticatorResponse,
        account: Account,
        authTokenType: String,
        options: Bundle?
    ) = bundleOf("test" to "getAuthToken")

    override fun getAuthTokenLabel(authTokenType: String) =
        if (BuildConfig.ACCOUNT_TYPE == authTokenType) AccountUtil.AUTH_TOKEN_TYPE else null

    @Throws(NetworkErrorException::class)
    override fun updateCredentials(
        response: AccountAuthenticatorResponse,
        account: Account,
        authTokenType: String?,
        options: Bundle?
    ) = bundleOf("test" to "updateCredentials")

    @Throws(NetworkErrorException::class)
    override fun hasFeatures(
        response: AccountAuthenticatorResponse,
        account: Account, features: Array<String>
    ) = bundleOf(AccountManager.KEY_BOOLEAN_RESULT to false)

    /**
     * Provides a bundle containing a Parcel
     * the Parcel packs an Intent with LoginActivity and Authenticator response (requires valid account type)
     */
    private fun addAccount(response: AccountAuthenticatorResponse): Bundle {
        val intent = Intent(context, LoginActivity::class.java)
            .putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        return bundleOf(AccountManager.KEY_INTENT to intent)
    }

    @Throws(NetworkErrorException::class)
    override fun getAccountRemovalAllowed(
        response: AccountAuthenticatorResponse?,
        account: Account?
    ): Bundle {
        val result = super.getAccountRemovalAllowed(response, account)

        if (result.containsKey(AccountManager.KEY_BOOLEAN_RESULT)
            && !result.containsKey(AccountManager.KEY_INTENT)
        ) {
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
