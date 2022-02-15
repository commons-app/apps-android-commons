package fr.free.nrw.commons

import android.content.Context
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.kvstore.JsonKvStore
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.dataclient.SharedPreferenceCookieManager
import org.wikipedia.json.GsonMarshaller
import org.wikipedia.login.LoginResult

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class CommonsAppAdapterUnitTest {

    private lateinit var adapter: CommonsAppAdapter
    private lateinit var context: Context

    @Mock
    private lateinit var sessionManager: SessionManager

    @Mock
    private lateinit var preferences: JsonKvStore

    @Mock
    private lateinit var result: LoginResult

    @Mock
    private lateinit var cookies: SharedPreferenceCookieManager

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        adapter = CommonsAppAdapter(sessionManager, preferences)
        context = RuntimeEnvironment.application.applicationContext
    }

    @Test
    @Throws(Exception::class)
    fun checkAdapterNotNull() {
        Assert.assertNotNull(adapter)
    }

    @Test
    @Throws(Exception::class)
    fun testGetMediaWikiBaseUrl() {
        Assert.assertEquals(adapter.mediaWikiBaseUrl, BuildConfig.COMMONS_URL)
    }

    @Test
    @Throws(Exception::class)
    fun testGetRestbaseUriFormat() {
        Assert.assertEquals(adapter.restbaseUriFormat, BuildConfig.COMMONS_URL)
    }

    @Test
    @Throws(Exception::class)
    fun testGetDesiredLeadImageDp() {
        Assert.assertEquals(adapter.desiredLeadImageDp, 640)
    }

    @Test
    @Throws(Exception::class)
    fun testIsLoggedIn() {
        whenever(sessionManager.isUserLoggedIn).thenReturn(true)
        Assert.assertEquals(adapter.isLoggedIn, true)
    }

    @Test
    @Throws(Exception::class)
    fun testGetUserName() {
        whenever(sessionManager.userName).thenReturn("test")
        Assert.assertEquals(adapter.userName, "test")
    }

    @Test
    @Throws(Exception::class)
    fun testGetPassword() {
        whenever(sessionManager.password).thenReturn("test")
        Assert.assertEquals(adapter.password, "test")
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateAccount() {
        adapter.updateAccount(result)
        verify(sessionManager).updateAccount(result)
    }

    @Test
    @Throws(Exception::class)
    fun testSetCookies() {
        adapter.cookies = cookies
        verify(preferences).putString("cookie_store", GsonMarshaller.marshal(cookies))
    }

    @Test
    @Throws(Exception::class)
    fun testLogErrorsInsteadOfCrashing() {
        Assert.assertEquals(adapter.logErrorsInsteadOfCrashing(), false)
    }

    @Test
    @Throws(Exception::class)
    fun testGetCookiesCaseNull() {
        whenever(preferences.contains("cookie_store")).thenReturn(false)
        Assert.assertEquals(adapter.cookies, null)
    }

}