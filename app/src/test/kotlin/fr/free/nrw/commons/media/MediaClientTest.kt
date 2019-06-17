package fr.free.nrw.commons.media

import io.reactivex.Observable
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.wikipedia.dataclient.mwapi.ImageDetails
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.MwQueryResult

class MediaClientTest {

    @Mock
    internal var mediaInterface: MediaInterface? = null

    @InjectMocks
    var mediaClient: MediaClient? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun checkPageExistsUsingTitle() {
        val mwQueryPage = mock(MwQueryPage::class.java)
        val mwQueryResult = mock(MwQueryResult::class.java)
        `when`(mwQueryResult.firstPage()).thenReturn(mwQueryPage)
        `when`(mwQueryResult.pages()).thenReturn(listOf(mwQueryPage))
        val mockResponse = mock(MwQueryResponse::class.java)
        `when`(mockResponse.query()).thenReturn(mwQueryResult)

        `when`(mediaInterface!!.checkPageExistsUsingTitle(ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(mockResponse))

        val checkPageExistsUsingTitle = mediaClient!!.checkPageExistsUsingTitle("File:Test.jpg").blockingGet()
        assertTrue(checkPageExistsUsingTitle)
    }

    @Test
    fun checkPageNotExistsUsingTitle() {
        val mwQueryResult = mock(MwQueryResult::class.java)
        `when`(mwQueryResult.firstPage()).thenReturn(null)
        `when`(mwQueryResult.pages()).thenReturn(listOf())
        val mockResponse = mock(MwQueryResponse::class.java)
        `when`(mockResponse.query()).thenReturn(mwQueryResult)

        `when`(mediaInterface!!.checkPageExistsUsingTitle(ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(mockResponse))

        val checkPageExistsUsingTitle = mediaClient!!.checkPageExistsUsingTitle("File:Test.jpg").blockingGet()
        assertFalse(checkPageExistsUsingTitle)
    }

    @Test
    fun checkFileExistsUsingSha() {
        val mwQueryPage = mock(MwQueryPage::class.java)
        val mwQueryResult = mock(MwQueryResult::class.java)
        `when`(mwQueryResult.allImages()).thenReturn(listOf(mock(ImageDetails::class.java)))
        `when`(mwQueryResult.firstPage()).thenReturn(mwQueryPage)
        `when`(mwQueryResult.pages()).thenReturn(listOf(mwQueryPage))
        val mockResponse = mock(MwQueryResponse::class.java)
        `when`(mockResponse.query()).thenReturn(mwQueryResult)

        `when`(mediaInterface!!.checkFileExistsUsingSha(ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(mockResponse))

        val checkFileExistsUsingSha = mediaClient!!.checkFileExistsUsingSha("abcde").blockingGet()
        assertTrue(checkFileExistsUsingSha)
    }

    @Test
    fun checkFileNotExistsUsingSha() {
        val mwQueryPage = mock(MwQueryPage::class.java)
        val mwQueryResult = mock(MwQueryResult::class.java)
        `when`(mwQueryResult.allImages()).thenReturn(listOf())
        `when`(mwQueryResult.firstPage()).thenReturn(mwQueryPage)
        `when`(mwQueryResult.pages()).thenReturn(listOf(mwQueryPage))
        val mockResponse = mock(MwQueryResponse::class.java)
        `when`(mockResponse.query()).thenReturn(mwQueryResult)

        `when`(mediaInterface!!.checkFileExistsUsingSha(ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(mockResponse))

        val checkFileExistsUsingSha = mediaClient!!.checkFileExistsUsingSha("abcde").blockingGet()
        assertFalse(checkFileExistsUsingSha)
    }
}