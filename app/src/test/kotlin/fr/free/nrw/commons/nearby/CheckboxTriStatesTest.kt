package fr.free.nrw.commons.nearby

import android.widget.CompoundButton
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.CheckBoxTriStates.CHECKED
import fr.free.nrw.commons.nearby.CheckBoxTriStates.UNCHECKED
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class CheckBoxTriStatesTest {
    @Mock
    internal lateinit var callback: CheckBoxTriStates.Callback
    @Mock
    internal lateinit var onCheckChangeListener: CompoundButton.OnCheckedChangeListener
    private lateinit var checkBoxTriStates: CheckBoxTriStates

    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        checkBoxTriStates = CheckBoxTriStates(ApplicationProvider.getApplicationContext())
        checkBoxTriStates.setCallback(callback)
        checkBoxTriStates.setOnCheckedChangeListener(onCheckChangeListener)
    }

    /**
     * If same state is trying to be set, nothing should happen
     */
    @Test
    fun testSetStateWhenSameState() {
        checkBoxTriStates.state = CHECKED
        checkBoxTriStates.setState(CHECKED)
        verifyNoInteractions(callback)
    }

    /**
     * If different, markers should be filtered by new state
     */
    @Test
    fun testSetStateWhenDiffState() {
        NearbyController.currentLocation = LatLng(0.0,0.0,0.0f)
        checkBoxTriStates.state = CHECKED
        checkBoxTriStates.setState(UNCHECKED)
        verify(callback).filterByMarkerType(null, UNCHECKED, false, true)
    }

    /**
     * If current latitude longtitude null, then no more interactions required
     */
    @Test
    fun testSetStateWhenCurrLatLngNull() {
        NearbyController.currentLocation = null
        checkBoxTriStates.state = CHECKED
        checkBoxTriStates.setState(UNCHECKED)
        verifyNoInteractions(callback)
    }
}