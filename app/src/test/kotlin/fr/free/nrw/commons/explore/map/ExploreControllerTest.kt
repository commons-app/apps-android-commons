package fr.free.nrw.commons.explore.map

import android.content.Context
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.*
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class ExploreControllerTest {

    @Mock
    private lateinit var searchLatLng: LatLng

    @Mock
    private lateinit var currentLatLng: LatLng

    @Mock
    private lateinit var place1: Place

    @Mock
    private lateinit var place2: Place

    @Mock
    private lateinit var exploreMapCalls: ExploreMapCalls

    @Mock
    private lateinit var callback: ExploreMapController.NearbyBaseMarkerThumbCallback

    private lateinit var context: Context
    private lateinit var exploreMapController: ExploreMapController

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        exploreMapController = ExploreMapController(exploreMapCalls)
        context = RuntimeEnvironment.application.applicationContext
    }

    @Test
    fun testLoadAttractionsForLocationCommonsQueryIsCalled() {
        exploreMapController.loadAttractionsFromLocation(
            searchLatLng,
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
                false
            ), null
        )
    }

    @Test
    fun testLoadAttracomputeDistanceBetweenctionsFromLocationCaseTwoPlacesAdded() {
        whenever(place1.getName()).thenReturn("placeName1")
        whenever(place2.getName()).thenReturn("placeName2")
        val media1 = Media()
        val media2 = Media()
        media1.categories = listOf("testCategory")
        media2.categories = listOf("testCategory")
        media1.filename = "placeName1"
        media2.filename = "placeName2"
        media1.coordinates = LatLng(0.0, 0.0, 1.0F)
        media2.coordinates = LatLng(-40.69, -74.04, 1.0F)

        Mockito.`when`(
            exploreMapCalls.callCommonsQuery(
                searchLatLng
            )
        ).thenReturn(mutableListOf(media1,media2))
        val result = exploreMapController.loadAttractionsFromLocation(
                currentLatLng,
                searchLatLng,
                false
            )
        Assertions.assertEquals(result.curLatLng, currentLatLng)
        Assertions.assertEquals(result.searchLatLng, searchLatLng)
        Assertions.assertEquals(result.mediaList, mutableListOf(media1, media2))
        Assertions.assertEquals(mutableListOf(result.placeList[0].getName(), result.placeList[1].getName()), mutableListOf(place1.getName(), place2.getName()))
    }

    @Test
    fun testLoadAttractionsFromLocationToBaseMarkerOptionsNullPlacelist() {
        val result = exploreMapController.loadAttractionsFromLocationToBaseMarkerOptions(
            currentLatLng,
            null,
            context,
            callback,
            null,
            false,
            null
        )
        Assertions.assertEquals(result, listOf<NearbyBaseMarker>())
    }

    @Test
    fun testLoadAttractionsFromLocationToBaseMarkerOptionsWithPlacelist() {
        val place1 = Place(
            "en",
            "File:placeName1.jpg",
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
            "File:placeName2.jpg",
            Label.FOREST,
            "placeDescription",
            LatLng(-40.69, -74.04, 1.0F),
            "placeCategory",
            Sitelinks.Builder().build(),
            "picName",
            false
        )
        val result = exploreMapController.loadAttractionsFromLocationToBaseMarkerOptions(
            any(),
            mutableListOf(place1, place2),
            context,
            callback,
            any(),
            any(),
            any()
        )
        Assertions.assertEquals(result.get(0).place, place1)
        Assertions.assertEquals(result.get(1).place, place2)
    }
}