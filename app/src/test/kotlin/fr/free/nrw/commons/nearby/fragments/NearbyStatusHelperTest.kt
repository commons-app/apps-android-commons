package fr.free.nrw.commons.nearby.fragments

import android.app.Activity
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Unit tests for [NearbyStatusHelper].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NearbyStatusHelperTest {
    private lateinit var helper: NearbyStatusHelper
    private lateinit var activity: Activity
    private lateinit var presenter: NearbyParentFragmentPresenter
    private lateinit var locationManager: LocationServiceManager

    @Before
    fun setUp() {
        helper = NearbyStatusHelper()
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        presenter = mock()
        locationManager = mock()

        helper.init(
            NearbyStatusDependencies(
                activityProvider = { activity },
                contextProvider = { activity },
                bindingProvider = { null },
                presenterProvider = { presenter },
                locationManagerProvider = { locationManager },
            ),
        )
    }

    @Test
    fun testInitializationRegistersBroadcastReceiver() {
        assertNotNull(helper.broadcastReceiver)
        assertNull(helper.snackbar)
    }

    @Test
    fun testOnWikidataEditSuccessfulCallsPresenter() {
        helper.onWikidataEditSuccessful()

        verify(presenter).updateMapAndList(LocationChangeType.MAP_UPDATED)
    }

    @Test
    fun testBackButtonClickedReturnsFalseWhenBindingIsNull() {
        org.junit.Assert.assertFalse(helper.backButtonClicked())
    }

    @Test
    fun testBackButtonClickedDelegatesToMediaDetailHelperWhenHandled() {
        val mockMediaHelper = mock<NearbyMediaDetailHelper>()
        whenever(mockMediaHelper.handleBackPress()).thenReturn(true)
        helper.mediaDetailHelperProvider = { mockMediaHelper }

        org.junit.Assert.assertTrue(helper.backButtonClicked())
    }

    @Test
    fun testBackButtonClickedDelegatesToPresenterWhenMediaDetailsNotHandled() {
        val mockMediaHelper = mock<NearbyMediaDetailHelper>()
        whenever(mockMediaHelper.handleBackPress()).thenReturn(false)
        whenever(presenter.backButtonClicked()).thenReturn(true)
        helper.mediaDetailHelperProvider = { mockMediaHelper }

        org.junit.Assert.assertTrue(helper.backButtonClicked())
        verify(presenter).backButtonClicked()
    }

    @Test
    fun testSetNearbyParentFragmentInstanceReadyCallbackCallsConsumer() {
        var capturedCallback: NearbyParentFragment.NearbyParentFragmentInstanceReadyCallback? = null
        helper.init(
            NearbyStatusDependencies(
                readyCallbackConsumer = { capturedCallback = it },
            ),
        )
        val mockCallback: NearbyParentFragment.NearbyParentFragmentInstanceReadyCallback = mock()
        helper.setNearbyParentFragmentInstanceReadyCallback(mockCallback)
        org.junit.Assert.assertEquals(mockCallback, capturedCallback)
    }

    @Test
    fun testUpdateSnackbarWhenNetworkErrorOccurredUpdatesText() {
        val snackbar: com.google.android.material.snackbar.Snackbar = mock()
        helper.snackbar = snackbar
        helper.isNetworkErrorOccurred = true
        helper.hasShownOfflinePinsMessage = false

        helper.updateSnackbar(true)

        verify(snackbar).setText(fr.free.nrw.commons.R.string.nearby_showing_pins_offline)
        org.junit.Assert.assertTrue(helper.hasShownOfflinePinsMessage)
    }

    @Test
    fun testIsNetworkConnectionEstablishedReturnsBoolean() {
        // Should execute without throwing any exception
        helper.isNetworkConnectionEstablished()
    }

    @Test
    fun testSetProgressBarVisibilityWhenBindingIsNullDoesNotCrash() {
        helper.setProgressBarVisibility(true)
        helper.setProgressBarVisibility(false)
    }

    @Test
    fun testBroadcastReceiverResetsHasShownOfflinePinsMessage() {
        helper.hasShownOfflinePinsMessage = true
        val receiver = helper.broadcastReceiver
        assertNotNull(receiver)
        receiver?.onReceive(activity, android.content.Intent())
        org.junit.Assert.assertFalse(helper.hasShownOfflinePinsMessage)
    }
}
