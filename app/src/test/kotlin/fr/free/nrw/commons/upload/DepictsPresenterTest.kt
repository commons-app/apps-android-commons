package fr.free.nrw.commons.upload

import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.depicts.DepictsContract
import fr.free.nrw.commons.upload.depicts.DepictsPresenter
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.mockito.Mock
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
        depictsPresenter = DepictsPresenter(repository)
        depictsPresenter?.onAttachView(view)
    }


}