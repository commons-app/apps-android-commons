package fr.free.nrw.commons.upload

//import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.depicts.DepictsContract
import fr.free.nrw.commons.upload.depicts.DepictsFragment
import fr.free.nrw.commons.upload.depicts.DepictsPresenter
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class DepictsPresenterTest {
    @Mock
    internal var repository: UploadRepository? = null
    @Mock
    internal var view: DepictsContract.View? = null

    var depictsPresenter: DepictsPresenter? = null

    var depictsFragment: DepictsFragment? = null

    var testScheduler: TestScheduler? = null

    var depictsClient : DepictsClient? = null

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
        depictedItem = DepictedItem("label", "desc", "", false, "entityId")
        depictedItems.add(depictedItem)
        testObservable = Observable.just(depictedItem)
        depictsPresenter = DepictsPresenter(repository, testScheduler, testScheduler, depictsClient)
        depictsFragment = DepictsFragment()
        depictsPresenter?.onAttachView(view)
    }

    @Test
    fun searchEnglishDepictionsTest() {
        whenever(repository?.sortBySimilarity(ArgumentMatchers.anyString())).thenReturn(Comparator<CategoryItem> { _, _ -> 1 })
        whenever(repository?.selectedDepictions).thenReturn(depictedItems)
        whenever(repository?.searchAllEntities(ArgumentMatchers.anyString())).thenReturn(Observable.empty())
        depictsPresenter?.searchForDepictions("test")
        verify(view)?.showProgress(true)
        verify(view)?.showError(true)
        verify(view)?.setDepictsList(null)
        testScheduler?.triggerActions()
        verify(view)?.showProgress(false)
    }

    @Test
    fun searchOtherLanguageDepictions() {
        whenever(repository?.sortBySimilarity(ArgumentMatchers.anyString())).thenReturn(Comparator<CategoryItem> { _, _ -> 1 })
        whenever(repository?.selectedDepictions).thenReturn(depictedItems)
        whenever(repository?.searchAllEntities(ArgumentMatchers.anyString())).thenReturn(Observable.empty())
        depictsPresenter?.searchForDepictions("वी")
        verify(view)?.showProgress(true)
        verify(view)?.showError(true)
        verify(view)?.setDepictsList(null)
        testScheduler?.triggerActions()
        verify(view)?.showProgress(false)
    }

    @Test
    fun searchForNonExistingDepictions() {
        whenever(repository?.sortBySimilarity(ArgumentMatchers.anyString())).thenReturn(Comparator<CategoryItem> { _, _ -> 1 })
        whenever(repository?.selectedDepictions).thenReturn(depictedItems)
        whenever(repository?.searchAllEntities(ArgumentMatchers.anyString())).thenReturn(Observable.empty())
        depictsPresenter?.searchForDepictions("******")
        verify(view)?.showProgress(true)
        verify(view)?.setDepictsList(null)
        testScheduler?.triggerActions()
        verify(view)?.setDepictsList(null)
        verify(view)?.showProgress(false)
    }

    @Test
    fun setSingleDepiction() {
        whenever(repository?.sortBySimilarity(ArgumentMatchers.anyString())).thenReturn(Comparator<CategoryItem> { _, _ -> 1 })
        whenever(repository?.selectedDepictions).thenReturn(depictedItems)
        whenever(repository?.searchAllEntities(ArgumentMatchers.anyString())).thenReturn(Observable.empty())
        depictsPresenter?.onDepictItemClicked(depictedItem)
        depictsPresenter?.verifyDepictions()
        verify(view)?.goToNextScreen()
    }

    @Test
    fun setMultipleDepictions() {
        whenever(repository?.sortBySimilarity(ArgumentMatchers.anyString())).thenReturn(Comparator<CategoryItem> { _, _ -> 1 })
        whenever(repository?.selectedDepictions).thenReturn(depictedItems)
        whenever(repository?.searchAllEntities(ArgumentMatchers.anyString())).thenReturn(Observable.empty())
        depictsPresenter?.onDepictItemClicked(depictedItem)
        val depictedItem2 = DepictedItem("label2", "desc2", "", false, "entityid2")
        depictsPresenter?.onDepictItemClicked(depictedItem2)
        depictsPresenter?.verifyDepictions()
        verify(view)?.goToNextScreen()
    }
}
