package fr.free.nrw.commons.upload

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.contributions.ContributionController
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.upload.categories.UploadCategoriesFragment
import fr.free.nrw.commons.upload.license.MediaLicenseFragment
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
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
class UploadActivityUnitTests {

    private lateinit var activity: UploadActivity
    private lateinit var context: Context

    @Mock
    private lateinit var uploadBaseFragment: UploadBaseFragment

    @Mock
    private lateinit var uploadableFile: UploadableFile

    @Mock
    private lateinit var presenter: UploadContract.UserActionListener

    @Mock
    private lateinit var contributionController: ContributionController

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        AppAdapter.set(TestAppAdapter())
        val intent = Intent()
        val list = ArrayList<UploadableFile>()
        list.add(uploadableFile)
        intent.putParcelableArrayListExtra(UploadActivity.EXTRA_FILES, list)
        activity = Robolectric.buildActivity(UploadActivity::class.java, intent).create().get()
        context = ApplicationProvider.getApplicationContext()

        Whitebox.setInternalState(activity, "fragments", mutableListOf(uploadBaseFragment))
        Whitebox.setInternalState(activity, "presenter", presenter)
        Whitebox.setInternalState(activity, "contributionController", contributionController)

        val config: Configuration = Configuration.Builder().build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testIsLoggedIn() {
        activity.isLoggedIn
    }

    @Test
    @Throws(Exception::class)
    fun testOnResume() {
        val method: Method = UploadActivity::class.java.getDeclaredMethod(
            "onResume"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnStop() {
        val method: Method = UploadActivity::class.java.getDeclaredMethod(
            "onStop"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testShowProgressCaseTrue() {
        activity.showProgress(true)
    }

    @Test
    @Throws(Exception::class)
    fun testShowProgressCaseFalse() {
        activity.showProgress(false)
    }

    @Test
    @Throws(Exception::class)
    fun testGetIndexInViewFlipper() {
        activity.getIndexInViewFlipper(uploadBaseFragment)
    }

    @Test
    @Throws(Exception::class)
    fun testGetTotalNumberOfSteps() {
        activity.totalNumberOfSteps
    }

    @Test
    @Throws(Exception::class)
    fun testIsWLMUpload() {
        activity.isWLMUpload
    }

    @Test
    @Throws(Exception::class)
    fun testShowMessage() {
        activity.showMessage(R.string.uploading_started)
    }

    @Test
    @Throws(Exception::class)
    fun testGetUploadableFiles() {
        activity.uploadableFiles
    }

    @Test
    @Throws(Exception::class)
    fun testShowHideTopCard() {
        activity.showHideTopCard(true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnUploadMediaDeleted() {
        Whitebox.setInternalState(activity, "uploadableFiles", mutableListOf(uploadableFile))
        activity.onUploadMediaDeleted(0)
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateTopCardTitle() {
        activity.updateTopCardTitle()
    }

    @Test
    @Throws(Exception::class)
    fun testMakeUploadRequest() {
        activity.makeUploadRequest()
    }

    @Test
    @Throws(Exception::class)
    fun testOnActivityResult() {
        val method: Method = UploadActivity::class.java.getDeclaredMethod(
            "onActivityResult",
            Int::class.java,
            Int::class.java,
            Intent::class.java
        )
        method.isAccessible = true
        method.invoke(activity, CommonsApplication.OPEN_APPLICATION_DETAIL_SETTINGS, 0, Intent())
    }

    @Test
    @Throws(Exception::class)
    fun testReceiveSharedItems() {
        val method: Method = UploadActivity::class.java.getDeclaredMethod(
            "receiveSharedItems"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testReceiveExternalSharedItems() {
        val method: Method = UploadActivity::class.java.getDeclaredMethod(
            "receiveExternalSharedItems"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testReceiveInternalSharedItems() {
        val method: Method = UploadActivity::class.java.getDeclaredMethod(
            "receiveInternalSharedItems"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testGetIsMultipleFilesSelected() {
        activity.isMultipleFilesSelected
    }

    @Test
    @Throws(Exception::class)
    fun testResetDirectPrefs() {
        activity.resetDirectPrefs()
    }

    @Test
    @Throws(Exception::class)
    fun testHandleNullMedia() {
        val method: Method = UploadActivity::class.java.getDeclaredMethod(
            "handleNullMedia"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnNextButtonClicked() {
        activity.onNextButtonClicked(-1)
    }

    @Test
    @Throws(Exception::class)
    fun testOnNextButtonClickedCaseFalse() {
        activity.onNextButtonClicked(0)
    }

    @Test
    @Throws(Exception::class)
    fun testOnPreviousButtonClicked() {
        activity.onPreviousButtonClicked(1)
    }

    @Test
    @Throws(Exception::class)
    fun testOnDestroy() {
        Whitebox.setInternalState(
            activity,
            "mediaLicenseFragment",
            mock(MediaLicenseFragment::class.java)
        )
        Whitebox.setInternalState(
            activity, "uploadCategoriesFragment", mock(
                UploadCategoriesFragment::class.java
            )
        )
        val method: Method = UploadActivity::class.java.getDeclaredMethod(
            "onDestroy"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackPressed() {
        val method: Method = UploadActivity::class.java.getDeclaredMethod(
            "onBackPressed"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

}