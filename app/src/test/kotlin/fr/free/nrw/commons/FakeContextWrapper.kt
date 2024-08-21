package fr.free.nrw.commons

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.ContextWrapper
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class FakeContextWrapper(base: Context?) : ContextWrapper(base) {

    @Mock
    private lateinit var mMockAccountManager: AccountManager

    override fun getSystemService(name: String): Any {
        return if (ACCOUNT_SERVICE == name) {
            mMockAccountManager
        } else super.getSystemService(name)
    }

    companion object {
        private val ACCOUNT = Account("test@example.com", BuildConfig.ACCOUNT_TYPE)
        private val ACCOUNTS = arrayOf(ACCOUNT)
    }

    init {
        MockitoAnnotations.openMocks(this)
        Mockito.`when`(mMockAccountManager.accounts).thenReturn(ACCOUNTS)
        Mockito.`when`(mMockAccountManager.getAccountsByType(BuildConfig.ACCOUNT_TYPE))
            .thenReturn(ACCOUNTS)
    }
}