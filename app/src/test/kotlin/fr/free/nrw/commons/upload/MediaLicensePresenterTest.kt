package fr.free.nrw.commons.upload

import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.license.MediaLicenseContract
import fr.free.nrw.commons.upload.license.MediaLicensePresenter
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations.initMocks
import org.powermock.modules.junit4.PowerMockRunner

/**
 * The class contains unit test cases for MediaLicensePresenter
 */

@RunWith(PowerMockRunner::class)
class MediaLicensePresenterTest {
    @Mock
    internal lateinit var repository: UploadRepository

    @Mock
    internal lateinit var defaultKvStore: JsonKvStore

    @Mock
    internal lateinit var view: MediaLicenseContract.View

    @InjectMocks
    lateinit var mediaLicensePresenter: MediaLicensePresenter

    /**
     * initial setup test environemnt
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        initMocks(this)
        mediaLicensePresenter.onAttachView(view)
    }

    /**
     * unit test case for method MediaLicensePresenter.getLicense
     */
    @Test
    fun getLicenseTest() {
        mediaLicensePresenter.getLicenses()
        verify(view).setLicenses(anyList())
        verify(view).setSelectedLicense(any())
    }

    /**
     * unit test case for method MediaLicensePresenter.selectLicense
     */
    @Test
    fun selectLicenseTest() {
        mediaLicensePresenter.selectLicense(anyString())
        verify(view).updateLicenseSummary(any(), anyInt())
    }
}
