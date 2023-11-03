package fr.free.nrw.commons.upload.depicts

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.textfield.TextInputLayout
import com.nhaarman.mockitokotlin2.whenever
import depictedItem
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.ui.PasteSensitiveTextInputEditText
import fr.free.nrw.commons.upload.UploadActivity
import fr.free.nrw.commons.upload.UploadBaseFragment
import io.reactivex.disposables.Disposable
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.AppAdapter
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class DepictsFragmentUnitTests {

    private lateinit var fragment: DepictsFragment
    private lateinit var fragmentManager: FragmentManager
    private lateinit var layoutInflater: LayoutInflater
    private lateinit var view: View
    private lateinit var context: Context

    @Mock
    private lateinit var savedInstanceState: Bundle

    @Mock
    private lateinit var textView: TextView

    @Mock
    private lateinit var imageView: ImageView

    @Mock
    private lateinit var recyclerView: RecyclerView

    @Mock
    private lateinit var textInputEditText: PasteSensitiveTextInputEditText

    @Mock
    private lateinit var progressBar: ProgressBar

    @Mock
    private lateinit var button: Button

    @Mock
    private lateinit var textInputLayout: TextInputLayout

    @Mock
    private lateinit var callback: UploadBaseFragment.Callback

    @Mock
    private lateinit var disposable: Disposable

    @Mock
    private lateinit var adapter: UploadDepictsAdapter

    @Mock
    private lateinit var applicationKvStore: JsonKvStore

    @Mock
    private lateinit var media: Media

    @Mock
    private lateinit var progressDialog: ProgressDialog

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        AppAdapter.set(TestAppAdapter())

        val activity = Robolectric.buildActivity(UploadActivity::class.java).create().get()
        fragment = DepictsFragment()
        fragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commitNowAllowingStateLoss()

        layoutInflater = LayoutInflater.from(activity)

        view = LayoutInflater.from(activity)
            .inflate(R.layout.upload_depicts_fragment, null) as View

        Whitebox.setInternalState(fragment, "depictsTitle", textView)
        Whitebox.setInternalState(fragment, "callback", callback)
        Whitebox.setInternalState(fragment, "tooltip", imageView)
        Whitebox.setInternalState(fragment, "btnNext", button)
        Whitebox.setInternalState(fragment, "btnPrevious", button)
        Whitebox.setInternalState(fragment, "depictsSubTitle", textView)
        Whitebox.setInternalState(fragment, "depictsRecyclerView", recyclerView)
        Whitebox.setInternalState(fragment, "depictsSearch", textInputEditText)
        Whitebox.setInternalState(fragment, "depictsSearchContainer", textInputLayout)
        Whitebox.setInternalState(fragment, "depictsSearchInProgress", progressBar)
        Whitebox.setInternalState(fragment, "subscribe", disposable)
        Whitebox.setInternalState(fragment, "adapter", adapter)
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreateView() {
        fragment.onCreateView(layoutInflater, null, savedInstanceState)
    }

    @Test
    @Throws(Exception::class)
    fun testInit() {
        val method: Method = DepictsFragment::class.java.getDeclaredMethod(
            "init"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun `Test init when media is not null`() {
        Whitebox.setInternalState(fragment, "media", media)
        val method: Method = DepictsFragment::class.java.getDeclaredMethod(
            "init"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testOnBecameVisible() {
        val method: Method = DepictsFragment::class.java.getDeclaredMethod(
            "onBecameVisible"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testGoToNextScreen() {
        fragment.goToNextScreen()
    }

    @Test
    @Throws(Exception::class)
    fun testGoToPreviousScreen() {
        fragment.goToPreviousScreen()
    }

    @Test
    @Throws(Exception::class)
    fun testNoDepictionSelected() {
        fragment.noDepictionSelected()
    }

    @Test
    @Throws(Exception::class)
    fun testOnDestroyView() {
        fragment.onDestroyView()
    }

    @Test
    @Throws(Exception::class)
    fun testShowProgress() {
        fragment.showProgress(true)
    }

    @Test
    @Throws(Exception::class)
    fun testShowErrorCaseTrue() {
        fragment.showError(true)
    }

    @Test
    @Throws(Exception::class)
    fun testShowErrorCaseFalse() {
        fragment.showError(false)
    }

    @Test
    @Throws(Exception::class)
    fun testSetDepictsList() {
        fragment.setDepictsList(listOf())
    }

    @Test
    @Throws(Exception::class)
    fun `Test setDepictsList when list is not empty`() {
        fragment.setDepictsList(listOf(depictedItem()))
    }

    @Test
    @Throws(Exception::class)
    fun `Test setDepictsList when applicationKvStore returns true`() {
        Whitebox.setInternalState(fragment, "applicationKvStore", applicationKvStore)
        whenever(applicationKvStore.getBoolean("first_edit_depict")).thenReturn(true)
        fragment.setDepictsList(listOf(depictedItem()))
    }

    @Test
    @Throws(Exception::class)
    fun testOnNextButtonClicked() {
        fragment.onNextButtonClicked()
    }

    @Test
    @Throws(Exception::class)
    fun testOnPreviousButtonClicked() {
        fragment.onPreviousButtonClicked()
    }

    @Test
    @Throws(Exception::class)
    fun testSearchForDepictions() {
        val method: Method = DepictsFragment::class.java.getDeclaredMethod(
            "searchForDepictions",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, "")
    }

    @Test
    @Throws(Exception::class)
    fun testOnResume() {
        fragment.onResume()
    }

    @Test
    @Throws(Exception::class)
    fun testOnStop() {
        fragment.onStop()
    }

    @Test
    @Throws(Exception::class)
    fun testInitRecyclerView() {
        val method: Method = DepictsFragment::class.java.getDeclaredMethod(
            "initRecyclerView"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun `Test initRecyclerView when media is not null`() {
        Whitebox.setInternalState(fragment, "media", media)
        val method: Method = DepictsFragment::class.java.getDeclaredMethod(
            "initRecyclerView"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testGetFragmentContext() {
        fragment.fragmentContext
    }

    @Test
    @Throws(Exception::class)
    fun testGoBackToPreviousScreen() {
        fragment.goBackToPreviousScreen()
    }

    @Test
    @Throws(Exception::class)
    fun testShowProgressDialog() {
        fragment.showProgressDialog()
    }

    @Test
    @Throws(Exception::class)
    fun testDismissProgressDialog() {
        Whitebox.setInternalState(fragment, "progressDialog", progressDialog)
        fragment.dismissProgressDialog()
    }
}