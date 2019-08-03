package fr.free.nrw.commons.search

import android.os.Build
import android.widget.AdapterView
import com.nhaarman.mockito_kotlin.verify
import com.pedrogomez.renderers.RVRendererAdapter
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.depictions.DepictedImagesActivity
import fr.free.nrw.commons.explore.SearchActivity
import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.explore.depictions.SearchDepictionsFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.upload.UploadService
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.*
import org.mockito.Mockito.*

class SearchTest {
    @Mock
    internal  var searchActivity : SearchActivity? = null

    @Mock
    internal var searchDepictionsFragment : SearchDepictionsFragment? = null

    @Mock
    internal var depictsClient: DepictsClient? = null

    @Mock
    internal var depictedImagesActivity: DepictedImagesActivity? = null

    internal var depictionsAdapter : RVRendererAdapter<DepictedItem>? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        searchActivity?.setTabs()
    }

    @Test
    fun searchDepictsEntity() {
        if (!BuildConfig.DEBUG){
            //initiates a call to updateDepictionList and checks whether it proceeds  to further calls
            Mockito.`when`(searchDepictionsFragment?.updateDepictionList("rabbit")).thenCallRealMethod()

            val depictedItem = DepictedItem("rabbit", "desc2", null, false, "Q9394")
            //checks whether the searchForDepictions method is called with the same searched item and it returs a non empty observable of depicted item
            Mockito.`when`(depictsClient?.searchForDepictions("rabbit", 25, 0))?.thenReturn(Observable.just(depictedItem))?.then{
                verify(searchDepictionsFragment, times(1))?.handleSuccess(ArgumentMatchers.anyList())
                depictionsAdapter?.addAll(ArgumentMatchers.anyList<DepictedItem>())?.let { it1 -> assert(it1)
                }
            }
            DepictedImagesActivity.startYourself(ArgumentMatchers.any(), depictedItem)
            val mediaDetails : MediaDetailPagerFragment = MediaDetailPagerFragment(false, true)
            depictedImagesActivity?.onItemClick(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyLong())
            mediaDetails.showImage(0)
            val media: Media? = depictedImagesActivity?.getMediaAtPosition(0)
            mediaDetails.downloadMedia(media)
        }
    }
}