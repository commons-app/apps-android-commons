package fr.free.nrw.commons.upload

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import depictedItem
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsController
import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.explore.depictions.WikipediaArticleClient
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.depicts.DepictsContract
import fr.free.nrw.commons.upload.depicts.DepictsDao
import fr.free.nrw.commons.upload.depicts.DepictsPresenter
import fr.free.nrw.commons.upload.depicts.DepictsSearchHandler
import fr.free.nrw.commons.upload.depicts.DepictsSelectionHandler
import fr.free.nrw.commons.upload.depicts.DepictsSession
import fr.free.nrw.commons.wikidata.WikidataDisambiguationItems
import fr.free.nrw.commons.wikidata.WikidataItemCreationClient
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
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

    private lateinit var searchHandler: DepictsSearchHandler

    private lateinit var selectionHandler: DepictsSelectionHandler

    private lateinit var session: DepictsSession

    @Mock
    lateinit var depictsClient: DepictsClient

    @Mock
    private lateinit var media: Media

    @Mock
    private lateinit var depictsDao: DepictsDao

    @Mock
    private lateinit var controller: BookmarkItemsController

    @Mock
    private lateinit var wikipediaArticleClient: WikipediaArticleClient

    @Mock
    private lateinit var wikidataItemCreationClient: WikidataItemCreationClient

    @Mock
    private lateinit var sessionManager: SessionManager

    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        testScheduler = TestScheduler()

        whenever(depictsDao.depictsList())
            .thenReturn(CompletableDeferred(emptyList()))

        searchHandler =
            DepictsSearchHandler(
                repository,
                { controller },
                { depictsDao },
                testScheduler,
            )
        session = DepictsSession()
        selectionHandler =
            DepictsSelectionHandler(
                repository,
                session,
                testScheduler,
                testScheduler,
            )

        depictsPresenter = DepictsPresenter(repository, testScheduler, testScheduler, session)
        depictsPresenter.controller = controller
        depictsPresenter.depictsDao = depictsDao
        Whitebox.setInternalState(depictsPresenter, "wikipediaArticleClient", wikipediaArticleClient)
        Whitebox.setInternalState(depictsPresenter, "wikidataItemCreationClient", wikidataItemCreationClient)
        Whitebox.setInternalState(depictsPresenter, "sessionManager", sessionManager)

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
        val searchResults =
            listOf(
                depictedItem(id = "nonUnique"),
                depictedItem(id = "nonUnique"),
                depictedItem(
                    instanceOfs = listOf(WikidataDisambiguationItems.CATEGORY.id),
                    id = "unique",
                ),
            )
        whenever(repository.searchAllEntities("")).thenReturn(Flowable.just(searchResults))
        val selectedItem = depictedItem(id = "selected")
        whenever(repository.getSelectedDepictions()).thenReturn(listOf(selectedItem))
        depictsPresenter.searchForDepictions("")
        testScheduler.triggerActions()
        verify(view).showProgress(false)
        verify(view).showError(false)
    }

    @Test
    fun `empty search results with empty term do not show error`() {
        whenever(repository.searchAllEntities("")).thenReturn(Flowable.just(emptyList()))
        depictsPresenter.searchForDepictions("")
        testScheduler.triggerActions()
        verify(view).showProgress(false)
        verify(view).showError(false)
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
    fun `pending Wikipedia depiction requires login`() {
        val pending = depictedItem(id = "pending-wikipedia:en:Test Article")
        whenever(wikipediaArticleClient.articleFromPendingDepictionId(pending.id))
            .thenReturn(WikipediaArticleClient.Article("en", "Test Article"))
        whenever(sessionManager.isUserLoggedIn).thenReturn(false)

        depictsPresenter.onDepictItemClicked(pending)

        verify(view).showDepictionCreationMessage(R.string.depicts_item_creation_login_required)
        verify(repository, never()).onDepictItemClicked(pending, null)
        verify(wikidataItemCreationClient, never()).createItem(anyString(), anyString())
    }

    @Test
    fun `pending Wikipedia depiction creates item and selects it`() {
        val pending = depictedItem(id = "pending-wikipedia:en:Test Article")
        val created = depictedItem(id = "Q123")
        whenever(wikipediaArticleClient.articleFromPendingDepictionId(pending.id))
            .thenReturn(WikipediaArticleClient.Article("en", "Test Article"))
        whenever(sessionManager.isUserLoggedIn).thenReturn(true)
        whenever(wikidataItemCreationClient.createItem("en", "Test Article")).thenReturn(Single.just("Q123"))
        whenever(repository.getDepictions(listOf("Q123"))).thenReturn(Flowable.just(listOf(created)))
        session.depictedItems.value = listOf(pending)

        depictsPresenter.onDepictItemClicked(pending)
        testScheduler.triggerActions()

        verify(repository).onDepictItemClicked(created, null)
        verify(view).showDepictionCreationMessage(R.string.depicts_item_created)
        assert(session.depictedItems.value == listOf(created))
    }

    @Test
    fun `verifyDepictions with non empty selectedDepictions goes to next screen`() {
        whenever(repository.getSelectedDepictions()).thenReturn(listOf(depictedItem()))
        depictsPresenter.verifyDepictions()
        verify(view).goToNextScreen()
    }

    @Test
    fun `verifyDepictions with empty selectedDepictions goes to noDepictionSelected`() {
        whenever(repository.getSelectedDepictions()).thenReturn(emptyList())
        depictsPresenter.verifyDepictions()
        verify(view).noDepictionSelected()
    }

    @Test
    fun testOnAttachViewWithMedia() {
        depictsPresenter.onAttachViewWithMedia(view, Mockito.mock(Media::class.java))
    }

    @Test
    fun testUpdateDepicts() {
        depictsPresenter.updateDepictions(Mockito.mock(Media::class.java))
    }

    @Test
    fun `Test searchResults when media is null`() {
        whenever(repository.searchAllEntities("querystring"))
            .thenReturn(Flowable.just(listOf(depictedItem())))

        searchHandler
            .searchResults("querystring", null)
            .test()
            .assertNoErrors()

        verify(repository, times(1)).searchAllEntities("querystring")
    }

    @Test
    fun `Test searchResults when media is not null`() {
        whenever(repository.getDepictions(repository.getSelectedExistingDepictions()))
            .thenReturn(Flowable.just(listOf(depictedItem())))

        whenever(repository.searchAllEntities("querystring"))
            .thenReturn(Flowable.just(listOf(depictedItem())))

        searchHandler
            .searchResults("querystring", media)
            .test()
            .assertNoErrors()

        verify(repository, times(1)).searchAllEntities("querystring")
    }

    @Test
    fun testSelectNewDepictions() {
        val item = depictedItem(id = "Q1", isSelected = false)
        whenever(repository.getPlaceDepictions()).thenReturn(Single.just(listOf(item)))
        session.media = media

        selectionHandler.selectPlaceDepictions()
        testScheduler.triggerActions()

        Assert.assertTrue(item.isSelected)
        verify(repository).onDepictItemClicked(item, media)
    }

    @Test
    fun testClearPreviousSelection() {
        val method: Method =
            DepictsPresenter::class.java.getDeclaredMethod(
                "clearPreviousSelection",
            )
        method.isAccessible = true
        method.invoke(depictsPresenter)
    }

    @Test
    fun testDepictionFetchAndSelection() {
        val place = Mockito.mock(Place::class.java)
        whenever(place.wikiDataEntityId).thenReturn("Q12345")

        val item = depictedItem(id = "Q12345", isSelected = false)
        whenever(repository.getDepictions(listOf("Q12345")))
            .thenReturn(Flowable.just(listOf(item)))

        depictsPresenter.onPlaceSelectedFromMap(place)
        testScheduler.triggerActions()

        Assert.assertTrue(item.isSelected)
    }
}
