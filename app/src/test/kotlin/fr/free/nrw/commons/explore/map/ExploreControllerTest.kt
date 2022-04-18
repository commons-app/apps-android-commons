package fr.free.nrw.commons.explore.map

import android.content.Context
import android.provider.MediaStore
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.*
import fr.free.nrw.commons.utils.PlaceUtils
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.powermock.api.mockito.PowerMockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.*

@RunWith(RobolectricTestRunner::class)
class ExploreControllerTest {

    @Mock
    private lateinit var searchLatLong: LatLng

    @Mock
    private lateinit var currentLatLng: LatLng

    @Mock
    private lateinit var exploreMapCalls: ExploreMapCalls

    private lateinit var context: Context
    private lateinit var exploreMapController: ExploreMapController

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        exploreMapController = ExploreMapController(exploreMapCalls)
        context = RuntimeEnvironment.application.applicationContext
    }

    @Test
    fun testLoadAttractionsForLocationTest() {
        Mockito.`when`(exploreMapCalls.callCommonsQuery(any()))
            .thenReturn(Collections.emptyList())
        exploreMapController.loadAttractionsFromLocation(
            searchLatLong,
            currentLatLng,
            false
        )
        Mockito.verify(exploreMapCalls).callCommonsQuery(
            eq(currentLatLng)
        )
    }

    @Test
    fun testLoadAttractionsForLocationSelectedLatLngNull() {
        Assertions.assertEquals(
            exploreMapController.loadAttractionsFromLocation(
                currentLatLng,
                null,
                any()
            ), null
        )
    }

    @Test
    fun testLoadAttractionsFromLocationCase1() {
        val place1 = Place(
            "en",
            "placeName",
            Label.FOREST,
            "placeDescription",
            LatLng(0.0, 0.0, 1.0F),
            "placeCategory",
            Sitelinks.Builder().build(),
            "picName",
            false
        )
        val place2 = Place(
            "en",
            "placeName",
            Label.FOREST,
            "placeDescription",
            LatLng(-40.69, -74.04, 1.0F),
            "placeCategory",
            Sitelinks.Builder().build(),
            "picName",
            false
        )
        // TODO: stattiği mockla
        PowerMockito.mock(Class<UUID>)
        Mockito.`when`(UUID.randomUUID().toString()).thenReturn("")
        val media1 = Media(any(), any(), any(), any(), any())
        val media2 = Media(any(), any(), any(), any(), any())
        Mockito.`when`(
            exploreMapCalls.callCommonsQuery(
                searchLatLong
            )
        ).thenReturn(mutableListOf(media1, media2))
        Mockito.`when`(
            PlaceUtils.mediaToExplorePlace(mutableListOf(media1, media2))
        ).thenReturn(mutableListOf(place1,place2))
        val result = exploreMapController.loadAttractionsFromLocation(
            currentLatLng,
            searchLatLong,
            false
        )
        Assertions.assertEquals(result.curLatLng, currentLatLng)
        Assertions.assertEquals(result.searchLatLng, searchLatLong)
        Assertions.assertEquals(result.mediaList, mutableListOf(media1, media2))
        Assertions.assertEquals(result.explorePlaceList, mutableListOf(place1, place2))
    }
}