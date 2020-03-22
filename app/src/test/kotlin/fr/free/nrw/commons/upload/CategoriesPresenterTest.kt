package fr.free.nrw.commons.upload

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.categories.CategoriesContract
import fr.free.nrw.commons.upload.categories.CategoriesPresenter
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
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
    fun searchForCategoriesTest() {
        whenever(repository.sortBySimilarity(ArgumentMatchers.anyString())).thenReturn(Comparator<CategoryItem> { _, _ -> 1 })
        whenever(repository.selectedCategories).thenReturn(categoryItems)
        whenever(repository.searchAll(ArgumentMatchers.anyString(), ArgumentMatchers.anyList())).thenReturn(Observable.empty())
        categoriesPresenter.searchForCategories("test")
        verify(view).showProgress(true)
        verify(view).showError(null)
        verify(view).setCategories(null)
        testScheduler.triggerActions()
        verify(view).setCategories(categoryItems)
        verify(view).showProgress(false)
    }

    /**
     * unit test for method CategoriesPresenter.verifyCategories
     */
    @Test
    fun verifyCategoriesTest() {
        whenever(repository.selectedCategories).thenReturn(categoryItems)
        categoriesPresenter.verifyCategories()
        verify(repository).setSelectedCategories(ArgumentMatchers.anyList())
        verify(view).goToNextScreen()
    }

    /**
     * Test onCategory Item clicked
     */
    @Test
    fun onCategoryItemClickedTest() {
        categoriesPresenter.onCategoryItemClicked(categoryItem)
        verify(repository).onCategoryClicked(categoryItem)
    }
}
