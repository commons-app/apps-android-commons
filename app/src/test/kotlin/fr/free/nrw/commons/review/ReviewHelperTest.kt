package fr.free.nrw.commons.review

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.mwapi.MediaWikiApi
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import io.reactivex.Single
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.RecentChange

/**
 * Test class for ReviewHelper
 */
class ReviewHelperTest {

    @Mock
    internal var okHttpJsonApiClient: OkHttpJsonApiClient? = null
    @Mock
    internal var mediaWikiApi: MediaWikiApi? = null

    @InjectMocks
    var reviewHelper: ReviewHelper? = null

    /**
     * Init mocks
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    /**
     * Test for getting random media
     */
    @Test
    fun getRandomMedia() {
        `when`(okHttpJsonApiClient?.recentFileChanges)
                .thenReturn(Single.just(listOf(getMockRecentChange("test", "File:Test1.jpeg", 0),
                        getMockRecentChange("test", "File:Test2.png", 0),
                        getMockRecentChange("test", "File:Test3.jpg", 0))))

        `when`(mediaWikiApi?.pageExists(ArgumentMatchers.anyString()))
                .thenReturn(Single.just(true))
        val randomMedia = reviewHelper?.randomMedia?.blockingGet()

        assertTrue(randomMedia is Media)
    }

    fun getMockRecentChange(type: String, title: String, oldRevisionId: Long): RecentChange {
        val recentChange = mock(RecentChange::class.java)
        `when`(recentChange!!.type).thenReturn(type)
        `when`(recentChange!!.title).thenReturn(title)
        `when`(recentChange!!.oldRevisionId).thenReturn(oldRevisionId)
        return recentChange
    }

    /**
     * Test for getting first revision of file
     */
    @Test
    fun getFirstRevisionOfFile() {
        `when`(okHttpJsonApiClient?.getFirstRevisionOfFile(ArgumentMatchers.anyString()))
                .thenReturn(Single.just(mock(MwQueryPage.Revision::class.java)))
        val firstRevisionOfFile = reviewHelper?.getFirstRevisionOfFile("Test.jpg")?.blockingGet()

        assertTrue(firstRevisionOfFile is MwQueryPage.Revision)
    }
}