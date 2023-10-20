package fr.free.nrw.commons.wikidata

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.wikipedia.csrf.CsrfTokenClient

class WikiBaseClientUnitTest {

    @Mock
    internal var csrfTokenClient: CsrfTokenClient? = null

    @InjectMocks
    var wikiBaseClient: WikiBaseClient? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Mockito.`when`(csrfTokenClient!!.tokenBlocking)
            .thenReturn("test")
    }

    @Test
    fun testPostEditEntityByFilename() {
        wikiBaseClient?.postEditEntityByFilename("File:Example.jpg", "data")
    }
}