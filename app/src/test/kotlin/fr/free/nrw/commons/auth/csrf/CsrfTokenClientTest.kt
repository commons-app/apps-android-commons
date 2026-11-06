package fr.free.nrw.commons.auth.csrf

import com.google.gson.stream.MalformedJsonException
import fr.free.nrw.commons.MockWebServerTest
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.auth.login.LoginClient
import fr.free.nrw.commons.auth.login.LoginFailedException
import fr.free.nrw.commons.wikidata.mwapi.MwException
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isA
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.io.IOException

class CsrfTokenClientTest : MockWebServerTest() {
    private val cb = mock(CsrfTokenClient.Callback::class.java)
    private val sessionManager = mock(SessionManager::class.java)
    private val tokenInterface = mock(CsrfTokenInterface::class.java)
    private val loginClient = mock(LoginClient::class.java)
    private val logoutClient = mock(LogoutClient::class.java)
    private val subject = CsrfTokenClient(sessionManager, tokenInterface, loginClient, logoutClient)

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        val expected = "b6f7bd58c013ab30735cb19ecc0aa08258122cba+\\"
        enqueueFromFile("csrf_token.json")

        performRequest()

        verify(cb).success(eq(expected))
        verify(cb, never()).failure(any(Throwable::class.java))
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseApiError() {
        enqueueFromFile("api_error.json")

        performRequest()

        verify(cb, never()).success(any(String::class.java))
        verify(cb).failure(isA(MwException::class.java))
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseFailure() {
        enqueue404()

        performRequest()

        verify(cb, never()).success(any(String::class.java))
        verify(cb).failure(isA(IOException::class.java))
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseMalformed() {
        enqueueMalformed()

        performRequest()

        verify(cb, never()).success(any(String::class.java))
        verify(cb).failure(isA(MalformedJsonException::class.java))
    }

    @Test
    @Throws(Throwable::class)
    fun testGetTokenBlockingKeepsLoginFailureAsCause() {
        val loginFailure = LoginFailedException("Login failed")
        // The first attempt returns no token, so the client logs in explicitly, which fails.
        doThrow(loginFailure).`when`(loginClient).loginBlocking("", "")

        val thrown =
            assertThrows(InvalidLoginTokenException::class.java) {
                subject.getTokenBlocking()
            }

        assertSame(loginFailure, thrown.cause)
    }

    private fun performRequest() {
        subject.request(service(CsrfTokenInterface::class.java), cb)
        server().takeRequest()
    }
}
