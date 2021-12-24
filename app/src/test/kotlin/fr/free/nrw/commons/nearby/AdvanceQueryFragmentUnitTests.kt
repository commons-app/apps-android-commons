package fr.free.nrw.commons.nearby

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.nearby.fragments.AdvanceQueryFragment
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.AppAdapter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class AdvanceQueryFragmentUnitTests {
    private lateinit var context: Context
    private lateinit var activity: MainActivity
    private lateinit var fragment: AdvanceQueryFragment

    private lateinit var viewGroup: ViewGroup

    @Mock
    private lateinit var layoutInflater: LayoutInflater

    @Mock
    private lateinit var callback: AdvanceQueryFragment.Callback

    @Mock
    private lateinit var view: View

    @Mock
    private lateinit var etQuery: AppCompatEditText

    @Mock
    private lateinit var btnReset: AppCompatButton

    @Mock
    private lateinit var btnApply: AppCompatButton

    @Mock
    private lateinit var bundle: Bundle

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        AppAdapter.set(TestAppAdapter())
        context = RuntimeEnvironment.application.applicationContext
        activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        viewGroup = FrameLayout(context)

        Mockito.`when`(bundle.getString("query")).thenReturn("test")

        fragment = AdvanceQueryFragment()
        fragment.callback = callback
        fragment.arguments = bundle

        val fragmentManager: FragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commit()

        Mockito.`when`(layoutInflater.inflate(R.layout.fragment_advance_query, viewGroup, false))
            .thenReturn(view)
        Mockito.`when`(view.findViewById<AppCompatEditText>(R.id.et_query)).thenReturn(etQuery)
        Mockito.`when`(view.findViewById<AppCompatButton>(R.id.btn_apply)).thenReturn(btnApply)
        Mockito.`when`(view.findViewById<AppCompatButton>(R.id.btn_reset)).thenReturn(btnReset)
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Assert.assertNotNull(fragment)
    }

    @Test
    fun testOnCreateView() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onCreateView(layoutInflater, viewGroup, bundle)
        verify(layoutInflater).inflate(R.layout.fragment_advance_query, viewGroup, false)
    }

    @Test
    fun testOnViewCreated() {
        fragment.onCreateView(layoutInflater, viewGroup, bundle)
        fragment.onViewCreated(view, bundle)
        verify(etQuery).setText("test")

        Mockito.`when`(btnReset.post(any())).thenAnswer {
            it.getArgument(0, Runnable::class.java).run()
        }

        Mockito.`when`(btnApply.post(any())).thenAnswer {
            it.getArgument(0, Runnable::class.java).run()
        }
        btnReset.performClick()
        btnApply.performClick()
    }

    @Test
    fun testHideKeyboard() {
        fragment.hideKeyBoard()
    }
}