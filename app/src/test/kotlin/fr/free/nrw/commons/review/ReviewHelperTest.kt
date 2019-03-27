package fr.free.nrw.commons.review

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.media.model.MwQueryPage
import fr.free.nrw.commons.mwapi.MediaWikiApi
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.mwapi.model.RecentChange
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

class ReviewHelperTest {

    @Mock
    internal var okHttpJsonApiClient: OkHttpJsonApiClient? = null
    @Mock
    internal var mediaWikiApi: MediaWikiApi? = null

    @InjectMocks
    var reviewHelper: ReviewHelper? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun getRandomMedia() {
        `when`(okHttpJsonApiClient!!.recentFileChanges)
                .thenReturn(Single.just(listOf(RecentChange("test", "File:Test1.jpeg", "0"),
                        RecentChange("test", "File:Test2.png", "0"),
                        RecentChange("test", "File:Test3.jpg", "0"))))

        `when`(mediaWikiApi!!.pageExists(ArgumentMatchers.anyString()))
                .thenReturn(Single.just(true))
        val randomMedia = reviewHelper!!.randomMedia.blockingGet()

        assertTrue(randomMedia is Media)
    }

    @Test
    fun getFirstRevisionOfFile() {
        `when`(okHttpJsonApiClient!!.getFirstRevisionOfFile(ArgumentMatchers.anyString()))
                .thenReturn(Single.just(mock(MwQueryPage.Revision::class.java)))
        val firstRevisionOfFile = reviewHelper!!.getFirstRevisionOfFile("Test.jpg").blockingGet()

        assertTrue(firstRevisionOfFile is MwQueryPage.Revision)
    }
}