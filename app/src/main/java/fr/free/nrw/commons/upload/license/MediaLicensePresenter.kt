package fr.free.nrw.commons.upload.license

import fr.free.nrw.commons.utils.Utils
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.settings.Prefs
import timber.log.Timber
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import javax.inject.Inject
import javax.inject.Named

/**
 * Added JavaDocs for MediaLicensePresenter
 */
class MediaLicensePresenter @Inject constructor(
    private val repository: UploadRepository,
    @param:Named("default_preferences") private val defaultKVStore: JsonKvStore
) : MediaLicenseContract.UserActionListener {
    private var view = DUMMY

    override fun onAttachView(view: MediaLicenseContract.View) {
        this.view = view
    }

    override fun onDetachView() {
        view = DUMMY
    }

    /**
     * asks the repository for the available licenses, and informs the view on the same
     */
    override fun getLicenses() {
        val licenses = repository.getLicenses()
        view.setLicenses(licenses)

        var selectedLicense = defaultKVStore.getString(
            Prefs.DEFAULT_LICENSE,
            Prefs.Licenses.CC_BY_SA_4
        ) //CC_BY_SA_4 is the default one used by the commons web app
        try { //I have to make sure that the stored default license was not one of the deprecated one's
            Utils.licenseNameFor(selectedLicense)
        } catch (exception: IllegalStateException) {
            Timber.e(exception)
            selectedLicense = Prefs.Licenses.CC_BY_SA_4
            defaultKVStore.putString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA_4)
        }
        view.setSelectedLicense(selectedLicense)
    }

    /**
     * ask the repository to select a license for the current upload
     */
    override fun selectLicense(licenseName: String?) {
        repository.setSelectedLicense(licenseName)
        view.updateLicenseSummary(repository.getSelectedLicense(), repository.getCount())
    }

    override fun isWLMSupportedForThisPlace(): Boolean =
        repository.isWMLSupportedForThisPlace()

    companion object {
        private val DUMMY = Proxy.newProxyInstance(
            MediaLicenseContract.View::class.java.classLoader,
            arrayOf<Class<*>>(MediaLicenseContract.View::class.java)
        ) { _: Any?, _: Method?, _: Array<Any?>? -> null } as MediaLicenseContract.View
    }
}
