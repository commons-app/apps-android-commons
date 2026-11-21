package fr.free.nrw.commons.nearby.fragments

import android.app.Activity
import android.content.Intent
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.NearbyController
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.io.File

/**
 * Unit tests for [NearbyFileExportHelper].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NearbyFileExportHelperTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var helper: NearbyFileExportHelper
    private lateinit var activity: Activity
    private lateinit var nearbyController: NearbyController
    private lateinit var compositeDisposable: CompositeDisposable
    private lateinit var viewCallback: NearbyFileExportHelper.ExportViewCallback
    private lateinit var downloadsDir: File

    private val testLatLng = LatLng(12.34, 56.78, 1.0f)

    @Before
    fun setUp() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline() }

        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        activity.setTheme(R.style.LightAppTheme)

        downloadsDir = temporaryFolder.newFolder("downloads")

        nearbyController = mock()
        compositeDisposable = CompositeDisposable()
        viewCallback = mock()

        helper = NearbyFileExportHelper()
        helper.init(
            activityProvider = { activity },
            nearbyControllerProvider = { nearbyController },
            compositeDisposableProvider = { compositeDisposable },
            mapFocusProvider = { testLatLng },
            viewCallback = viewCallback,
        )
        helper.downloadsDirProvider = { downloadsDir }
        helper.isStorageWritableProvider = { true }
    }

    @After
    fun tearDown() {
        compositeDisposable.dispose()
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun testSavePlacesAsKmlSuccess() {
        whenever(nearbyController.getPlacesAsKML(testLatLng)).thenReturn("<kml>test</kml>")

        helper.savePlacesAsKML(testLatLng)

        verify(viewCallback).hideProgressDialog()
        val files = downloadsDir.listFiles()
        assertNotNull(files)
        assertTrue(files!!.any { it.name.startsWith("KML_") && it.name.endsWith(".kml") })
    }

    @Test
    fun testSavePlacesAsKmlNullResponse() {
        whenever(nearbyController.getPlacesAsKML(testLatLng)).thenReturn(null)

        helper.savePlacesAsKML(testLatLng)

        verify(viewCallback, never()).hideProgressDialog()
    }

    @Test
    fun testSavePlacesAsKmlError() {
        whenever(nearbyController.getPlacesAsKML(testLatLng)).thenThrow(RuntimeException("Network failure"))

        helper.savePlacesAsKML(testLatLng)

        verify(viewCallback).showErrorMessage(any())
        verify(viewCallback).setProgressBarVisibility(false)
        verify(viewCallback).lockUnlockNearby(false)
        verify(viewCallback).setFilterState()
    }

    @Test
    fun testSavePlacesAsKmlSaveFileFailureShowsToast() {
        helper.isStorageWritableProvider = { false }
        whenever(nearbyController.getPlacesAsKML(testLatLng)).thenReturn("<kml>test</kml>")

        helper.savePlacesAsKML(testLatLng)

        verify(viewCallback).hideProgressDialog()
        verify(viewCallback).showToast(R.string.failed_to_save_kml_file)
    }

    @Test
    fun testSavePlacesAsGpxSuccess() {
        whenever(nearbyController.getPlacesAsGPX(testLatLng)).thenReturn("<gpx>test</gpx>")

        helper.savePlacesAsGPX(testLatLng)

        verify(viewCallback).hideProgressDialog()
        val files = downloadsDir.listFiles()
        assertNotNull(files)
        assertTrue(files!!.any { it.name.startsWith("GPX_") && it.name.endsWith(".gpx") })
    }

    @Test
    fun testSavePlacesAsGpxNullResponse() {
        whenever(nearbyController.getPlacesAsGPX(testLatLng)).thenReturn(null)

        helper.savePlacesAsGPX(testLatLng)

        verify(viewCallback, never()).hideProgressDialog()
    }

    @Test
    fun testSavePlacesAsGpxError() {
        whenever(nearbyController.getPlacesAsGPX(testLatLng)).thenThrow(RuntimeException("Server error"))

        helper.savePlacesAsGPX(testLatLng)

        verify(viewCallback).showErrorMessage(any())
        verify(viewCallback).setProgressBarVisibility(false)
        verify(viewCallback).lockUnlockNearby(false)
        verify(viewCallback).setFilterState()
    }

    @Test
    fun testSavePlacesAsGpxSaveFileFailureShowsToast() {
        helper.isStorageWritableProvider = { false }
        whenever(nearbyController.getPlacesAsGPX(testLatLng)).thenReturn("<gpx>test</gpx>")

        helper.savePlacesAsGPX(testLatLng)

        verify(viewCallback).hideProgressDialog()
        verify(viewCallback).showToast(R.string.failed_to_save_gpx_file)
    }

    @Test
    fun testSavePlacesDelegationWithoutParams() {
        whenever(nearbyController.getPlacesAsKML(testLatLng)).thenReturn("<kml>delegated</kml>")

        helper.savePlacesAsKML()

        verify(viewCallback).hideProgressDialog()
    }

    @Test
    fun testSavePlacesAsGpxDelegationWithoutParams() {
        whenever(nearbyController.getPlacesAsGPX(testLatLng)).thenReturn("<gpx>delegated</gpx>")

        helper.savePlacesAsGPX()

        verify(viewCallback).hideProgressDialog()
    }

    @Test
    fun testSaveFileSuccess() {
        val fileName = "sample.kml"
        val content = "<kml>sample content</kml>"

        val result = helper.saveFile(content, fileName)

        assertTrue(result)
        val file = File(downloadsDir, fileName)
        assertTrue(file.exists())
        assertEquals(content, file.readText())
    }

    @Test
    fun testSaveFileStorageNotWritable() {
        helper.isStorageWritableProvider = { false }

        val result = helper.saveFile("content", "test.kml")

        assertFalse(result)
    }

    @Test
    fun testSaveFileNullFileName() {
        val result = helper.saveFile("content", null)

        assertFalse(result)
    }

    @Test
    fun testShowOpenFileDialogKml() {
        helper.showOpenFileDialog(activity, "test.kml", false)
    }

    @Test
    fun testShowOpenFileDialogGpx() {
        helper.showOpenFileDialog(activity, "test.gpx", true)
    }

    @Test
    fun testOpenFileLaunchesIntent() {
        val testFile = File(downloadsDir, "test.gpx")
        testFile.writeText("<gpx>data</gpx>")

        helper.openFile(activity, "test.gpx", true)

        val shadowActivity = Shadows.shadowOf(activity)
        val intent = shadowActivity.nextStartedActivity
        assertNotNull(intent)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("application/gpx", intent.type)
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
