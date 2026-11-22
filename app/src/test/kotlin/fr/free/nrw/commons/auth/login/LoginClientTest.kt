package fr.free.nrw.commons.auth.login

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.argumentCaptor
import fr.free.nrw.commons.wikidata.WikidataConstants.WIKIPEDIA_URL
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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

    @Test
    fun testApiErrorResponseGivesLoginFailedException() {
        val loginCall = mock(Call::class.java) as Call<LoginResponse?>
        `when`(
            loginInterface.postLogIn("user", "pw", "token", "en", WIKIPEDIA_URL),
        ).thenReturn(loginCall)
        val body =
            Gson().fromJson(
                """{"errors":[{"code":"badtoken","text":"Invalid CSRF token."}]}""",
                LoginResponse::class.java,
            )

        loginClient.login(
            LoginRequest("user", "pw", loginToken = "token", userLanguage = "en"),
            callback,
        )

        val callbackCaptor = argumentCaptor<Callback<LoginResponse?>>()
        verify(loginCall).enqueue(callbackCaptor.capture())
        callbackCaptor.firstValue.onResponse(loginCall, Response.success(body))

        val errorCaptor = argumentCaptor<Throwable>()
        verify(callback).error(errorCaptor.capture())
        assertTrue(errorCaptor.firstValue is LoginFailedException)
        assertEquals("Invalid CSRF token.", errorCaptor.firstValue.message)
    }
}
