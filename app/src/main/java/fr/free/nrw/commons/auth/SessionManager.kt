package fr.free.nrw.commons.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.Build
import android.text.TextUtils
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.kvstore.JsonKvStore
import io.reactivex.Completable
import io.reactivex.Observable
import org.wikipedia.login.LoginResult
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Manage the current logged in user session.
 */
@Singleton
class SessionManager @Inject constructor(
    private val context: Context,
    @param:Named("default_preferences") private val defaultKvStore: JsonKvStore
) {

    private fun createAccount(userName: String, password: String): Boolean {
        var account = currentAccount
        if (account == null || TextUtils.isEmpty(account.name) || account.name != userName) {
            removeAccount()
            account = Account(userName, BuildConfig.ACCOUNT_TYPE)
            return accountManager().addAccountExplicitly(account, password, null)
        }
        return true
    }

    private fun removeAccount() {
        val account = currentAccount
        if (account != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                accountManager().removeAccountExplicitly(account)
            } else {
                accountManager().removeAccount(account, null, null)
            }
        }
    }

    fun updateAccount(result: LoginResult) {
        val accountCreated = createAccount(result.userName!!, result.password!!)
        if (accountCreated) setPassword(result.password!!)
    }

    private fun setPassword(password: String) {
        val account = currentAccount
        if (account != null) accountManager().setPassword(account, password)
    }

    var currentAccount: Account? = null // Unlike a savings account...  ;-)
        get() {
            if (field == null) {
                val accountManager = AccountManager.get(context)
                val allAccounts = accountManager.getAccountsByType(BuildConfig.ACCOUNT_TYPE)
                if (allAccounts.isNotEmpty()) {
                    currentAccount = allAccounts[0]
                }
            }
            return field
        }

    fun doesAccountExist(): Boolean {
        return currentAccount != null
    }

    val userName: String?
        get() {
            val account = currentAccount
            return account?.name
        }

    val password: String?
        get() {
            val account = currentAccount
            return if (account == null) null else accountManager().getPassword(account)
        }

    private fun accountManager(): AccountManager {
        return AccountManager.get(context)
    }

    var isUserLoggedIn: Boolean
        get() = defaultKvStore.getBoolean("isUserLoggedIn", false)
        set(isLoggedIn) {
            defaultKvStore.putBoolean("isUserLoggedIn", isLoggedIn)
        }

    fun forceLogin(context: Context?) {
        if (context != null) {
            LoginActivity.startYourself(context)
        }
    }

    /**
     * 1. Clears existing accounts from account manager
     * 2. Calls MediaWikiApi's logout function to clear cookies
     * @return
     */
    fun logout(): Completable {
        val accountManager = AccountManager.get(context)
        val allAccounts = accountManager.getAccountsByType(BuildConfig.ACCOUNT_TYPE)
        return Completable.fromObservable(Observable.fromArray(*allAccounts)
            .map { account -> accountManager.removeAccount(account, null, null).result })
            .doOnComplete { currentAccount = null }
    }

}