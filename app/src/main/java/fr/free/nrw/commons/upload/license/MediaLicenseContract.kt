package fr.free.nrw.commons.upload.license

import fr.free.nrw.commons.BasePresenter

/**
 * The contract with with MediaLicenseFragment and its presenter would talk to each other
 */
interface MediaLicenseContract {
    interface View {
        fun setLicenses(licenses: List<String>?)

        fun setSelectedLicense(license: String?)

        fun updateLicenseSummary(selectedLicense: String?, numberOfItems: Int?)
    }

    interface UserActionListener : BasePresenter<View> {
        fun getLicenses()

        fun selectLicense(licenseName: String)

        fun isWLMSupportedForThisPlace(): Boolean
    }
}
