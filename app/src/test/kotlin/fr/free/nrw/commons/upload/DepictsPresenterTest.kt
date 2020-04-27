package fr.free.nrw.commons.upload

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.jraska.livedata.test
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.explore.depictions.DepictsClient.NO_DEPICTED_IMAGE
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.depicts.DepictsContract
import fr.free.nrw.commons.upload.depicts.DepictsPresenter
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations


class DepictsPresenterTest {

    @get:Rule
    var testRule = InstantTaskExecutorRule()

    @Mock
    internal lateinit var repository: UploadRepository

    @Mock
    internal lateinit var view: DepictsContract.View

    private lateinit var depictsPresenter: DepictsPresenter

    private lateinit var testScheduler: TestScheduler

    @Mock
    lateinit var depictsClient: DepictsClient

    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        testScheduler = TestScheduler()
        depictsPresenter = DepictsPresenter(repository, testScheduler, testScheduler, depictsClient)
        depictsPresenter.onAttachView(view)
    }

    @Test
    fun `Search emission shows view progress`() {
        depictsPresenter.searchForDepictions("")
        testScheduler.triggerActions()
        verify(view).showProgress(false)
    }

    @Test
    fun `search results emission returns distinct results + selected items`() {
        val searchResults = listOf(depictedItem(), depictedItem())
        whenever(repository.searchAllEntities("")).thenReturn(Flowable.just(searchResults))
        val selectedItem = depictedItem(id = "selected")
        whenever(repository.selectedDepictions).thenReturn(listOf(selectedItem))
        depictsPresenter.searchForDepictions("")
        testScheduler.triggerActions()
        verify(view).showProgress(false)
        verify(view).showError(false)
        depictsPresenter.depictedItems
            .test()
            .assertValue(listOf(selectedItem, depictedItem()))
    }

    @Test
    fun `searchResults retrieve imageUrls from cache`() {
        val depictedItem = depictedItem()
        whenever(depictsClient.getP18ForItem(depictedItem.id)).thenReturn(Single.just("url"))
        depictsPresenter.fetchThumbnailForEntityId(depictedItem)
        testScheduler.triggerActions()
        val searchResults = listOf(depictedItem(), depictedItem())
        whenever(repository.searchAllEntities("")).thenReturn(Flowable.just(searchResults))
        depictsPresenter.searchForDepictions("")
        testScheduler.triggerActions()
        depictsPresenter.depictedItems
            .test()
            .assertValue(listOf(depictedItem(imageUrl = "url")))
    }

    @Test
    fun `empty search results with empty term do not show error`() {
        whenever(repository.searchAllEntities("")).thenReturn(Flowable.just(emptyList()))
        depictsPresenter.searchForDepictions("")
        testScheduler.triggerActions()
        verify(view).showProgress(false)
        verify(view).showError(false)
        depictsPresenter.depictedItems
            .test()
            .assertValue(emptyList())
    }

    @Test
    fun `empty search results with non empty term do show error`() {
        whenever(repository.searchAllEntities("a")).thenReturn(Flowable.just(emptyList()))
        depictsPresenter.searchForDepictions("a")
        testScheduler.triggerActions()
        verify(view).showProgress(false)
        verify(view).showError(true)
        depictsPresenter.depictedItems
            .test()
            .assertValue(emptyList())
    }

    @Test
    fun `search error shows error`() {
        whenever(repository.searchAllEntities("")).thenReturn(Flowable.error(Exception()))
        depictsPresenter.searchForDepictions("")
        testScheduler.triggerActions()
        verify(view).showProgress(false)
        verify(view).showError(true)
    }

    @Test
    fun `onPreviousButtonClicked goes to previous screen`() {
        depictsPresenter.onPreviousButtonClicked()
        verify(view).goToPreviousScreen()
    }

    @Test
    fun `onDepictItemClicked calls repository`() {
        val depictedItem = depictedItem()
        depictsPresenter.onDepictItemClicked(depictedItem)
        verify(repository).onDepictItemClicked(depictedItem)
    }

    @Test
    fun `verifyDepictions with non empty selectedDepictions goes to next screen`() {
        whenever(repository.selectedDepictions).thenReturn(listOf(depictedItem()))
        depictsPresenter.verifyDepictions()
        verify(view).goToNextScreen()
    }

    @Test
    fun `verifyDepictions with empty selectedDepictions goes to noDepictionSelected`() {
        whenever(repository.selectedDepictions).thenReturn(emptyList())
        depictsPresenter.verifyDepictions()
        verify(view).noDepictionSelected()
    }

    @Test
    fun `image urls fetched from network update the view`() {
        val depictedItem = depictedItem()
        whenever(depictsClient.getP18ForItem(depictedItem.id)).thenReturn(Single.just("url"))
        depictsPresenter.fetchThumbnailForEntityId(depictedItem)
        testScheduler.triggerActions()
        verify(view).onUrlFetched(depictedItem, "url")
    }

    @Test
    fun `image urls fetched from network filter NO_DEPICTED_IMAGE`() {
        val depictedItem = depictedItem()
        whenever(depictsClient.getP18ForItem(depictedItem.id))
            .thenReturn(Single.just(NO_DEPICTED_IMAGE))
        depictsPresenter.fetchThumbnailForEntityId(depictedItem)
        testScheduler.triggerActions()
        verify(view, never()).onUrlFetched(depictedItem, NO_DEPICTED_IMAGE)
    }

    @Test
    fun `successive image urls fetched from cache`() {
        val depictedItem = depictedItem()
        whenever(depictsClient.getP18ForItem(depictedItem.id)).thenReturn(Single.just("url"))
        depictsPresenter.fetchThumbnailForEntityId(depictedItem)
        testScheduler.triggerActions()
        verify(view).onUrlFetched(depictedItem, "url")
        depictsPresenter.fetchThumbnailForEntityId(depictedItem)
        testScheduler.triggerActions()
        verify(view, times(2)).onUrlFetched(depictedItem, "url")
    }
}

fun depictedItem(
    name: String = "label",
    description: String = "desc",
    imageUrl: String = "",
    isSelected: Boolean = false,
    id: String = "entityId"
) = DepictedItem(name, description, imageUrl, isSelected, id)
