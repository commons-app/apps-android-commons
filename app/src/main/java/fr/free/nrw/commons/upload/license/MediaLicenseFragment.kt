package fr.free.nrw.commons.upload.license

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.FragmentMediaLicenseBinding
import fr.free.nrw.commons.upload.UploadActivity
import fr.free.nrw.commons.upload.UploadBaseFragment
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.handleWebUrl
import javax.inject.Inject

class MediaLicenseFragment :
    UploadBaseFragment(),
    MediaLicenseContract.View {
    @Inject
    lateinit var presenter: MediaLicenseContract.UserActionListener

    @Suppress("ktlint:standard:backing-property-naming")
    private var _binding: FragmentMediaLicenseBinding? = null
    private val binding: FragmentMediaLicenseBinding get() = _binding!!

    private var licenseAdapter: LicenseAdapter? = null
    private var licenses: List<String>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMediaLicenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvTitle.text =
            getString(
                R.string.step_count,
                callback!!.getIndexInViewFlipper(this) + 1,
                callback!!.totalNumberOfSteps,
                getString(R.string.license_step_title),
            )
        setTvSubTitle()
        binding.btnPrevious.setOnClickListener {
            callback!!.onPreviousButtonClicked(
                callback!!.getIndexInViewFlipper(this),
            )
        }

        binding.btnSubmit.setOnClickListener {
            callback!!.onNextButtonClicked(
                callback!!.getIndexInViewFlipper(this),
            )
        }

        binding.tooltip.setOnClickListener {
            showAlertDialog(
                requireActivity(),
                getString(R.string.license_step_title),
                getString(R.string.license_tooltip),
                getString(R.string.ok),
                null,
            )
        }
        binding.tvShowMoreLicenses.setOnClickListener {
            (presenter as? MediaLicensePresenter)?.onShowMoreClicked()
            // hidee the button once it is clicked
            binding.tvShowMoreLicenses.visibility = View.GONE
        }

        initPresenter()
        binding.rvLicenseList.layoutManager = LinearLayoutManager(requireContext())
        presenter.getLicenses()
    }

    /**
     * Removes the tv Subtitle If the activity is the instance of [UploadActivity] and
     * if multiple files aren't selected.
     */
    private fun setTvSubTitle() {
        val activity: Activity? = activity
        if (activity is UploadActivity) {
            if (!activity.isMultipleFilesSelected) {
                binding.tvSubtitle.visibility = View.GONE
            }
        }
    }

    private fun initPresenter() = presenter.onAttachView(this)

    override fun setLicenses(licenses: List<String>?) {
        this.licenses = licenses
        if (licenses != null) {
            licenseAdapter =
                LicenseAdapter(
                    licenses,
                    null,
                    { selected -> presenter.selectLicense(selected) },
                    { url -> launchBrowser(url) },
                )
            binding.rvLicenseList.adapter = licenseAdapter
        }
    }

    override fun setSelectedLicense(license: String?) {
        licenseAdapter?.setSelectedLicense(license)
    }

    override fun updateLicenseSummary(
        selectedLicense: String?,
        numberOfItems: Int,
    ) {}

    private fun launchBrowser(hyperLink: String) = handleWebUrl(requireContext(), Uri.parse(hyperLink))

    override fun onDestroyView() {
        presenter.onDetachView()
        // Free the adapter to avoid memory leaks
        licenseAdapter = null
        _binding = null
        super.onDestroyView()
    }

    override fun onBecameVisible() {
        super.onBecameVisible()
        /**
         * Show the wlm info message if the upload is a WLM upload
         */
        binding.llInfoMonumentUpload.visibility =
            if (callback!!.isWLMUpload && presenter.isWLMSupportedForThisPlace()) View.VISIBLE else View.GONE
    }
}
