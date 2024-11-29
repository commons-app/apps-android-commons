package fr.free.nrw.commons.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.Build
import android.text.TextUtils
import fr.free.nrw.commons.BuildConfig.ACCOUNT_TYPE
import fr.free.nrw.commons.auth.login.LoginResult
import fr.free.nrw.commons.kvstore.JsonKvStore
import io.reactivex.Completable
import io.reactivex.Observable
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
    private val accountManager: AccountManager get() = AccountManager.get(context)

    private var _currentAccount: Account? = null // Unlike a savings account...  ;-)
    val currentAccount: Account? get() {
        if (_currentAccount == null) {
            val allAccounts = AccountManager.get(context).getAccountsByType(ACCOUNT_TYPE)
            if (allAccounts.isNotEmpty()) {
                _currentAccount = allAccounts[0]
            }
        }
        return _currentAccount
    }

    val userName: String?
        get() = currentAccount?.name

    var password: String?
        get() = currentAccount?.let { accountManager.getPassword(it) }
        private set(value) {
            currentAccount?.let { accountManager.setPassword(it, value) }
        }

    val isUserLoggedIn: Boolean
        get() = defaultKvStore.getBoolean("isUserLoggedIn", false)

    fun updateAccount(result: LoginResult) {
        if (createAccount(result.userName!!, result.password!!)) {
            password = result.password
        }
    }

    fun doesAccountExist(): Boolean =
        currentAccount != null

    fun setUserLoggedIn(isLoggedIn: Boolean) =
        defaultKvStore.putBoolean("isUserLoggedIn", isLoggedIn)

    fun forceLogin(context: Context?) =
        context?.let { LoginActivity.startYourself(it) }

    fun getPreference(key: String): Boolean =
        defaultKvStore.getBoolean(key)

    fun logout(): Completable = Completable.fromObservable(
        Observable.empty<Any>()
            .doOnComplete {
                removeAccount()
                _currentAccount = null
            }
    )

    private fun createAccount(userName: String, password: String): Boolean {
        var account = currentAccount
        if (account == null || TextUtils.isEmpty(account.name) || account.name != userName) {
            removeAccount()
            account = Account(userName, ACCOUNT_TYPE)
            return accountManager.addAccountExplicitly(account, password, null)
        }
        return true
    }

    private fun removeAccount() {
        currentAccount?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                accountManager.removeAccountExplicitly(it)
            } else {
                accountManager.removeAccount(it, null, null)
            }
        }
    }
}
