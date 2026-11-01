package fr.free.nrw.commons.nearby

import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.createTestClient
import fr.free.nrw.commons.nearby.fragments.AdvanceQueryFragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class AdvanceQueryFragmentUnitTests {
    private lateinit var view: View
    private lateinit var activity: MainActivity
    private lateinit var layoutInflater: LayoutInflater
    private lateinit var fragment: AdvanceQueryFragment

    private lateinit var seekRadius: SeekBar
    private lateinit var placeTypesHeader: View
    private lateinit var placeTypesScroll: ScrollView
    private lateinit var placeTypesContainer: LinearLayout
    private lateinit var includeSubclassesTitle: TextView
    private lateinit var checkboxMissingImage: CheckBox
    private lateinit var checkboxHideDestroyed: CheckBox
    private lateinit var btnReset: AppCompatButton
    private lateinit var btnApply: AppCompatButton

    @Mock
    private lateinit var bundle: Bundle

    private val callback = RecordingCallback()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        OkHttpConnectionFactory.CLIENT = createTestClient()
        activity = Robolectric.buildActivity(MainActivity::class.java).create().get()

        fragment = AdvanceQueryFragment()
        fragment.arguments = bundle
        fragment.callback = callback
        val fragmentManager: FragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commit()

        Shadows.shadowOf(Looper.getMainLooper()).idle()

        layoutInflater = LayoutInflater.from(activity)
        view = layoutInflater.inflate(R.layout.fragment_advance_query, null)

        seekRadius = view.findViewById(R.id.seek_radius)
        placeTypesHeader = view.findViewById(R.id.place_types_header)
        placeTypesScroll = view.findViewById(R.id.place_types_scroll)
        placeTypesContainer = view.findViewById(R.id.place_types_container)
        includeSubclassesTitle = view.findViewById(R.id.tv_include_subclasses)
        checkboxMissingImage = view.findViewById(R.id.checkbox_missing_image)
        checkboxHideDestroyed = view.findViewById(R.id.checkbox_hide_destroyed)
        btnApply = view.findViewById(R.id.btn_apply)
        btnReset = view.findViewById(R.id.btn_reset)

        Whitebox.setInternalState(fragment, "seekRadius", seekRadius)
        Whitebox.setInternalState(fragment, "radiusValue", view.findViewById<TextView>(R.id.tv_radius_value))
        Whitebox.setInternalState(fragment, "radiusTicks", view.findViewById<LinearLayout>(R.id.radius_ticks))
        Whitebox.setInternalState(fragment, "placeTypesHeader", placeTypesHeader)
        Whitebox.setInternalState(fragment, "placeTypesArrow", view.findViewById<ImageView>(R.id.place_types_arrow))
        Whitebox.setInternalState(fragment, "placeTypesScroll", placeTypesScroll)
        Whitebox.setInternalState(fragment, "placeTypesContainer", placeTypesContainer)
        Whitebox.setInternalState(fragment, "checkboxMissingImage", checkboxMissingImage)
        Whitebox.setInternalState(fragment, "checkboxHideDestroyed", checkboxHideDestroyed)
        Whitebox.setInternalState(fragment, "btnApply", btnApply)
        Whitebox.setInternalState(fragment, "btnReset", btnReset)
    }

    @Test
    fun `check none of the views are null`() {
        assertNotNull(activity)
        assertNotNull(fragment)
        assertNotNull(bundle)
        assertNotNull(seekRadius)
        assertNotNull(placeTypesHeader)
        assertNotNull(placeTypesScroll)
        assertNotNull(placeTypesContainer)
        assertNotNull(includeSubclassesTitle)
        assertNotNull(checkboxMissingImage)
        assertNotNull(checkboxHideDestroyed)
        assertNotNull(btnReset)
        assertNotNull(btnApply)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `saved options are shown on the form`() {
        val options =
            NearbySearchOptions(
                10.0,
                listOf("Q16970"),
                listOf(Label.CHURCH.name),
                true,
                true,
                true,
            )
        `when`(bundle.getSerializable(NearbySearchOptions.ARG_KEY)).thenReturn(options)
        fragment.onViewCreated(view, bundle)

        assertEquals(NearbySearchOptions.sliderProgressForRadius(10.0), seekRadius.progress)
        assertTrue(checkboxMissingImage.isChecked)
        assertTrue(checkboxHideDestroyed.isChecked)
        val churchSelected =
            placeTypeRow(Label.CHURCH.name)
                .findViewById<ImageView>(R.id.place_selected)
        val buildingSelected =
            placeTypeRow(Label.BUILDING.name)
                .findViewById<ImageView>(R.id.place_selected)
        assertEquals(View.VISIBLE, churchSelected.visibility)
        assertEquals(View.GONE, buildingSelected.visibility)
    }

    @Test
    fun `apply sends the choices on the form`() {
        fragment.onViewCreated(view, bundle)
        seekRadius.progress = NearbySearchOptions.sliderProgressForRadius(10.0)
        checkboxMissingImage.isChecked = true
        placeTypeRow(Label.CHURCH.name).performClick()

        btnApply.performClick()

        val applied = callback.applied
        assertNotNull(applied)
        assertEquals(10.0, applied!!.radiusKm)
        assertTrue(applied.missingImageOnly)
        assertFalse(applied.excludeDestroyed)
        assertFalse(applied.includeSubclasses)
        assertEquals(listOf(Label.CHURCH.name), applied.labelNames)
        assertTrue(applied.classQids.contains("Q16970"))
    }

    @Suppress("DEPRECATION")
    @Test
    fun `reset clears the form and notifies the callback`() {
        val options =
            NearbySearchOptions(
                10.0,
                listOf("Q16970"),
                listOf(Label.CHURCH.name),
                false,
                true,
                true,
            )
        `when`(bundle.getSerializable(NearbySearchOptions.ARG_KEY)).thenReturn(options)
        fragment.onViewCreated(view, bundle)

        btnReset.performClick()

        assertTrue(callback.resetCalled)
        assertEquals(0, seekRadius.progress)
        assertFalse(checkboxMissingImage.isChecked)
        assertFalse(checkboxHideDestroyed.isChecked)
        val churchSelected =
            placeTypeRow(Label.CHURCH.name)
                .findViewById<ImageView>(R.id.place_selected)
        assertEquals(View.GONE, churchSelected.visibility)
    }

    @Test
    fun `place types list opens and closes without leaving the form`() {
        fragment.onViewCreated(view, bundle)
        assertEquals(View.VISIBLE, placeTypesScroll.visibility)

        placeTypesHeader.performClick()
        assertEquals(View.GONE, placeTypesScroll.visibility)

        placeTypesHeader.performClick()
        assertEquals(View.VISIBLE, placeTypesScroll.visibility)
    }

    private fun placeTypeRow(name: String): View {
        for (index in 0 until placeTypesContainer.childCount) {
            val row = placeTypesContainer.getChildAt(index)
            val text = row.findViewById<TextView>(R.id.place_text).text
            if (text == name) {
                return row
            }
        }
        throw AssertionError("Missing place type row $name")
    }

    private class RecordingCallback : AdvanceQueryFragment.Callback {
        var applied: NearbySearchOptions? = null
        var resetCalled: Boolean = false

        override fun reset() {
            resetCalled = true
        }

        override fun apply(options: NearbySearchOptions) {
            applied = options
        }
    }
}
