package fr.free.nrw.commons.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import fr.free.nrw.commons.BuildConfig
import timber.log.Timber

object AccountUtil {
    const val AUTH_TOKEN_TYPE = "CommonsAndroid"

    fun account(context: Context): Account? {
        try {
            val accounts = AccountManager.get(context).getAccountsByType(BuildConfig.ACCOUNT_TYPE)
            if (accounts.isNotEmpty()) {
                return accounts[0]
            }
        } catch (e: SecurityException) {
            Timber.e(e)
        }
        return null
    }

    @JvmStatic
    fun getUserName(context: Context): String? {
        val account = account(context)
        return account?.name
    }
}