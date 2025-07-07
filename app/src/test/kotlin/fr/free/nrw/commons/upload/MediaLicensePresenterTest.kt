package fr.free.nrw.commons.upload

import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.utils.UnderlineUtils
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.license.MediaLicenseContract
import fr.free.nrw.commons.upload.license.MediaLicensePresenter
import fr.free.nrw.commons.utils.toLicenseName
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.RobolectricTestRunner

/**
 * The class contains unit test cases for MediaLicensePresenter
 */

@RunWith(RobolectricTestRunner::class)
@PrepareForTest(UnderlineUtils::class)
class MediaLicensePresenterTest {
    @Mock
    internal lateinit var repository: UploadRepository

    @Mock
    internal lateinit var defaultKvStore: JsonKvStore

    @Mock
    internal lateinit var view: MediaLicenseContract.View

    @InjectMocks
    lateinit var mediaLicensePresenter: MediaLicensePresenter

    private lateinit var mockedUtil: MockedStatic<UnderlineUtils>

    /**
     * initial setup test environemnt
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mediaLicensePresenter.onAttachView(view)
        mockedUtil = Mockito.mockStatic(UnderlineUtils::class.java)
    }

    @After
    fun tearDown() {
        mockedUtil.close()
    }

    /**
     * unit test case for method MediaLicensePresenter.getLicense
     */
    @Test
    fun getLicenseTest() {
        mediaLicensePresenter.getLicenses()
        verify(view).setLicenses(ArgumentMatchers.anyList())
        verify(view).setSelectedLicense(ArgumentMatchers.any())
    }

    /**
     * unit test case for method MediaLicensePresenter.selectLicense
     */
    @Test
    fun selectLicenseTest() {
        mediaLicensePresenter.selectLicense(ArgumentMatchers.anyString())
        verify(view).updateLicenseSummary(ArgumentMatchers.any(), ArgumentMatchers.anyInt())
    }
}
