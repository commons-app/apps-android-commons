package fr.free.nrw.commons.upload

import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.categories.CategoriesContract
import fr.free.nrw.commons.upload.categories.CategoriesPresenter
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

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

    private val categoryItems: ArrayList<CategoryItem> = ArrayList()

    @Mock
    lateinit var categoryItem: CategoryItem

    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        testScheduler = TestScheduler()
        categoryItems.add(categoryItem)
        categoriesPresenter = CategoriesPresenter(repository, testScheduler, testScheduler)
        categoriesPresenter.onAttachView(view)
    }

    /**
     * unit test case for method CategoriesPresenter.searchForCategories
     */
    @Test
    fun `searchForCategories combines selection and search results without years distinctly`() {
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
        whenever(repository.searchAll("test", listOf("nonEmpty")))
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
        whenever(repository.selectedCategories).thenReturn(listOf(categoryItem("selected", true)))
        categoriesPresenter.searchForCategories("test")
        testScheduler.triggerActions()
        verify(view).showProgress(true)
        verify(view).showError(null)
        verify(view).setCategories(null)
        verify(view).setCategories(listOf(categoryItem("selected", true)))
        verify(view).showProgress(false)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `searchForCategoriesTest sets Error when list is empty`() {
        whenever(repository.uploads).thenReturn(listOf())
        whenever(repository.searchAll(any(), any())).thenReturn(Observable.just(listOf()))
        whenever(repository.selectedCategories).thenReturn(listOf())
        categoriesPresenter.searchForCategories("test")
        testScheduler.triggerActions()
        verify(view).showProgress(true)
        verify(view).showError(null)
        verify(view).setCategories(null)
        verify(view).setCategories(listOf())
        verify(view).showProgress(false)
        verify(view).showError(R.string.no_categories_found)
        verifyNoMoreInteractions(view)
    }

    /**
     * unit test for method CategoriesPresenter.verifyCategories
     */
    @Test
    fun `verifyCategories with non empty selection goes to next screen`() {
        val item = categoryItem()
        whenever(repository.selectedCategories).thenReturn(listOf(item))
        categoriesPresenter.verifyCategories()
        verify(repository).setSelectedCategories(listOf(item.name))
        verify(view).goToNextScreen()
    }

    @Test
    fun `verifyCategories with empty selection show no category selected`() {
        whenever(repository.selectedCategories).thenReturn(listOf())
        categoriesPresenter.verifyCategories()
        verify(view).showNoCategorySelected()
    }

    /**
     * Test onCategory Item clicked
     */
    @Test
    fun onCategoryItemClickedTest() {
        categoriesPresenter.onCategoryItemClicked(categoryItem)
        verify(repository).onCategoryClicked(categoryItem)
    }

    private fun categoryItem(name: String = "name", selected: Boolean = false) =
        CategoryItem(name, selected)
}
