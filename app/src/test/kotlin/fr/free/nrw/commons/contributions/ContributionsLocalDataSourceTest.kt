package fr.free.nrw.commons.contributions

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.kvstore.JsonKvStore
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

/**
 * Tests marking a contribution after its file page is gone from Commons.
 */
class ContributionsLocalDataSourceTest {
    @Mock
    private lateinit var defaultKvStore: JsonKvStore

    @Mock
    private lateinit var contributionDao: ContributionDao

    private lateinit var localDataSource: ContributionsLocalDataSource

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        localDataSource = ContributionsLocalDataSource(defaultKvStore, contributionDao)
    }

    @Test
    fun markCompletedContributionDeletedLeavesUploadsAlone() {
        val media = Media(filename = "File:Gone.jpg")
        val completed = Contribution(media = media, state = Contribution.STATE_COMPLETED)
        val uploading =
            Contribution(
                media = media,
                pageId = "still-uploading",
                state = Contribution.STATE_QUEUED,
            )
        `when`(contributionDao.getContributionWithTitle("File:Gone.jpg"))
            .thenReturn(listOf(completed, uploading))

        localDataSource.markCompletedContributionDeleted("File:Gone.jpg").blockingAwait()

        assertEquals(Contribution.STATE_DELETED, completed.state)
        assertEquals(Contribution.STATE_QUEUED, uploading.state)
        verify(contributionDao).updateSynchronous(completed)
        verify(contributionDao, never()).updateSynchronous(uploading)
    }
}
