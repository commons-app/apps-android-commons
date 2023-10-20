package fr.free.nrw.commons.upload

import androidx.lifecycle.MutableLiveData
import categoryItem
import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.categories.CategoriesContract
import fr.free.nrw.commons.upload.categories.CategoriesPresenter
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import media
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoAnnotations
import java.lang.reflect.Method


/**
 * The class contains unit test cases for CategoriesPresenter
 */
class CategoriesPresenterTest {
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
        whenever(repository.getCategories(repository.selectedExistingCategories))
            .thenReturn(Observable.just(mutableListOf(categoryItem())))
        whenever(repository.searchAll("mock", emptyList(), repository.selectedDepictions))
            .thenReturn(Observable.just(mutableListOf(categoryItem())))
        val method: Method = CategoriesPresenter::class.java.getDeclaredMethod(
            "searchResults",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(categoriesPresenter, "mock")    }

    /**
     * unit test case for method CategoriesPresenter.searchForCategories
     */
    @Test
    fun `searchForCategories combines selection and search results without years distinctly`() {
        categoriesPresenter.onAttachView(view)
        val liveData = MutableLiveData<List<CategoryItem>>()
        categoriesPresenter.setCategoryList(liveData)
        categoriesPresenter.setCategoryListValue(listOf(
            categoryItem("selected", "", "", true)))
        val nonEmptyCaptionUploadItem = mock<UploadItem>()
        whenever(nonEmptyCaptionUploadItem.uploadMediaDetails)
            .thenReturn(listOf(UploadMediaDetail(captionText = "nonEmpty")))
        val emptyCaptionUploadItem = mock<UploadItem>()
        whenever(emptyCaptionUploadItem.uploadMediaDetails)
            .thenReturn(listOf(UploadMediaDetail(captionText = "")))
        whenever(repository.uploads).thenReturn(
            listOf(
                nonEmptyCaptionUploadItem,
                emptyCaptionUploadItem
            )
        )
        whenever(repository.searchAll(any(), any(), any()))
            .thenReturn(
                Observable.just(
                    listOf(
                        categoryItem("selected"),
                        categoryItem("doesContainYear")
                    )
                )
            )
        whenever(repository.containsYear("selected")).thenReturn(false)
        whenever(repository.containsYear("doesContainYear")).thenReturn(true)
        whenever(repository.selectedCategories).thenReturn(listOf(
            categoryItem("selected", "", "",true)))
        categoriesPresenter.searchForCategories("test")
        testScheduler.triggerActions()
        verify(view).showProgress(true)
        verify(view).showProgress(false)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `searchForCategoriesTest sets Error when list is empty`() {
        categoriesPresenter.onAttachView(view)
        val liveData = MutableLiveData<List<CategoryItem>>()
        categoriesPresenter.setCategoryList(liveData)
        // Arrange
        val query = "testQuery"
        val emptyCategories = ArrayList<CategoryItem>()

        liveData.postValue(emptyCategories)

        whenever(repository.searchAll(any(), any(), any()))
            .thenReturn(Observable.just(emptyCategories))
        whenever(repository.selectedCategories).thenReturn(listOf())
        categoriesPresenter.searchForCategories(query)
        testScheduler.triggerActions()
        val method: Method = CategoriesPresenter::class.java.getDeclaredMethod(
            "searchResults",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(categoriesPresenter, query)

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
        whenever(repository.selectedCategories).thenReturn(listOf(item))
        categoriesPresenter.verifyCategories()
        verify(repository).setSelectedCategories(listOf(item.name))
        verify(view).goToNextScreen()
    }

    @Test
    fun `verifyCategories with empty selection show no category selected`() {
        categoriesPresenter.onAttachView(view)
        whenever(repository.selectedCategories).thenReturn(listOf())
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

    @Test
    fun testClearPreviousSelection() {
        categoriesPresenter.clearPreviousSelection()
    }

    @Test
    fun testUpdateCategories() {
        categoriesPresenter.onAttachView(view)
        categoriesPresenter.updateCategories(media(), "[[Category:Test]]")
    }
}
