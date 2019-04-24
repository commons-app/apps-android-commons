package fr.free.nrw.commons.upload

import com.nhaarman.mockito_kotlin.verify
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
import org.mockito.Mockito
import org.mockito.MockitoAnnotations


class CategoriesPresenterTest {
    @Mock
    internal var repository: UploadRepository? = null
    @Mock
    internal var view: CategoriesContract.View? = null

    var categoriesPresenter: CategoriesPresenter? = null

    var testScheduler: TestScheduler? = null

    val categoryItems: ArrayList<CategoryItem> = ArrayList()

    @Mock
    var categoryItem: CategoryItem? = null

    var testObservable: Observable<CategoryItem>? = null

    private val imageTitleList = ArrayList<String>()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        testScheduler = TestScheduler()
        categoryItems.add(categoryItem!!)
        testObservable = Observable.just(categoryItem)
        categoriesPresenter = CategoriesPresenter(repository, testScheduler, testScheduler)
        categoriesPresenter!!.onAttachView(view)
    }

    @Test
    fun searchForCategoriesTest() {
        Mockito.`when`(repository!!.sortBySimilarity(ArgumentMatchers.anyString())).thenReturn(Comparator<CategoryItem> { _, _ -> 1 })
        Mockito.`when`(repository!!.selectedCategories).thenReturn(categoryItems)
        Mockito.`when`(repository!!.searchCategories(ArgumentMatchers.anyString(), ArgumentMatchers.anyList())).thenReturn(Observable.empty())
        Mockito.`when`(repository!!.searchAll(ArgumentMatchers.anyString(), ArgumentMatchers.anyList())).thenReturn(Observable.empty())
        Mockito.`when`(repository!!.defaultCategories(ArgumentMatchers.anyList())).thenReturn(Observable.empty())
        categoriesPresenter!!.searchForCategories("test", imageTitleList)
        verify(view!!).showProgress(true)
        verify(view!!).showError(null)
        verify(view!!).setCategories(null)
        testScheduler!!.triggerActions()
        verify(view!!).setCategories(categoryItems)
        verify(view!!).showProgress(false)
    }

    @Test
    fun verifyCategoriesTest() {
        Mockito.`when`(repository!!.selectedCategories).thenReturn(categoryItems)
        categoriesPresenter!!.verifyCategories()
        verify(repository!!).setSelectedCategories(ArgumentMatchers.anyList())
        verify(view!!).goToNextScreen()
    }
}