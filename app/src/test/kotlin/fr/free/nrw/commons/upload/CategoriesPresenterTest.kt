package fr.free.nrw.commons.upload

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import categoryItem
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryEditHelper
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.categories.CategoriesContract
import fr.free.nrw.commons.upload.categories.CategoriesPresenter
import fr.free.nrw.commons.upload.categories.CategorySearchResults
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import media
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.io.IOException

/**
 * The class contains unit test cases for CategoriesPresenter
 */
class CategoriesPresenterTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    internal lateinit var repository: UploadRepository

    @Mock
    internal lateinit var view: CategoriesContract.View

    private lateinit var categoriesPresenter: CategoriesPresenter

    private lateinit var testScheduler: TestScheduler

    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        testScheduler = TestScheduler()
        categoriesPresenter = CategoriesPresenter(repository, testScheduler, testScheduler)
    }

    @Test
    @Throws(Exception::class)
    fun testOnAttachViewWithMedia() {
        categoriesPresenter.onAttachViewWithMedia(view, media())
    }

    @Test
    @Throws(Exception::class)
    fun `Test onAttachViewWithMedia when media is not null`() {
        categoriesPresenter.onAttachViewWithMedia(view, media())
        whenever(repository.getCategories(repository.getSelectedExistingCategories()))
            .thenReturn(Observable.just(mutableListOf(categoryItem())))
        whenever(repository.searchAll("mock", emptyList(), repository.getSelectedDepictions()))
            .thenReturn(Observable.just(mutableListOf(categoryItem())))
        CategorySearchResults(repository, testScheduler).searchResults("mock", media())
    }

    /**
     * unit test case for method CategoriesPresenter.searchForCategories
     */
    @Test
    fun `searchForCategories combines selection and search results without years distinctly`() {
        categoriesPresenter.onAttachView(view)
        val liveData = categoriesPresenter.categoryList
        liveData.postValue(
            listOf(
                categoryItem("selected", "", "", true),
            ),
        )
        val nonEmptyCaptionUploadItem = mock<UploadItem>()
        whenever(nonEmptyCaptionUploadItem.uploadMediaDetails)
            .thenReturn(mutableListOf(UploadMediaDetail(captionText = "nonEmpty")))
        val emptyCaptionUploadItem = mock<UploadItem>()
        whenever(emptyCaptionUploadItem.uploadMediaDetails)
            .thenReturn(mutableListOf(UploadMediaDetail(captionText = "")))
        whenever(repository.getUploads()).thenReturn(
            listOf(
                nonEmptyCaptionUploadItem,
                emptyCaptionUploadItem,
            ),
        )
        whenever(repository.searchAll(any(), any(), any()))
            .thenReturn(
                Observable.just(
                    listOf(
                        categoryItem("selected"),
                        categoryItem("doesContainYear"),
                    ),
                ),
            )
        whenever(repository.isSpammyCategory("selected")).thenReturn(false)
        whenever(repository.isSpammyCategory("doesContainYear")).thenReturn(true)
        whenever(repository.getSelectedCategories()).thenReturn(
            listOf(
                categoryItem("selected", "", "", true),
            ),
        )
        categoriesPresenter.searchForCategories("test")
        testScheduler.triggerActions()
        verify(view).showProgress(true)
        verify(view).showProgress(false)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `searchForCategoriesTest sets Error when list is empty`() {
        categoriesPresenter.onAttachView(view)
        val liveData = categoriesPresenter.categoryList
        // Arrange
        val query = "testQuery"
        val emptyCategories = ArrayList<CategoryItem>()

        liveData.postValue(emptyCategories)

        whenever(repository.searchAll(any(), any(), any()))
            .thenReturn(Observable.just(emptyCategories))
        whenever(repository.getSelectedCategories()).thenReturn(listOf())
        categoriesPresenter.searchForCategories(query)
        testScheduler.triggerActions()
        CategorySearchResults(repository, testScheduler).searchResults(query, null)

        verify(view).showProgress(true)
        verify(view).showProgress(false)
        verify(view).showError(R.string.no_categories_found)
        verifyNoMoreInteractions(view)
    }

    /**
     * unit test for method CategoriesPresenter.verifyCategories
     */
    @Test
    fun `verifyCategories with non empty selection goes to next screen`() {
        categoriesPresenter.onAttachView(view)
        val item = categoryItem()
        whenever(repository.getSelectedCategories()).thenReturn(listOf(item))
        categoriesPresenter.verifyCategories()
        verify(repository).setSelectedCategories(listOf(item.name))
        verify(view).goToNextScreen()
    }

    @Test
    fun `verifyCategories with empty selection show no category selected`() {
        categoriesPresenter.onAttachView(view)
        whenever(repository.getSelectedCategories()).thenReturn(listOf())
        categoriesPresenter.verifyCategories()
        verify(view).showNoCategorySelected()
    }

    /**
     * Test onCategory Item clicked
     */
    @Test
    fun onCategoryItemClickedTest() {
        val categoryItem = categoryItem()
        categoriesPresenter.onCategoryItemClicked(categoryItem)
        verify(repository).onCategoryClicked(categoryItem, null)
    }

    /**
     * A failed search is reported to the user and does not end the subscription, so the next
     * query still returns results (#6438).
     */
    @Test
    fun `searchForCategories reports an API error and accepts the next query`() {
        categoriesPresenter.onAttachView(view)
        val liveData = categoriesPresenter.categoryList

        whenever(repository.getUploads()).thenReturn(emptyList())
        whenever(repository.getSelectedCategories()).thenReturn(emptyList())
        whenever(repository.searchAll(any(), any(), any()))
            .thenReturn(Observable.error(RuntimeException("API timeout")))

        categoriesPresenter.searchForCategories("Cat")
        testScheduler.triggerActions()

        verify(view).showError(R.string.error_loading_categories)
        verify(view, never()).showError(R.string.no_categories_found)
        assertNull(liveData.value)

        whenever(repository.searchAll(any(), any(), any()))
            .thenReturn(Observable.just(listOf(categoryItem("Cats"))))
        whenever(repository.isSpammyCategory("Cats")).thenReturn(false)

        categoriesPresenter.searchForCategories("Cat")
        testScheduler.triggerActions()

        assertEquals(listOf("Cats"), liveData.value?.map { it.name })
    }

    @Test
    fun testClearPreviousSelection() {
        categoriesPresenter.clearPreviousSelection()
    }

    @Test
    fun testUpdateCategories() {
        categoriesPresenter.onAttachView(view)
        categoriesPresenter.updateCategories(media(), "[[Category:Test]]")
    }

    @Test
    fun `updateCategories sends the saved categories to the details screen and closes`() {
        saveCategories(Single.just(true))

        verify(categoryEditHelper).makeCategoryEdit(any(), any(), eq(listOf("New", "Old")), any())
        verify(view).dismissProgressDialog()
        verify(repository).cleanup()
        verify(view).refreshCategories(listOf("New", "Old"))
        verify(view).goBackToPreviousScreen()
        assertTrue(swallowedErrors.isEmpty())
    }

    @Test
    fun `updateCategories closes the editor even if refreshing the details screen fails`() {
        val refreshError = NullPointerException()
        doThrow(refreshError).whenever(view).refreshCategories(any())

        saveCategories(Single.just(true))

        inOrder(view) {
            verify(view).goBackToPreviousScreen()
            verify(view).refreshCategories(listOf("New", "Old"))
        }
        assertEquals(listOf(refreshError), swallowedErrors)
    }

    @Test
    fun `updateCategories keeps the selection if the editor cannot close`() {
        doThrow(IllegalStateException()).whenever(view).goBackToPreviousScreen()

        saveCategories(Single.just(true))

        verify(repository, never()).cleanup()
        verify(view, never()).refreshCategories(any())
    }

    @Test
    fun `updateCategories keeps the selection when the edit is not saved`() {
        saveCategories(Single.just(false))

        verify(view).dismissProgressDialog()
        verify(view).showError(R.string.category_edit_helper_edit_message_else)
        verify(repository, never()).cleanup()
        verify(view, never()).goBackToPreviousScreen()
    }

    @Test
    fun `updateCategories keeps the selection when the edit fails`() {
        saveCategories(Single.error(IOException()))

        verify(view).dismissProgressDialog()
        verify(view).showError(R.string.category_edit_helper_edit_message_else)
        verify(repository, never()).cleanup()
        verify(view, never()).goBackToPreviousScreen()
    }

    @Test
    fun `updateCategories closes the editor without a message when nothing changed`() {
        categoriesPresenter.categoryEditHelper = categoryEditHelper
        categoriesPresenter.onAttachView(view)
        whenever(view.getExistingCategories()).thenReturn(listOf("Old"))
        whenever(repository.getSelectedCategories()).thenReturn(emptyList())
        whenever(repository.getSelectedExistingCategories()).thenReturn(listOf("Old"))

        categoriesPresenter.updateCategories(media(), "[[Category:Old]]")

        verify(view).goBackToPreviousScreen()
        verify(repository).cleanup()
        verify(view, never()).showNoCategorySelected()
        verify(categoryEditHelper, never()).makeCategoryEdit(any(), any(), any(), any())
    }

    @Test
    fun `updateCategories says no categories selected when the file has none`() {
        categoriesPresenter.categoryEditHelper = categoryEditHelper
        categoriesPresenter.onAttachView(view)
        whenever(view.getExistingCategories()).thenReturn(emptyList())
        whenever(repository.getSelectedCategories()).thenReturn(emptyList())
        whenever(repository.getSelectedExistingCategories()).thenReturn(emptyList())

        categoriesPresenter.updateCategories(media(), "")

        verify(view).showNoCategorySelected()
        verify(categoryEditHelper, never()).makeCategoryEdit(any(), any(), any(), any())
    }

    private val categoryEditHelper = mock<CategoryEditHelper>()
    private val swallowedErrors = mutableListOf<Throwable>()

    private fun saveCategories(result: Single<Boolean>) {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline() }
        // errors thrown from onSuccess go to this handler, which the app leaves empty
        RxJavaPlugins.setErrorHandler { swallowedErrors.add(it) }
        categoriesPresenter.categoryEditHelper = categoryEditHelper
        categoriesPresenter.onAttachView(view)
        whenever(view.getFragmentContext()).thenReturn(mock<Context>())
        whenever(view.getExistingCategories()).thenReturn(listOf("Old"))
        whenever(repository.getSelectedCategories()).thenReturn(listOf(categoryItem("New")))
        whenever(repository.getSelectedExistingCategories()).thenReturn(listOf("Old"))
        whenever(categoryEditHelper.makeCategoryEdit(any(), any(), any(), any())).thenReturn(result)

        categoriesPresenter.updateCategories(media(), "[[Category:Old]]")
    }

    @After
    fun tearDown() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }
}
