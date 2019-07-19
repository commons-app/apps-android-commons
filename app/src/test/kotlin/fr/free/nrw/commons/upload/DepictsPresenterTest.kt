package fr.free.nrw.commons.upload

import com.nhaarman.mockito_kotlin.verify
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.depicts.DepictsContract
import fr.free.nrw.commons.upload.depicts.DepictsPresenter
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class DepictsPresenterTest {
    @Mock
    internal var repository: UploadRepository? = null
    @Mock
    internal var view: DepictsContract.View? = null

    var depictsPresenter: DepictsPresenter? = null

    var testScheduler: TestScheduler? = null

    val depictedItems: ArrayList<DepictedItem> = ArrayList()

    @Mock
    lateinit var depictedItem: DepictedItem

    var testObservable: Observable<DepictedItem>? = null

    private val imageTitleList = ArrayList<UploadMediaDetail>()

    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        testScheduler = TestScheduler()
        depictedItems.add(depictedItem)
        testObservable = Observable.just(depictedItem)
        depictsPresenter = DepictsPresenter(repository, testScheduler, testScheduler)
        depictsPresenter?.onAttachView(view)
    }

    @Test
    fun searchEnglishDepictionsTest() {
        Mockito.`when`(repository?.sortBySimilarity(ArgumentMatchers.anyString())).thenReturn(Comparator<CategoryItem> { _, _ -> 1 })
        Mockito.`when`(repository?.selectedDepictions).thenReturn(depictedItems)
        Mockito.`when`(repository?.searchAllEntities(ArgumentMatchers.anyString(), ArgumentMatchers.anyList())).thenReturn(Observable.empty())
        depictsPresenter?.searchForDepictions("test")
        verify(view)?.showProgress(true)
        verify(view)?.showError()
        verify(view)?.setDepictsList(null)
        testScheduler?.triggerActions()
        verify(view)?.showProgress(false)
    }

    @Test
    fun searchOtherLanguageDepictions() {
        Mockito.`when`(repository?.sortBySimilarity(ArgumentMatchers.anyString())).thenReturn(Comparator<CategoryItem> { _, _ -> 1 })
        Mockito.`when`(repository?.selectedDepictions).thenReturn(depictedItems)
        Mockito.`when`(repository?.searchAllEntities(ArgumentMatchers.anyString(), ArgumentMatchers.anyList())).thenReturn(Observable.empty())
        depictsPresenter?.searchForDepictions("वी")
        verify(view)?.showProgress(true)
        verify(view)?.showError()
        verify(view)?.setDepictsList(null)
        testScheduler?.triggerActions()
        verify(view)?.showProgress(false)
    }

    @Test
    fun searchForNonExistingDepictions() {
        Mockito.`when`(repository?.sortBySimilarity(ArgumentMatchers.anyString())).thenReturn(Comparator<CategoryItem> { _, _ -> 1 })
        Mockito.`when`(repository?.selectedDepictions).thenReturn(depictedItems)
        Mockito.`when`(repository?.searchAllEntities(ArgumentMatchers.anyString(), ArgumentMatchers.anyList())).thenReturn(Observable.empty())
        depictsPresenter?.searchForDepictions("******")
        verify(view)?.showProgress(true)
        verify(view)?.setDepictsList(null)
        testScheduler?.triggerActions()
        verify(view)?.setDepictsList(null)
        verify(view)?.showProgress(false)
    }
}