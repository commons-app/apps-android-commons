package fr.free.nrw.commons.search

import com.nhaarman.mockito_kotlin.verify
import com.pedrogomez.renderers.RVRendererAdapter
import fr.free.nrw.commons.explore.SearchActivity
import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.explore.depictions.SearchDepictionsFragment
import fr.free.nrw.commons.upload.UploadService
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.*
import org.mockito.Mockito.times
import org.mockito.Mockito.verifyZeroInteractions

class SearchTest {
    @Mock
    internal  var searchActivity : SearchActivity? = null

    @Mock
    internal var searchDepictionsFragment : SearchDepictionsFragment? = null

    @Mock
    internal var depictsClient: DepictsClient? = null

    internal var depictionsAdapter : RVRendererAdapter<DepictedItem>? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        searchActivity?.setTabs()
    }

    @Test
    fun searchDepictsEntity() {
        //initiates a call to updateDepictionList and checks whether it proceeds  to further calls
        Mockito.`when`(searchDepictionsFragment?.updateDepictionList("rabbit")).thenCallRealMethod()

        val depictedItem = DepictedItem("label2", "desc2", null, false, "entityid2")
        //checks whether the searchForDepictions method is called with the same searched item and it returs a non empty observable of depicted item
        Mockito.`when`(depictsClient?.searchForDepictions("rabbit", 25, 0))?.thenReturn(Observable.just(depictedItem))?.then{
            verify(searchDepictionsFragment, times(1))?.handleSuccess(ArgumentMatchers.anyList())
        }
    }
}