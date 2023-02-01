package fr.free.nrw.commons.customselector.ui.selector

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.os.Looper.getMainLooper
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.soloader.SoLoader
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.customselector.model.CallbackStatus
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.model.Result
import fr.free.nrw.commons.customselector.ui.adapter.ImageAdapter
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.AppAdapter
import java.lang.reflect.Field

/**
 * Custom Selector Image Fragment Test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ImageFragmentTest {

    private lateinit var fragment: ImageFragment
    private lateinit var activity: CustomSelectorActivity
    private lateinit var view: View
    private lateinit var selectorRV : RecyclerView
    private lateinit var loader : ProgressBar
    private lateinit var layoutInflater: LayoutInflater
    private lateinit var context: Context
    private lateinit var viewModelField: Field

    @Mock
    private lateinit var layoutManager: GridLayoutManager

    @Mock
    private lateinit var image: Image

    @Mock
    private lateinit var adapter: ImageAdapter

    @Mock
    private lateinit var savedInstanceState: Bundle

    /**
     * Setup the image fragment.
     */
    @Before
    fun setUp(){
        MockitoAnnotations.initMocks(this)
        context = ApplicationProvider.getApplicationContext()
        AppAdapter.set(TestAppAdapter())
        SoLoader.setInTestMode()
        Fresco.initialize(context)
        activity = Robolectric.buildActivity(CustomSelectorActivity::class.java).create().get()

        fragment = ImageFragment.newInstance(1,0)
        val fragmentManager: FragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commit()

        layoutInflater = LayoutInflater.from(activity)
        view = layoutInflater.inflate(R.layout.fragment_custom_selector, null, false) as View
        selectorRV = view.findViewById(R.id.selector_rv)
        loader = view.findViewById(R.id.loader)

        Whitebox.setInternalState(fragment, "imageAdapter", adapter)
        Whitebox.setInternalState(fragment, "selectorRV", selectorRV )
        Whitebox.setInternalState(fragment, "loader", loader)
        Whitebox.setInternalState(fragment, "filteredImages", arrayListOf(image,image))

        viewModelField = fragment.javaClass.getDeclaredField("viewModel")
        viewModelField.isAccessible = true
    }

    /**
     * Test onCreate
     */
    @Test
    @Throws(Exception::class)
    fun testOnCreate(){
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onCreate(savedInstanceState);
    }

    /**
     * Test onCreateView
     */
    @Test
    @Throws(Exception::class)
    fun testOnCreateView() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        viewModelField.set(fragment, null)
        fragment.onCreateView(layoutInflater, null, savedInstanceState)
    }

    /**
     * Test handleResult.
     */
    @Test
    fun testHandleResult(){
        val func = fragment.javaClass.getDeclaredMethod("handleResult", Result::class.java)
        func.isAccessible = true
        func.invoke(fragment, Result(CallbackStatus.SUCCESS, arrayListOf()))
        func.invoke(fragment, Result(CallbackStatus.SUCCESS, arrayListOf(image,image)))
    }

    /**
     * Test getSpanCount.
     */
    @Test
    fun testGetSpanCount() {
        val func = fragment.javaClass.getDeclaredMethod("getSpanCount")
        func.isAccessible = true
        assertEquals(3, func.invoke(fragment))
    }

    /**
     * Test onAttach function.
     */
    @Test
    fun testOnAttach() {
        fragment.onAttach(activity)
    }

    /**
     * Test refresh function.
     */
    @Test
    fun testRefresh() {
        fragment.refresh()
    }

    /**
     * Test onResume.
     */
    @Test
    fun testOnResume() {
        val func = fragment.javaClass.getDeclaredMethod("onResume")
        func.isAccessible = true
        func.invoke(fragment)
    }

    /**
     * Test onDestroy.
     */
    @Test
    fun testOnDestroy() {
        shadowOf(getMainLooper()).idle()
        selectorRV.layoutManager = layoutManager
        whenever(layoutManager.findFirstVisibleItemPosition()).thenReturn(1)
        val func = fragment.javaClass.getDeclaredMethod("onDestroy")
        func.isAccessible = true
        func.invoke(fragment)
    }

}