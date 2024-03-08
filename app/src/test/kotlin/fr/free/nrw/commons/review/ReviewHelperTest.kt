package fr.free.nrw.commons.review

import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResult
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.CoreMatchers.`is`

/**
 * Test class for ReviewHelper
 */
class ReviewHelperTest {

    private val reviewInterface = mock<ReviewInterface>()
    private val mediaClient = mock<MediaClient>()
    private val reviewHelper = ReviewHelper(mediaClient, reviewInterface)


    private val mwQueryResult = mock<MwQueryResult>()
    private val mockResponse = mock<MwQueryResponse>()

    /**
     * Init mocks
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        whenever(mockResponse.query()).thenReturn(mwQueryResult)
        whenever(reviewInterface.getRecentChanges()).thenReturn(Observable.just(mockResponse))
    }

    /**
     * Test for getting random media
     */
    @Test
    fun getRandomMedia() {
        whenever(mediaClient.checkPageExistsUsingTitle(any())).thenReturn(Single.just(false))

        val page1 = setupMedia("one.jpg")
        val page2 = setupMedia("two.jpeg")
        val page3 = setupMedia("three.png")
        val ignored = setupMedia("ignored.txt")
        whenever(mwQueryResult.pages()).thenReturn(listOf(page1, page2, page3, ignored))

        val random = reviewHelper.getRandomMedia().test()

        random.assertNoErrors()
        assertThat(1, equalTo( random.valueCount()))
        assertThat(setOf("one.jpg", "two.jpeg", "three.png").contains(random.values().first().filename), `is`(true))
    }

    /**
     * Test scenario when all media is already nominated for deletion
     */
    @Test(expected = RuntimeException::class)
    fun getRandomMediaWithWithAllMediaNominatedForDeletion() {
        whenever(mediaClient.checkPageExistsUsingTitle(any())).thenReturn(Single.just(true))

        val page1 = setupMedia("one.jpg")
        whenever(mwQueryResult.pages()).thenReturn(listOf(page1))

        val media = reviewHelper.getRandomMedia().blockingGet()
        assertThat(media, nullValue())
        verify(reviewInterface, times(1))!!.getRecentChanges()
    }

    /**
     * Test scenario when first media is already nominated for deletion
     */
    @Test
    fun getRandomMediaWithWithOneMediaNominatedForDeletion() {
        whenever(mediaClient.checkPageExistsUsingTitle("Commons:Deletion_requests/one.jpg")).thenReturn(Single.just(true))

        val page1 = setupMedia("one.jpg")
        whenever(mwQueryResult.pages()).thenReturn(listOf(page1))

        val random = reviewHelper.getRandomMedia().test()

        assertThat("one.jpg is deleted", equalTo( random.errors().first().message))
    }

    /**
     * Test for getting first revision of file
     */
    @Test
    fun getFirstRevisionOfFile() {
        val rev1 = mock<MwQueryPage.Revision>()
        whenever(rev1.user).thenReturn("TestUser")
        whenever(rev1.revisionId).thenReturn(1L)
        val rev2 = mock<MwQueryPage.Revision>()
        whenever(rev2.user).thenReturn("TestUser")
        whenever(rev2.revisionId).thenReturn(2L)

        val page = setupMedia("Test.jpg", rev1, rev2)
        whenever(mwQueryResult.firstPage()).thenReturn(page)
        whenever(reviewInterface.getFirstRevisionOfFile(any())).thenReturn(Observable.just(mockResponse))

        val firstRevisionOfFile = reviewHelper.getFirstRevisionOfFile("Test.jpg").blockingFirst()

        assertEquals(1, firstRevisionOfFile.revisionId)
    }

    @Test
    fun checkFileUsage() {
        whenever(reviewInterface.getGlobalUsageInfo(any())).thenReturn(Observable.just(mockResponse))
        val page = setupMedia("Test.jpg")
        whenever(mwQueryResult.firstPage()).thenReturn(page)
        whenever(page.checkWhetherFileIsUsedInWikis()).thenReturn(true)

        val result = reviewHelper.checkFileUsage("Test.jpg").test()

        assertThat(result.values().first(), `is`(true))
    }

    @Test
    fun testReviewStatus() {
        val reviewDao = mock<ReviewDao>()
        whenever(reviewDao.isReviewedAlready("Test.jpg")).thenReturn(true)

        reviewHelper.dao = reviewDao
        val result = reviewHelper.getReviewStatus("Test.jpg")

        assertThat(result, `is`(true))
    }

    private fun setupMedia(file: String, vararg revision: MwQueryPage.Revision): MwQueryPage = mock<MwQueryPage>().apply {
        whenever(title()).thenReturn(file)
        if (revision.isNotEmpty()) {
            whenever(revisions()).thenReturn(*revision.toMutableList())
        }

        val media = mock<Media>().apply {
            whenever(filename).thenReturn(file)
            whenever(pageId).thenReturn(file.split(".").first())
        }
        whenever(mediaClient.getMedia(file)).thenReturn(Single.just(media))
    }
}
