package fr.free.nrw.commons.upload

import androidx.exifinterface.media.ExifInterface
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.mwapi.CategoryApi
import fr.free.nrw.commons.upload.structure.depictions.DepictModel
import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Verifies that pictures of one upload taken at the same place look up location categories once. */
class FileProcessorLocationTest {
    private val apiCall = mock<CategoryApi>()
    private val fileProcessor =
        FileProcessor(
            mock(),
            mock(),
            mock(),
            FileProcessorDependencies(GpsCategoryModel(), mock<DepictModel>(), apiCall, mock()),
        )

    @Before
    fun setUp() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        whenever(apiCall.request(any())).thenReturn(Single.just(emptyList()))
    }

    @After
    fun tearDown() = RxJavaPlugins.reset()

    // north of Villa Borghese by about 1.1 m per step
    private fun picture(step: Int): ImageCoordinates {
        val exif = mock<ExifInterface>()
        whenever(exif.latLong).thenReturn(doubleArrayOf(41.9142 + step * 0.00001, 12.4923))
        return ImageCoordinates(exif, null)
    }

    @Test
    fun `pictures taken at the same place look it up once`() {
        (0 until 20).forEach { fileProcessor.prePopulateCategoriesAndDepictionsBy(picture(it)) }

        verify(apiCall, times(1)).request(any())
    }

    @Test
    fun `a picture more than 50 m away is looked up`() {
        fileProcessor.prePopulateCategoriesAndDepictionsBy(picture(0))
        fileProcessor.prePopulateCategoriesAndDepictionsBy(picture(100))

        verify(apiCall, times(2)).request(any())
    }

    @Test
    fun `a new upload looks up the same place again`() {
        fileProcessor.prePopulateCategoriesAndDepictionsBy(picture(0))
        fileProcessor.cleanup()
        fileProcessor.prePopulateCategoriesAndDepictionsBy(picture(0))

        verify(apiCall, times(2)).request(any())
    }

    @Test
    fun `a failed lookup lets the next picture of that place try again`() {
        whenever(apiCall.request(any())).thenReturn(Single.error(Exception("offline")), Single.just(emptyList()))

        fileProcessor.prePopulateCategoriesAndDepictionsBy(picture(0))
        fileProcessor.prePopulateCategoriesAndDepictionsBy(picture(1))

        verify(apiCall, times(2)).request(any())
    }
}
