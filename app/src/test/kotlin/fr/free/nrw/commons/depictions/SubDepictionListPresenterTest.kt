package fr.free.nrw.commons.depictions

import fr.free.nrw.commons.depictions.subClass.SubDepictionListContract
import fr.free.nrw.commons.depictions.subClass.SubDepictionListPresenter
import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class SubDepictionListPresenterTest {

    @Mock
    internal lateinit var view: SubDepictionListContract.View

    lateinit var subDepictionListPresenter: SubDepictionListPresenter

    lateinit var testScheduler: TestScheduler

    @Mock
    internal lateinit var recentSearchesDao: RecentSearchesDao

    @Mock
    internal lateinit var depictsClient: DepictsClient

    @Mock
    internal lateinit var okHttpJsonApiClient: OkHttpJsonApiClient

    var testObservable: Observable<List<DepictedItem>>? = null

    @Mock
    lateinit var depictedItem: DepictedItem

    val depictedItems: ArrayList<DepictedItem> = ArrayList()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        testScheduler = TestScheduler()
        depictedItems.add(depictedItem)
        testObservable = Observable.just(depictedItems)
        subDepictionListPresenter = SubDepictionListPresenter(recentSearchesDao, depictsClient, okHttpJsonApiClient, testScheduler, testScheduler)
        subDepictionListPresenter.onAttachView(view)
    }

    @Test
    fun initSubDepictionListForParentClass() {
        Mockito.`when`(okHttpJsonApiClient.getParentQIDs(ArgumentMatchers.anyString())).thenReturn(testObservable)
        subDepictionListPresenter.initSubDepictionList("Q9394", true)
        testScheduler.triggerActions()
        verify(view)?.onSuccess(depictedItems)
    }

    @Test
    fun initSubDepictionListForChildClass() {
        Mockito.`when`(okHttpJsonApiClient.getChildQIDs(ArgumentMatchers.anyString())).thenReturn(testObservable)
        subDepictionListPresenter.initSubDepictionList("Q9394", false)
        testScheduler.triggerActions()
        verify(view)?.onSuccess(depictedItems)
    }
}
