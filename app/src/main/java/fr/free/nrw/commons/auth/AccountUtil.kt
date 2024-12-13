package fr.free.nrw.commons.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import androidx.annotation.VisibleForTesting
import fr.free.nrw.commons.BuildConfig.ACCOUNT_TYPE
import timber.log.Timber

const val AUTH_TOKEN_TYPE: String = "CommonsAndroid"

fun getUserName(context: Context): String? {
    return account(context)?.name
}

@VisibleForTesting
fun account(context: Context): Account? = try {
    val accountManager = AccountManager.get(context)
    val accounts = accountManager.getAccountsByType(ACCOUNT_TYPE)
    if (accounts.isNotEmpty()) accounts[0] else null
} catch (e: SecurityException) {
    Timber.e(e)
    null
}
