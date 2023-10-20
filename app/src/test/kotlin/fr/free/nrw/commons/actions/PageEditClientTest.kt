package fr.free.nrw.commons.actions

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.Service
import org.wikipedia.edit.Edit

class PageEditClientTest {
    @Mock
    private lateinit var csrfTokenClient: CsrfTokenClient
    @Mock
    private lateinit var pageEditInterface: PageEditInterface

    private lateinit var pageEditClient: PageEditClient

    @Mock
    lateinit var edit: Edit

    @Mock
    lateinit var editResult: Edit.Result

    /**
     * initial setup, test environment
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        pageEditClient = PageEditClient(csrfTokenClient, pageEditInterface)
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
        Mockito.`when`(
            pageEditInterface.postAppendEdit(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(
            Observable.just(edit)
        )
        Mockito.`when`(edit.edit()).thenReturn(editResult)
        Mockito.`when`(editResult.editSucceeded()).thenReturn(true)
        pageEditClient.appendEdit("test", "test", "test").test()
        verify(csrfTokenClient).tokenBlocking
        verify(pageEditInterface).postAppendEdit(eq("test"), eq("test"), eq("test"), eq("test"))
        verify(edit).edit()
        verify(editResult).editSucceeded()
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

    /**
     * Test setCaptions
     */
    @Test
    fun testSetCaptions() {
        Mockito.`when`(csrfTokenClient.tokenBlocking).thenReturn("test")
        pageEditClient.setCaptions("test", "test", "en", "test")
        verify(pageEditInterface).postCaptions(eq("test"), eq("test"), eq("en"),
            eq("test"), eq("test"))
    }
}