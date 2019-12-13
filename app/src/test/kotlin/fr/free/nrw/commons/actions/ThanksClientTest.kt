package fr.free.nrw.commons.actions

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.CommonsApplication
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.Service

@RunWith(PowerMockRunner::class)
@PrepareForTest(CommonsApplication::class)
class ThanksClientTest {
    @Mock
    private lateinit var csrfTokenClient: CsrfTokenClient
    @Mock
    private lateinit var service: Service

    @Mock
    private lateinit var commonsApplication: CommonsApplication

    private lateinit var thanksClient: ThanksClient

    /**
     * initial setup, test environment
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        PowerMockito.mockStatic(CommonsApplication::class.java)
        PowerMockito.`when`(CommonsApplication.getInstance()).thenReturn(commonsApplication)
        thanksClient = ThanksClient(csrfTokenClient, service)
    }

    /**
     * Test thanks
     */
    @Test
    fun testThanks() {
        Mockito.`when`(csrfTokenClient.tokenBlocking).thenReturn("test")
        Mockito.`when`(commonsApplication.userAgent).thenReturn("test")
        thanksClient.thank(1L)
        verify(service).thank(ArgumentMatchers.anyString(), ArgumentMatchers.any(), eq("test"), eq("test"))
    }
}