package fr.free.nrw.commons.upload

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.jraska.livedata.test
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import depictedItem
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.depicts.DepictsContract
import fr.free.nrw.commons.upload.depicts.DepictsPresenter
import fr.free.nrw.commons.wikidata.WikidataDisambiguationItems
import io.reactivex.Flowable
import io.reactivex.schedulers.TestScheduler
import media
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.lang.reflect.Method


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
        depictsPresenter = DepictsPresenter(repository, testScheduler, testScheduler)
        depictsPresenter.onAttachView(view)
    }

    @Test
    fun `Search emission shows view progress`() {
        depictsPresenter.searchForDepictions("")
        testScheduler.triggerActions()
        verify(view).showProgress(false)
    }

    @Test
    fun `search results emission returns distinct results + selected items without disambiguations`() {
        val searchResults = listOf(
            depictedItem(id="nonUnique"),
            depictedItem(id="nonUnique"),
            depictedItem(
                instanceOfs = listOf(WikidataDisambiguationItems.CATEGORY.id),
                id = "unique"
            )
        )
        whenever(repository.searchAllEntities("")).thenReturn(Flowable.just(searchResults))
        val selectedItem = depictedItem(id = "selected")
        whenever(repository.selectedDepictions).thenReturn(listOf(selectedItem))
        depictsPresenter.searchForDepictions("")
        testScheduler.triggerActions()
        verify(view).showProgress(false)
        verify(view).showError(true)
    }


    @Test
    fun `empty search results with empty term do not show error`() {
        whenever(repository.searchAllEntities("")).thenReturn(Flowable.just(emptyList()))
        depictsPresenter.searchForDepictions("")
        testScheduler.triggerActions()
        verify(view).showProgress(false)
        verify(view).showError(true)
    }

    @Test
    fun `empty search results with non empty term do show error`() {
        whenever(repository.searchAllEntities("a")).thenReturn(Flowable.just(emptyList()))
        depictsPresenter.searchForDepictions("a")
        testScheduler.triggerActions()
        verify(view).showProgress(false)
        verify(view).showError(true)
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
        verify(repository).onDepictItemClicked(depictedItem, null)
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
    fun testOnAttachViewWithMedia() {
        depictsPresenter.onAttachViewWithMedia(view, Mockito.mock(Media::class.java))
    }

    @Test
    fun testUpdateDepicts() {
        depictsPresenter.updateDepicts(Mockito.mock(Media::class.java))
    }
}
