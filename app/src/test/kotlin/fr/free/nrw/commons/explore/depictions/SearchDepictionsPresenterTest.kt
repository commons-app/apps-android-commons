package fr.free.nrw.commons.explore.depictions

import org.mockito.Mockito.verify
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class SearchDepictionsPresenterTest {

    @Mock
    internal var view: SearchDepictionsFragmentContract.View? = null

    var searchDepictionsFragmentPresenter: SearchDepictionsFragmentPresenter? = null

    var testScheduler: TestScheduler? = null

    var jsonKvStore: JsonKvStore? = null

    //var mediaWikiApi: MediaWikiApi? = null

    @Mock
    var recentSearchesDao: RecentSearchesDao? = null

    @Mock
    var depictsClient: DepictsClient? = null

    var testObservable: Observable<DepictedItem>? = null

    var mediaList: ArrayList<DepictedItem> = ArrayList()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        testScheduler = TestScheduler()
        val depictedItem: DepictedItem = DepictedItem("label", "description", "url", false, "Q9394")
        mediaList.add(depictedItem)
        testObservable = Observable.just(depictedItem)
        searchDepictionsFragmentPresenter = SearchDepictionsFragmentPresenter(jsonKvStore, recentSearchesDao, depictsClient, testScheduler, testScheduler)
        searchDepictionsFragmentPresenter?.onAttachView(view)
    }

    @Test
    fun updateDepictionList() {
        Mockito.`when`(depictsClient?.searchForDepictions(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenReturn(testObservable)
        searchDepictionsFragmentPresenter?.updateDepictionList("rabbit", 25, false)
        testScheduler?.triggerActions()
        verify(view)?.onSuccess(mediaList)
    }

    @Test
    fun fetchThumbnailForEntityId() {
        val singleString: Single<String> = Single.just(String())
        Mockito.`when`(depictsClient?.getP18ForItem(ArgumentMatchers.anyString())).thenReturn(singleString)
        searchDepictionsFragmentPresenter?.fetchThumbnailForEntityId("Q9394", 0)
        testScheduler?.triggerActions()
        verify(view)?.onImageUrlFetched("", 0)
    }
}