package fr.free.nrw.commons.auth.login

import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import retrofit2.Call

class LoginClientTest {
    @Mock
    private lateinit var loginInterface: LoginInterface

    @Mock
    private lateinit var mockCall: Call<MwQueryResponse?>

    @Mock
    private lateinit var callback: LoginCallback

    private lateinit var loginClient: LoginClient

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(loginInterface.getLoginToken()).thenReturn(mockCall)
        loginClient = LoginClient(loginInterface)
    }

    @Test
    fun testLoginRequestDataClass() {
        val request =
            LoginRequest(
                username = "testUser",
                password = "testPassword",
                twoFactorCode = "123456",
                userLanguage = "en",
            )
        assertEquals("testUser", request.username)
        assertEquals("testPassword", request.password)
        assertEquals("123456", request.twoFactorCode)
        assertEquals("en", request.userLanguage)
    }

    @Test
    fun testRequestTriggersGetLoginToken() {
        loginClient.request("username", "password", callback)
        verify(loginInterface).getLoginToken()
    }

    @Test
    fun testCancelCancelsTokenCall() {
        loginClient.request("username", "password", callback)
        loginClient.cancel()
        verify(mockCall).cancel()
    }
}
