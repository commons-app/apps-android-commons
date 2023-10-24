package fr.free.nrw.commons.login

import android.app.ProgressDialog
import android.content.Context
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.auth.LoginActivity
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenuItem
import org.wikipedia.AppAdapter
import java.lang.reflect.Field


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class LoginActivityUnitTests {

    @Mock
    private lateinit var activity: LoginActivity

    private lateinit var context: Context

    @Mock
    private lateinit var progressDialog: ProgressDialog

    @Mock
    private lateinit var view: View

    @Mock
    private lateinit var params: ViewGroup.LayoutParams

    private lateinit var menuItem: MenuItem

    @Before
    fun setUp() {

        MockitoAnnotations.openMocks(this)

        AppAdapter.set(TestAppAdapter())

        activity = Robolectric.buildActivity(LoginActivity::class.java).create().get()

        context = ApplicationProvider.getApplicationContext()

        val fieldProgressDialog: Field =
            LoginActivity::class.java.getDeclaredField("progressDialog")
        fieldProgressDialog.isAccessible = true
        fieldProgressDialog.set(activity, progressDialog)

        menuItem = RoboMenuItem(null)
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testShowMessageAndCancelDialog() {
        activity.showMessageAndCancelDialog("")
    }

    @Test
    @Throws(Exception::class)
    fun testStartMainActivity() {
        activity.startMainActivity()
    }

    @Test
    @Throws(Exception::class)
    fun testShowMessageAndCancelDialogRes() {
        activity.showMessageAndCancelDialog(R.color.secondaryDarkColor)
    }

    @Test
    @Throws(Exception::class)
    fun testAskUserForTwoFactorAuth() {
        activity.askUserForTwoFactorAuth()
    }

    @Test
    @Throws(Exception::class)
    fun testShowSuccessAndDismissDialog() {
        activity.showSuccessAndDismissDialog()
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelected() {
        activity.onOptionsItemSelected(menuItem)
    }

    @Test
    @Throws(Exception::class)
    fun testPerformLogin() {
        activity.performLogin()
    }

    @Test
    @Throws(Exception::class)
    fun testSetContentView() {
        activity.setContentView(view, params)
    }

}
