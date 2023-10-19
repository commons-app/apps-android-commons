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
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.MwQueryResult
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

    val dao = mock(ReviewDao::class.java)

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

    /**
     * Test the review status of the image
     *  Case 1: Image identifier exists in the database
     */
    @Test
    fun getReviewStatusWhenImageHasBeenReviewedAlready() {
        val testImageId1 = "123456"
        `when`(dao.isReviewedAlready(testImageId1)).thenReturn(true)

        val observer = io.reactivex.observers.TestObserver<Boolean>()
        Observable.fromCallable(Callable<Boolean> {
            dao.isReviewedAlready(testImageId1)
        }).subscribeWith(observer)
        observer.assertValue(true)
        observer.dispose()
    }

    /**
     * Test the review status of the image
     *  Case 2: Image identifier does not exist in the database
     */
    @Test
    fun getReviewStatusWhenImageHasBeenNotReviewedAlready() {
        val testImageId2 = "789101"
        `when`(dao.isReviewedAlready(testImageId2)).thenReturn(false)

        val observer = io.reactivex.observers.TestObserver<Boolean>()
        Observable.fromCallable(Callable<Boolean> {
            dao.isReviewedAlready(testImageId2)
        }).subscribeWith(observer)
        observer.assertValue(false)
        observer.dispose()
    }

    /**
     * Test the successful insertion of the image identifier into the database
     */
    @Test
    fun addViewedImagesToDB() {
        val testImageId = "123456"

        val observer = io.reactivex.observers.TestObserver<Boolean>()
        Completable.fromAction {
            dao.insert(ReviewEntity(testImageId))
        }.subscribeWith(observer)
        observer.assertComplete()
        observer.dispose()
    }
}
