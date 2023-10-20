package fr.free.nrw.commons.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.kvstore.JsonKvStore
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.api.mockito.PowerMockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.login.LoginResult
import java.lang.reflect.Method


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class SessionManagerUnitTests {

    private lateinit var sessionManager: SessionManager
    private lateinit var accountManager: AccountManager

    @Mock
    private lateinit var account: Account

    @Mock
    private lateinit var defaultKvStore: JsonKvStore

    @Mock
    private lateinit var loginResult: LoginResult

    @Mock
    private lateinit var context: Context

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        accountManager = AccountManager.get(ApplicationProvider.getApplicationContext())
        shadowOf(accountManager).addAccount(account)
        sessionManager =
            SessionManager(ApplicationProvider.getApplicationContext(), defaultKvStore)
    }

    @Test
    @Throws(Exception::class)
    fun checkNotNull() {
        Assert.assertNotNull(sessionManager)
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveAccountCaseNull() {
        val method: Method = SessionManager::class.java.getDeclaredMethod(
            "removeAccount"
        )
        method.isAccessible = true
        method.invoke(sessionManager)
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateAccount() {
        `when`(loginResult.userName).thenReturn("username")
        `when`(loginResult.password).thenReturn("password")
        val method: Method = SessionManager::class.java.getDeclaredMethod(
            "updateAccount",
            LoginResult::class.java
        )
        method.isAccessible = true
        method.invoke(sessionManager, loginResult)
    }

    @Test
    @Throws(Exception::class)
    fun testLogout() {
        sessionManager.logout()
    }

    @Test
    @Throws(Exception::class)
    fun testDoesAccountExist() {
        sessionManager.doesAccountExist()
    }

    @Test
    @Throws(Exception::class)
    fun testIsUserLoggedIn() {
        Assert.assertEquals(sessionManager.isUserLoggedIn, false)
    }

    @Test
    @Throws(Exception::class)
    fun testGetPreference() {
        Assert.assertEquals(sessionManager.getPreference("key"), false)
    }

    @Test
    @Throws(Exception::class)
    fun testForceLoginCaseNull() {
        sessionManager.forceLogin(null)
    }

    @Test
    @Throws(Exception::class)
    fun testForceLogin() {
        sessionManager.forceLogin(context)
    }

    @Test
    @Throws(Exception::class)
    fun testCreateAccount() {
        val method: Method = SessionManager::class.java.getDeclaredMethod(
            "createAccount",
            String::class.java,
            String::class.java
        )
        method.isAccessible = true
        Assert.assertEquals(method.invoke(sessionManager, "username", "password"), true)
    }

    @Test
    @Throws(Exception::class)
    fun testSetUserLoggedIn() {
        val method: Method = SessionManager::class.java.getDeclaredMethod(
            "setUserLoggedIn",
            Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(sessionManager, true)
    }

    @Test
    @Throws(Exception::class)
    fun testGetUserName() {
        val method: Method = SessionManager::class.java.getDeclaredMethod(
            "getUserName"
        )
        method.isAccessible = true
        Assert.assertEquals(method.invoke(sessionManager), null)
    }

    @Test
    @Throws(Exception::class)
    fun testGetPassword() {
        val method: Method = SessionManager::class.java.getDeclaredMethod(
            "getPassword"
        )
        method.isAccessible = true
        Assert.assertEquals(method.invoke(sessionManager), null)
    }

}