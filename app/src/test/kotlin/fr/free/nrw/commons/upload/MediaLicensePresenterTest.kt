package fr.free.nrw.commons.upload

import com.nhaarman.mockito_kotlin.verify
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


@RunWith(PowerMockRunner::class)
@PrepareForTest(Utils::class)
class MediaLicensePresenterTest {
    @Mock
    internal var repository: UploadRepository? = null
    @Mock
    internal var view: MediaLicenseContract.View? = null

    @InjectMocks
    var mediaLicensePresenter: MediaLicensePresenter? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        mediaLicensePresenter!!.onAttachView(view)
        PowerMockito.mockStatic(Utils::class.java)
        PowerMockito.`when`(Utils.licenseNameFor(ArgumentMatchers.anyString())).thenReturn(1)
    }


    @Test
    fun getLicenseTest() {
        mediaLicensePresenter!!.getLicenses()
        verify(view!!).setLicenses(ArgumentMatchers.anyList())
        verify(view!!).setSelectedLicense(ArgumentMatchers.any())
    }

    @Test
    fun selectLicenseTest() {
        mediaLicensePresenter!!.selectLicense("test")
        verify(view!!).updateLicenseSummary(ArgumentMatchers.any(), ArgumentMatchers.anyInt())
    }
}