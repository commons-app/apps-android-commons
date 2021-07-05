package fr.free.nrw.commons.customselector.ui.selector

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.soloader.SoLoader
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.customselector.ui.adapter.FolderAdapter
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.AppAdapter
import java.lang.reflect.Field


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class FolderFragmentTest {

    private lateinit var fragment: FolderFragment
    private lateinit var layoutInflater: LayoutInflater
    private lateinit var context: Context
    private lateinit var viewModelField:Field

    @Mock
    private lateinit var adapter: FolderAdapter

    @Mock
    private lateinit var savedInstanceState: Bundle

    @Mock
    internal var sessionManager: SessionManager? = null

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = RuntimeEnvironment.application.applicationContext
        AppAdapter.set(TestAppAdapter())
        SoLoader.setInTestMode()
        Fresco.initialize(context)
        val activity = Robolectric.buildActivity(CustomSelectorActivity::class.java).create().get()
        fragment = FolderFragment.newInstance()
        val fragmentManager: FragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commit()
        layoutInflater = LayoutInflater.from(activity)
        val fieldContext: Field =
            SessionManager::class.java.getDeclaredField("context")
        fieldContext.isAccessible = true
        fieldContext.set(sessionManager, context)


        Whitebox.setInternalState(fragment, "folderAdapter", adapter)

        viewModelField = fragment.javaClass.getDeclaredField("viewModel")
        viewModelField.isAccessible = true
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreateView() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        viewModelField.set(fragment, null)
        fragment.onCreateView(layoutInflater, null, savedInstanceState)
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreate() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onCreate(savedInstanceState)
    }

    @Test
    fun testColumnCount() {
        val func = fragment.javaClass.getDeclaredMethod("columnCount")
        func.isAccessible = true
        assertEquals(2, func.invoke(fragment))
    }

}