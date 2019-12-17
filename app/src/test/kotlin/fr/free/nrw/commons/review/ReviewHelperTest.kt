package fr.free.nrw.commons.review

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.Observable
import io.reactivex.Single
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.dataclient.mwapi.RecentChange

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
        MockitoAnnotations.initMocks(this)

        val mwQueryPage = mock(MwQueryPage::class.java)
        val mockRevision = mock(MwQueryPage.Revision::class.java)
        `when`(mockRevision.user).thenReturn("TestUser")
        `when`(mwQueryPage.revisions()).thenReturn(listOf(mockRevision))

        val recentChange = getMockRecentChange("log", "File:Test1.jpeg", 0)
        val recentChange1 = getMockRecentChange("log", "File:Test2.png", 0)
        val recentChange2 = getMockRecentChange("log", "File:Test3.jpg", 0)
        val mwQueryResult = mock(MwQueryResult::class.java)
        `when`(mwQueryResult.recentChanges).thenReturn(listOf(recentChange, recentChange1, recentChange2))
        `when`(mwQueryResult.firstPage()).thenReturn(mwQueryPage)
        `when`(mwQueryResult.pages()).thenReturn(listOf(mwQueryPage))
        val mockResponse = mock(MwQueryResponse::class.java)
        `when`(mockResponse.query()).thenReturn(mwQueryResult)
        `when`(reviewInterface?.getRecentChanges(ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(mockResponse))

        `when`(reviewInterface?.getFirstRevisionOfFile(ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(mockResponse))

        val media = mock(Media::class.java)
        `when`(media.filename).thenReturn("File:Test.jpg")
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

        val randomMedia = reviewHelper?.randomMedia?.blockingGet()

        assertNotNull(randomMedia)
        assertTrue(randomMedia is Media)
        verify(reviewInterface, times(1))!!.getRecentChanges(ArgumentMatchers.anyString())
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
        verify(reviewInterface, times(1))!!.getRecentChanges(ArgumentMatchers.anyString())
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

        val media = reviewHelper?.randomMedia?.blockingGet()

        assertNotNull(media)
        assertTrue(media is Media)
        verify(reviewInterface, times(1))!!.getRecentChanges(ArgumentMatchers.anyString())
    }

    private fun getMockRecentChange(type: String, title: String, oldRevisionId: Long): RecentChange {
        val recentChange = mock(RecentChange::class.java)
        `when`(recentChange!!.type).thenReturn(type)
        `when`(recentChange.title).thenReturn(title)
        `when`(recentChange.oldRevisionId).thenReturn(oldRevisionId)
        return recentChange
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