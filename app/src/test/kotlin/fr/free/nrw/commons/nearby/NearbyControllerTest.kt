package fr.free.nrw.commons.nearby

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.NearbyController.loadAttractionsFromLocationToBaseMarkerOptions
import fr.free.nrw.commons.nearby.NearbyController.updateMarkerLabelListBookmark
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NearbyControllerTest {

    @Mock
    private lateinit var place: Place

    @Mock
    private lateinit var nearbyPlaces: NearbyPlaces

    @Mock
    private lateinit var searchLatLong: LatLng

    @Mock
    private lateinit var currentLatLng: LatLng


    private lateinit var context: Context
    private var customQuery: String = "test"
    private lateinit var nearbyController: NearbyController
    private lateinit var markerPlaceGroup: MarkerPlaceGroup

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        nearbyController = NearbyController(nearbyPlaces)
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testLoadAttractionsForLocationTest() {
        `when`(nearbyPlaces.radiusExpander(any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList())
        nearbyController.loadAttractionsFromLocation(
            searchLatLong,
            currentLatLng,
            false,
            false,
            true,
            customQuery
        )
        Mockito.verify(nearbyPlaces).radiusExpander(
            eq(currentLatLng),
            any(String::class.java),
            eq(false),
            eq(true),
            eq(customQuery)
        )
    }

    @Test
    fun testLoadAttractionsForLocationTestNoQuery() {
        `when`(nearbyPlaces.radiusExpander(any(), any(), any(), any(), anyOrNull()))
            .thenReturn(Collections.emptyList())
        nearbyController.loadAttractionsFromLocation(
            searchLatLong,
            currentLatLng,
            false,
            false,
            true
        )
        Mockito.verify(nearbyPlaces).radiusExpander(
            eq(currentLatLng),
            any(String::class.java),
            eq(false),
            eq(true),
            eq(null)
        )
    }

    @Test
    fun testLoadAttractionsFromLocationCaseNull() {
        assertEquals(
            nearbyController.loadAttractionsFromLocation(
                currentLatLng,
                null,
                false,
                false,
                false,
                customQuery
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
        `when`(
            nearbyPlaces.radiusExpander(
                searchLatLong, Locale.getDefault().language, false,
                false, customQuery
            )
        ).thenReturn(mutableListOf(place1, place2))
        val result = nearbyController.loadAttractionsFromLocation(
            currentLatLng,
            searchLatLong,
            false,
            true,
            false,
            customQuery
        )
        assertEquals(result.curLatLng, currentLatLng)
        assertEquals(result.searchLatLng, searchLatLong)
    }

    @Test
    fun testLoadAttractionsFromLocationCase2() {
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
            LatLng(40.69, -74.04, 1.0F),
            "placeCategory",
            Sitelinks.Builder().build(),
            "picName",
            false
        )
        `when`(
            nearbyPlaces.radiusExpander(
                searchLatLong, Locale.getDefault().language, false,
                false, customQuery
            )
        ).thenReturn(mutableListOf(place1, place2))
        val result = nearbyController.loadAttractionsFromLocation(
            currentLatLng,
            searchLatLong,
            false,
            true,
            false,
            customQuery
        )
        assertEquals(result.curLatLng, currentLatLng)
        assertEquals(result.searchLatLng, searchLatLong)
    }

    @Test
    fun testLoadAttractionsFromLocationCase3() {
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
            LatLng(40.69, 74.04, 1.0F),
            "placeCategory",
            Sitelinks.Builder().build(),
            "picName",
            false
        )
        `when`(
            nearbyPlaces.radiusExpander(
                searchLatLong, Locale.getDefault().language, false,
                false, customQuery
            )
        ).thenReturn(mutableListOf(place1, place2))
        val result = nearbyController.loadAttractionsFromLocation(
            currentLatLng,
            searchLatLong,
            false,
            true,
            false,
            customQuery
        )
        assertEquals(result.curLatLng, currentLatLng)
        assertEquals(result.searchLatLng, searchLatLong)
    }

    @Test
    fun testLoadAttractionsFromLocationToBaseMarkerOptionsCaseNull() {
        assertEquals(
            loadAttractionsFromLocationToBaseMarkerOptions(
                currentLatLng,
                null,
                context,
                mutableListOf(place)
            ), listOf<NearbyBaseMarker>()
        )
    }

    @Test
    fun testLoadAttractionsFromLocationToBaseMarkerOptionsCaseIsMonument() {
        place = Place(
            "en",
            "placeName",
            Label.FOREST,
            "placeDescription",
            currentLatLng,
            "placeCategory",
            Sitelinks.Builder().build(),
            "picName",
            false
        )
        place.isMonument = true
        `when`(currentLatLng.latitude).thenReturn(0.0)
        `when`(currentLatLng.longitude).thenReturn(0.0)
        assertEquals(
            loadAttractionsFromLocationToBaseMarkerOptions(
                currentLatLng,
                listOf(place),
                context,
                mutableListOf(place)
            )[0].place, place
        )
    }

    @Test
    fun testLoadAttractionsFromLocationToBaseMarkerOptionsCaseIsNotMonumentPicNotEmpty() {
        place = Place(
            "en",
            "placeName",
            Label.FOREST,
            "placeDescription",
            currentLatLng,
            "placeCategory",
            Sitelinks.Builder().build(),
            "picName",
            false
        )
        place.isMonument = false
        `when`(currentLatLng.latitude).thenReturn(0.0)
        `when`(currentLatLng.longitude).thenReturn(0.0)
        assertEquals(
            loadAttractionsFromLocationToBaseMarkerOptions(
                currentLatLng,
                listOf(place),
                context,
                mutableListOf(place)
            )[0].place, place
        )
    }

    @Test
    fun testLoadAttractionsFromLocationToBaseMarkerOptionsCaseIsNotMonumentPicEmptyPlaceDoesNotExists() {
        place = Place(
            "en",
            "placeName",
            Label.FOREST,
            "placeDescription",
            currentLatLng,
            "placeCategory",
            Sitelinks.Builder().build(),
            "",
            false
        )
        place.isMonument = false
        `when`(currentLatLng.latitude).thenReturn(0.0)
        `when`(currentLatLng.longitude).thenReturn(0.0)
        assertEquals(
            loadAttractionsFromLocationToBaseMarkerOptions(
                currentLatLng,
                listOf(place),
                context,
                mutableListOf(place)
            )[0].place, place
        )
    }

    @Test
    fun testLoadAttractionsFromLocationToBaseMarkerOptionsCaseIsNotMonumentPicEmptyPlaceExists() {
        place = Place(
            "en",
            "placeName",
            Label.FOREST,
            "placeDescription",
            currentLatLng,
            "placeCategory",
            Sitelinks.Builder().build(),
            "",
            true
        )
        place.isMonument = false
        `when`(currentLatLng.latitude).thenReturn(0.0)
        `when`(currentLatLng.longitude).thenReturn(0.0)
        assertEquals(
            loadAttractionsFromLocationToBaseMarkerOptions(
                currentLatLng,
                listOf(place),
                context,
                mutableListOf(place)
            )[0].place, place
        )
    }

    @Test
    fun testUpdateMarkerLabelListBookmarkCaseTrue() {
        markerPlaceGroup = MarkerPlaceGroup(null, true, place)
        `when`(place.wikiDataEntityId).thenReturn("someString")
        val list = mutableListOf(markerPlaceGroup)
        Whitebox.setInternalState(
            NearbyController::class.java,
            "markerLabelList",
            list
        )
        updateMarkerLabelListBookmark(place, false)
        assertEquals(list[0].isBookmarked, false)
        assertEquals(list[0].place, place)
        assertEquals(list[0].marker, null)
    }

    @Test
    fun testUpdateMarkerLabelListBookmarkCaseFalse() {
        markerPlaceGroup = MarkerPlaceGroup(null, false, place)
        `when`(place.wikiDataEntityId).thenReturn("someString")
        val list = mutableListOf(markerPlaceGroup)
        Whitebox.setInternalState(
            NearbyController::class.java,
            "markerLabelList",
            list
        )
        updateMarkerLabelListBookmark(place, true)
        assertEquals(list[0].isBookmarked, true)
        assertEquals(list[0].place, place)
        assertEquals(list[0].marker, null)
    }

    fun <T> any(type: Class<T>): T = Mockito.any(type)
}