package fr.free.nrw.commons.review

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResult
import java.util.concurrent.Callable

/**
 * Test class for ReviewHelper
 */
class ReviewHelperTest {

    @Mock
    internal var reviewInterface: ReviewInterface? = null
    @Mock
    internal var mediaClient: MediaClient? = null

    @InjectMocks
    var reviewHelper: ReviewHelper? = null

    /**
     * Init mocks
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        val mwQueryPage = mock(MwQueryPage::class.java)
        val mockRevision = mock(MwQueryPage.Revision::class.java)
        `when`(mockRevision.user).thenReturn("TestUser")
        `when`(mwQueryPage.revisions()).thenReturn(listOf(mockRevision))

        val mwQueryResult = mock(MwQueryResult::class.java)
        `when`(mwQueryResult.firstPage()).thenReturn(mwQueryPage)
        `when`(mwQueryResult.pages()).thenReturn(listOf(mwQueryPage))
        val mockResponse = mock(MwQueryResponse::class.java)
        `when`(mockResponse.query()).thenReturn(mwQueryResult)
        `when`(reviewInterface?.getRecentChanges())
                .thenReturn(Observable.just(mockResponse))

        `when`(reviewInterface?.getFirstRevisionOfFile(ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(mockResponse))

        val media = mock(Media::class.java)
        whenever(media.filename).thenReturn("Test file.jpg")
        `when`(mediaClient?.getMedia(ArgumentMatchers.anyString()))
                .thenReturn(Single.just(media))
    }

    /**
     * Test for getting random media
     */
    @Test
    fun getRandomMedia() {
        `when`(mediaClient?.checkPageExistsUsingTitle(ArgumentMatchers.anyString()))
                .thenReturn(Single.just(false))

        `when`(mediaClient?.checkPageExistsUsingTitle(ArgumentMatchers.anyString()))
                .thenReturn(Single.just(false))

        reviewHelper?.randomMedia
        verify(reviewInterface, times(1))!!.getRecentChanges()
    }

    /**
     * Test scenario when all media is already nominated for deletion
     */
    @Test(expected = RuntimeException::class)
    fun getRandomMediaWithWithAllMediaNominatedForDeletion() {
        `when`(mediaClient?.checkPageExistsUsingTitle(ArgumentMatchers.anyString()))
                .thenReturn(Single.just(true))
        val media = reviewHelper?.randomMedia?.blockingGet()
        assertNull(media)
        verify(reviewInterface, times(1))!!.getRecentChanges()
    }

    /**
     * Test scenario when first media is already nominated for deletion
     */
    @Test
    fun getRandomMediaWithWithOneMediaNominatedForDeletion() {
        `when`(mediaClient?.checkPageExistsUsingTitle("Commons:Deletion_requests/File:Test1.jpeg"))
                .thenReturn(Single.just(true))
        `when`(mediaClient?.checkPageExistsUsingTitle("Commons:Deletion_requests/File:Test2.png"))
                .thenReturn(Single.just(false))
        `when`(mediaClient?.checkPageExistsUsingTitle("Commons:Deletion_requests/File:Test3.jpg"))
                .thenReturn(Single.just(true))

        reviewHelper?.randomMedia
        verify(reviewInterface, times(1))!!.getRecentChanges()
    }

    /**
     * Test for getting first revision of file
     */
    @Test
    fun getFirstRevisionOfFile() {
        val firstRevisionOfFile = reviewHelper?.getFirstRevisionOfFile("Test.jpg")?.blockingFirst()

        assertTrue(firstRevisionOfFile is MwQueryPage.Revision)
    }

}
