package fr.free.nrw.commons.auth.csrf

import com.google.gson.stream.MalformedJsonException
import fr.free.nrw.commons.MockWebServerTest
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.auth.login.LoginClient
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isA
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import fr.free.nrw.commons.wikidata.mwapi.MwException
import fr.free.nrw.commons.OkHttpConnectionFactory.HttpStatusException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Ignore

@OptIn(ExperimentalCoroutinesApi::class)
class CsrfTokenClientTest : MockWebServerTest() {
    private val cb = mock(CsrfTokenClient.Callback::class.java)
    private val sessionManager = mock(SessionManager::class.java)
    private val tokenInterface = mock(CsrfTokenInterface::class.java)
    private val loginClient = mock(LoginClient::class.java)
    private val logoutClient = mock(LogoutClient::class.java)
    private val subject = CsrfTokenClient(sessionManager, tokenInterface, loginClient, logoutClient, UnconfinedTestDispatcher())

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() = runTest {
        val expected = "b6f7bd58c013ab30735cb19ecc0aa08258122cba+\\"
        enqueueFromFile("csrf_token.json")

        performRequest()

        verify(cb).success(eq(expected))
        verify(cb, never()).failure(any(Throwable::class.java))
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseApiError() = runTest {
        enqueueFromFile("api_error.json")

        performRequest()

        verify(cb, never()).success(any(String::class.java))
        verify(cb).failure(isA(MwException::class.java))
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseFailure() = runTest {
        enqueue404()

        performRequest()

        verify(cb, never()).success(any(String::class.java))
        verify(cb).failure(isA(HttpStatusException::class.java))
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseMalformed() = runTest {
        enqueueMalformed()

        performRequest()

        verify(cb, never()).success(any(String::class.java))
        verify(cb).failure(isA(MalformedJsonException::class.java))
    }

    private fun performRequest() {
        subject.request(service(CsrfTokenInterface::class.java), cb)
        server().takeRequest()
    }
}
