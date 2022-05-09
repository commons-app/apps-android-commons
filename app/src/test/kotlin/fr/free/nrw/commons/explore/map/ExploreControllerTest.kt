package fr.free.nrw.commons.explore.map

import android.content.Context
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.spy
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.MapController
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.Utils
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.*
import fr.free.nrw.commons.utils.PlaceUtils
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import java.util.*

@RunWith(RobolectricTestRunner::class)
class ExploreControllerTest {

    @Mock
    private lateinit var searchLatLng: LatLng

    @Mock
    private lateinit var currentLatLng: LatLng

    @Mock
    private lateinit var exploreMapCalls: ExploreMapCalls

    @Mock
    private lateinit var callback: ExploreMapController.NearbyBaseMarkerThumbCallback

    private lateinit var context: Context
    private lateinit var exploreMapController: ExploreMapController
    @Mock
    private lateinit var exploreMapControllerSpy: ExploreMapController

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
                any()
            ), null
        )
    }

    @Test
    fun testLoadAttracomputeDistanceBetweenctionsFromLocationCaseTwoPlacesAdded() {
        val place1 = Place(
            "en",
            "placeName1",
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
            "placeName2",
            Label.FOREST,
            "placeDescription",
            LatLng(-40.69, -74.04, 1.0F),
            "placeCategory",
            Sitelinks.Builder().build(),
            "picName",
            false
        )
        val defaultUuid = UUID.fromString("8d8b30e3-de52-4f1c-a71c-9905a8043dac")
        Mockito.mockStatic(UUID::class.java).use { mockedUuid ->
            mockedUuid.`when`<Any> { UUID.randomUUID() }.thenReturn(defaultUuid)
        }

        val pageTitle = spy(PageTitle("title", WikiSite(BuildConfig.COMMONS_URL)))
        Mockito.mockStatic(Utils::class.java).use { mockedPageTitle ->
            mockedPageTitle.`when`<Any> { Utils.getPageTitle(any()) }.thenReturn(pageTitle)
        }
        Mockito.`when`(pageTitle.getCanonicalUri()).thenReturn("canonicalUri")

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

        Mockito.mockStatic(PlaceUtils::class.java).use { mockedMediaList ->
            mockedMediaList.`when`<Any> { PlaceUtils.mediaToExplorePlace(mutableListOf(media1, media2)) }
                .thenReturn(mutableListOf(place1,place2))
        }
        val result = exploreMapController.loadAttractionsFromLocation(
                currentLatLng,
                searchLatLng,
                false
            )
        Assertions.assertEquals(result.curLatLng, currentLatLng)
        Assertions.assertEquals(result.searchLatLng, searchLatLng)
        Assertions.assertEquals(result.mediaList, mutableListOf(media1, media2))
        Assertions.assertEquals(mutableListOf(result.explorePlaceList[0].name, result.explorePlaceList[1].name), mutableListOf(place1.name, place2.name))
    }

    @Test
    fun testLoadAttractionsFromLocationToBaseMarkerOptionsNullPlacelist() {
        val result = exploreMapController.loadAttractionsFromLocationToBaseMarkerOptions(
            any(),
            null,
            context,
            any(),
            any(),
            any(),
            any()
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