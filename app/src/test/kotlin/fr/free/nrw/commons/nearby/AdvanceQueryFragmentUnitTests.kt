package fr.free.nrw.commons.nearby

import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.nearby.fragments.AdvanceQueryFragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.AppAdapter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class AdvanceQueryFragmentUnitTests {

    private lateinit var view: View
    private lateinit var activity: MainActivity
    private lateinit var layoutInflater: LayoutInflater
    private lateinit var fragment: AdvanceQueryFragment

    private val defaultQuery = "test"

    @Mock
    private lateinit var bundle: Bundle

    @Mock
    private lateinit var etQuery: AppCompatEditText

    @Mock
    private lateinit var btnReset: AppCompatButton

    @Mock
    private lateinit var btnApply: AppCompatButton

    @Mock
    private lateinit var callback: AdvanceQueryFragment.Callback

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        AppAdapter.set(TestAppAdapter())
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

        etQuery = view.findViewById(R.id.et_query)
        btnApply = view.findViewById(R.id.btn_apply)
        btnReset = view.findViewById(R.id.btn_reset)

        Whitebox.setInternalState(fragment, "etQuery", etQuery)
        Whitebox.setInternalState(fragment, "btnApply", btnApply)
        Whitebox.setInternalState(fragment, "btnReset", btnReset)

        // setting default query
        `when`(bundle.getString("query")).thenReturn(defaultQuery)
    }

    @Test
    fun `check none of the views are null`() {
        assertNotNull(activity)
        assertNotNull(fragment)
        assertNotNull(bundle)
        assertNotNull("EditText could not be found", etQuery)
        assertNotNull("Button could not be found", btnReset)
        assertNotNull("Button could not be found", btnApply)
    }

    @Test
    fun `when query passed in fragment argument, it is visible in text field`() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onViewCreated(view, bundle)
        assertEquals(defaultQuery, etQuery.text.toString())
    }


    @Test
    fun `when no query is passed in fragment argument, nothing is visible in text field`() {
        `when`(bundle.getString("query")).thenReturn("")
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onViewCreated(view, bundle)
        assertEquals("", etQuery.text.toString())
    }

    @Test
    fun `when apply button is clicked, callback is notified with new string and screen is closed`() {
        // Checking initial query is showing on text view
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onViewCreated(view, bundle)
        assertEquals(defaultQuery, etQuery.text.toString())

        // Setting new query to text view
        val newQuery = "$defaultQuery 2"
        etQuery.setText(newQuery)

        // Clicking apply button
        btnApply.performClick()

        // Verifying if call is notified with changed argument query
        verify(callback).apply(newQuery)
        verify(callback).close()
    }

    @Test
    fun `when reset button is clicked, initial query is visible in text field`() {
        // Checking initial query is showing on text view
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onViewCreated(view, bundle)
        assertEquals(defaultQuery, etQuery.text.toString())

        // Setting new query to text view
        val newQuery = "$defaultQuery 2"
        etQuery.setText(newQuery)

        // Clicking reset button
        btnReset.performClick()

        // Verifying if text view is showing initial query and callback is notified
        assertEquals(defaultQuery, etQuery.text.toString())
        verify(callback).reset()
    }

    @Test
    fun `when apply button is clicked with no change, callback is notified and screen is closed`() {
        // Checking initial query is showing on text view
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onViewCreated(view, bundle)
        assertEquals(defaultQuery, etQuery.text.toString())

        // Clicking apply button
        btnApply.performClick()

        // Verifying if call is notified with initial argument query
        verify(callback).apply(defaultQuery)
        verify(callback).close()
    }
}
