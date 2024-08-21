package fr.free.nrw.commons

import android.accounts.AccountManager
import android.content.Context
import android.content.ContextWrapper
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class FakeContextWrapperWithException(base: Context?) : ContextWrapper(base) {

    @Mock
    private lateinit var mMockAccountManager: AccountManager

    override fun getSystemService(name: String): Any {
        return if (ACCOUNT_SERVICE == name) {
            mMockAccountManager
        } else super.getSystemService(name)
    }

    init {
        MockitoAnnotations.openMocks(this)
        Mockito.`when`(mMockAccountManager.getAccountsByType(BuildConfig.ACCOUNT_TYPE))
            .thenThrow(SecurityException("Permission Denied"))
    }
}