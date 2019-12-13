package fr.free.nrw.commons.actions

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.Service

class PageEditClientTest {
    @Mock
    private lateinit var csrfTokenClient: CsrfTokenClient
    @Mock
    private lateinit var pageEditInterface: PageEditInterface
    @Mock
    private lateinit var service: Service

    private lateinit var pageEditClient: PageEditClient

    /**
     * initial setup, test environment
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        pageEditClient = PageEditClient(csrfTokenClient, pageEditInterface, service)
    }

    /**
     * Test page edit
     */
    @Test
    fun testEdit() {
        Mockito.`when`(csrfTokenClient.tokenBlocking).thenReturn("test")
        pageEditClient.edit("test", "test", "test")
        verify(pageEditInterface).postEdit(eq("test"), eq("test"), eq("test"), eq("test"))
    }

    /**
     * Test appendEdit
     */
    @Test
    fun testAppendEdit() {
        Mockito.`when`(csrfTokenClient.tokenBlocking).thenReturn("test")
        pageEditClient.appendEdit("test", "test", "test")
        verify(pageEditInterface).postAppendEdit(eq("test"), eq("test"), eq("test"), eq("test"))
    }

    /**
     * Test prependEdit
     */
    @Test
    fun testPrependEdit() {
        Mockito.`when`(csrfTokenClient.tokenBlocking).thenReturn("test")
        pageEditClient.prependEdit("test", "test", "test")
        verify(pageEditInterface).postPrependEdit(eq("test"), eq("test"), eq("test"), eq("test"))
    }
}