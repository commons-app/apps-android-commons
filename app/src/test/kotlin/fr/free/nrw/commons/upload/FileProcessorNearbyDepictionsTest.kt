package fr.free.nrw.commons.upload

import androidx.exifinterface.media.ExifInterface
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.mwapi.CategoryApi
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.upload.structure.depictions.DepictModel
import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Verifies that pictures of one upload taken at the same place search nearby depictions once. */
class FileProcessorNearbyDepictionsTest {
    private val apiCall = mock<CategoryApi>()
    private val okHttpJsonApiClient = mock<OkHttpJsonApiClient>()
    private val nearbyPlaces = BehaviorProcessor.createDefault(emptyList<Place>())
    private val depictModel = mock<DepictModel>()
    private val fileProcessor =
        FileProcessor(
            mock(),
            mock(),
            mock(),
            FileProcessorDependencies(GpsCategoryModel(), depictModel, apiCall, okHttpJsonApiClient),
        )
    private val fivePlaces = List(5) { mock<Place>() }
    private val clock = TestScheduler()

    @Before
    fun setUp() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxJavaPlugins.setComputationSchedulerHandler { clock }
        whenever(apiCall.request(any())).thenReturn(Single.just(emptyList()))
        whenever(depictModel.nearbyPlaces).thenReturn(nearbyPlaces)
    }

    @After
    fun tearDown() = RxJavaPlugins.reset()

    // north of Villa Borghese by about 1.1 m per step
    private fun picture(step: Int): ImageCoordinates {
        val exif = mock<ExifInterface>()
        whenever(exif.latLong).thenReturn(doubleArrayOf(41.9142 + step * 0.00001, 12.4923))
        return ImageCoordinates(exif, null)
    }

    private fun nearbySearch() =
        okHttpJsonApiClient.getNearbyPlaces(any<LatLng>(), any<String>(), any<Double>(), anyOrNull<String>())

    private fun verifySearches(count: Int) =
        verify(okHttpJsonApiClient, times(count))
            .getNearbyPlaces(any<LatLng>(), any<String>(), any<Double>(), anyOrNull<String>())

    @Test
    fun `pictures taken at a busy place search it once`() {
        whenever(nearbySearch()).thenReturn(fivePlaces)

        (0 until 20).forEach { fileProcessor.prePopulateCategoriesAndDepictionsBy(picture(it)) }

        verifySearches(1)
        assertEquals(fivePlaces, nearbyPlaces.value)
    }

    @Test
    fun `pictures taken at a quiet place try the 10 radii once`() {
        whenever(nearbySearch()).thenReturn(emptyList())

        (0 until 20).forEach { fileProcessor.prePopulateCategoriesAndDepictionsBy(picture(it)) }

        verifySearches(10)
    }

    @Test
    fun `a search that fails while the other pictures wait for it is tried again`() {
        whenever(nearbySearch()).thenThrow(IOException("Too Many Requests")).thenReturn(fivePlaces)
        // the search only runs when the clock moves, after all pictures were processed
        RxJavaPlugins.setIoSchedulerHandler { clock }

        (0 until 20).forEach { fileProcessor.prePopulateCategoriesAndDepictionsBy(picture(it)) }
        clock.advanceTimeBy(5, TimeUnit.SECONDS)

        verifySearches(2)
        assertEquals(fivePlaces, nearbyPlaces.value)
    }

    @Test
    fun `a search that keeps failing lets the next picture of that place try again`() {
        val offline = IOException("offline")
        whenever(nearbySearch()).thenThrow(offline, offline, offline).thenReturn(fivePlaces)

        fileProcessor.prePopulateCategoriesAndDepictionsBy(picture(0))
        clock.advanceTimeBy(15, TimeUnit.SECONDS)
        fileProcessor.prePopulateCategoriesAndDepictionsBy(picture(1))

        verifySearches(4)
        assertEquals(fivePlaces, nearbyPlaces.value)
    }

    @Test
    fun `a picture more than 50 m away is searched`() {
        whenever(nearbySearch()).thenReturn(fivePlaces)

        fileProcessor.prePopulateCategoriesAndDepictionsBy(picture(0))
        fileProcessor.prePopulateCategoriesAndDepictionsBy(picture(100))

        verifySearches(2)
    }

    @Test
    fun `a new upload searches the same place again`() {
        whenever(nearbySearch()).thenReturn(fivePlaces)

        fileProcessor.prePopulateCategoriesAndDepictionsBy(picture(0))
        fileProcessor.cleanup()
        fileProcessor.prePopulateCategoriesAndDepictionsBy(picture(0))

        verifySearches(2)
    }
}
