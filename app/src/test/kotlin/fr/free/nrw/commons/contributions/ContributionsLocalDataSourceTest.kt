package fr.free.nrw.commons.contributions

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.upload.FileUtilsWrapper
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.io.File

/**
 * Tests marking a contribution after its file page is gone from Commons.
 */
class ContributionsLocalDataSourceTest {
    @Mock
    private lateinit var defaultKvStore: JsonKvStore

    @Mock
    private lateinit var contributionDao: ContributionDao

    @Mock
    private lateinit var fileUtilsWrapper: FileUtilsWrapper

    private lateinit var localDataSource: ContributionsLocalDataSource

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        localDataSource = ContributionsLocalDataSource(defaultKvStore, contributionDao, fileUtilsWrapper)
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

    @Test
    fun deleteContributionDeletesTheCopyOfItsFile() {
        val copy = File("copy.jpg")
        val contribution = mock(Contribution::class.java)
        `when`(contribution.localUriPath).thenReturn(copy)
        `when`(contributionDao.delete(contribution)).thenReturn(Completable.complete())

        localDataSource.deleteContribution(contribution).blockingAwait()

        verify(fileUtilsWrapper).deleteUploadFile(copy)
    }

    @Test
    fun deleteContributionsWithStatesDeletesTheCopiesOfTheirFiles() {
        val states = listOf(Contribution.STATE_QUEUED, Contribution.STATE_FAILED)
        val first = mock(Contribution::class.java)
        val second = mock(Contribution::class.java)
        `when`(first.localUriPath).thenReturn(File("first.jpg"))
        `when`(second.localUriPath).thenReturn(File("second.jpg"))
        `when`(contributionDao.getContribution(states)).thenReturn(Single.just(listOf(first, second)))
        `when`(contributionDao.deleteContributionsWithStates(states)).thenReturn(Completable.complete())

        localDataSource.deleteContributionsWithStates(states).blockingAwait()

        verify(fileUtilsWrapper).deleteUploadFile(File("first.jpg"))
        verify(fileUtilsWrapper).deleteUploadFile(File("second.jpg"))
    }
}
