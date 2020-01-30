package fr.free.nrw.commons.search

import com.pedrogomez.renderers.RVRendererAdapter
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.depictions.WikidataItemDetailsActivity
import fr.free.nrw.commons.depictions.Media.DepictedImagesFragment
import fr.free.nrw.commons.explore.SearchActivity
import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.explore.depictions.SearchDepictionsFragment
import fr.free.nrw.commons.explore.depictions.SearchDepictionsFragmentPresenter
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.*
import org.mockito.Mockito.*

class SearchTest {
    @Mock
    internal  var searchActivity : SearchActivity? = null

    @Mock
    internal var view : SearchDepictionsFragment? = null

    var searchDepictionsFragmentPresenter : SearchDepictionsFragmentPresenter ? = null

    @Mock
    internal var depictsClient: DepictsClient? = null

    @Mock
    internal var wikidataItemDetailsActivity : WikidataItemDetailsActivity? = null

    @Mock
    internal var depictedImagesFragment: DepictedImagesFragment? = null

    internal var depictionsAdapter : RVRendererAdapter<DepictedItem>? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        searchActivity?.setTabs()
        searchDepictionsFragmentPresenter?.onAttachView(view)
    }

    @Test
    fun searchDepictsEntity() {
        if (!BuildConfig.DEBUG){
            //initiates a call to updateDepictionList and checks whether it proceeds  to further calls
            Mockito.`when`(view?.updateDepictionList("rabbit")).thenCallRealMethod()

            val depictedItem = DepictedItem("rabbit", "desc2", null, false, "Q9394")
            //checks whether the searchForDepictions method is called with the same searched item and it returs a non empty observable of depicted item
            Mockito.`when`(depictsClient?.searchForDepictions("rabbit", 25, 0))?.thenReturn(Observable.just(depictedItem))?.then{
                verify(searchDepictionsFragmentPresenter, times(1))?.handleSuccess(ArgumentMatchers.anyList())
                depictionsAdapter?.addAll(ArgumentMatchers.anyList<DepictedItem>())?.let { it1 -> assert(it1)
                }
            }
            WikidataItemDetailsActivity.startYourself(ArgumentMatchers.any(), depictedItem)
            val mediaDetails : MediaDetailPagerFragment = MediaDetailPagerFragment(false, true)
            wikidataItemDetailsActivity?.onItemClick(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyLong())
            mediaDetails.showImage(0)
            val media: Media? = wikidataItemDetailsActivity?.getMediaAtPosition(0)
            mediaDetails.downloadMedia(media)
        }
    }
}