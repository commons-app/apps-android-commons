package fr.free.nrw.commons.upload

import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.Utils
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.license.MediaLicenseContract
import fr.free.nrw.commons.upload.license.MediaLicensePresenter
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

/**
 * The class contains unit test cases for MediaLicensePresenter
 */

@RunWith(PowerMockRunner::class)
@PrepareForTest(Utils::class)
class MediaLicensePresenterTest {
    @Mock
    internal var repository: UploadRepository? = null

    @Mock
    internal var view: MediaLicenseContract.View? = null

    @InjectMocks
    var mediaLicensePresenter: MediaLicensePresenter? = null

    /**
     * initial setup test environemnt
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        mediaLicensePresenter?.onAttachView(view)
        PowerMockito.mockStatic(Utils::class.java)
        PowerMockito.`when`(Utils.licenseNameFor(ArgumentMatchers.anyString())).thenReturn(1)
    }


    /**
     * unit test case for method MediaLicensePresenter.getLicense
     */
    @Test
    fun getLicenseTest() {
        mediaLicensePresenter?.getLicenses()
        verify(view)?.setLicenses(ArgumentMatchers.anyList())
        verify(view)?.setSelectedLicense(ArgumentMatchers.any())
    }

    /**
     * unit test case for method MediaLicensePresenter.selectLicense
     */
    @Test
    fun selectLicenseTest() {
        mediaLicensePresenter?.selectLicense(ArgumentMatchers.anyString())
        verify(view)?.updateLicenseSummary(ArgumentMatchers.any(), ArgumentMatchers.anyInt())
    }
}