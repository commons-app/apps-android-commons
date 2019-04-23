package fr.free.nrw.commons.upload

import com.nhaarman.mockito_kotlin.verify
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.categories.CategoriesContract
import fr.free.nrw.commons.upload.categories.CategoriesPresenter
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.*
import kotlin.collections.ArrayList

class CategoriesPresenterTest {
    @Mock
    internal var repository: UploadRepository? = null
    @Mock
    internal var view: CategoriesContract.View? = null

    var categoriesPresenter: CategoriesPresenter? = null

    var testScheduler: Scheduler? = null

    @Mock
    val categoryItems: ArrayList<CategoryItem> = ArrayList()

    @Mock
    var categoryItem: CategoryItem? = null

    var testObservable: Observable<CategoryItem>? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        testScheduler = TestScheduler()
        (categoryItems).add(categoryItem!!)
        testObservable = io.reactivex.Observable.just(categoryItem)
        categoriesPresenter = CategoriesPresenter(repository, testScheduler, testScheduler)
        categoriesPresenter!!.onAttachView(view)
    }

    @Test
    fun searchForCategoriesTest() {
        Mockito.`when`(repository!!.selectedCategories).thenReturn(categoryItems)
        Mockito.`when`(repository!!.searchCategories(ArgumentMatchers.anyString(), ArgumentMatchers.anyList())).thenReturn(testObservable)
        categoriesPresenter!!.searchForCategories(ArgumentMatchers.anyString(), ArgumentMatchers.any(Collections.emptyList<String>().javaClass))
        verify(view!!).showProgress(true)
    }
}