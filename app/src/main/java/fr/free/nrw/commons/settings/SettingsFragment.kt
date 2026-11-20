package fr.free.nrw.commons.settings

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import fr.free.nrw.commons.R
import fr.free.nrw.commons.activity.SingleWebViewActivity
import fr.free.nrw.commons.campaigns.CampaignView
import fr.free.nrw.commons.contributions.ContributionController
import fr.free.nrw.commons.databinding.ProgressDialogBinding
import fr.free.nrw.commons.di.ApplicationlessInjection
import fr.free.nrw.commons.filepicker.FilePicker
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.logging.CommonsLogSender
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao
import fr.free.nrw.commons.utils.DialogUtil
import fr.free.nrw.commons.utils.PermissionUtils
import fr.free.nrw.commons.utils.StringUtil
import fr.free.nrw.commons.utils.ViewUtil
import fr.free.nrw.commons.utils.handleWebUrl
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.settings.sync.UserSettingsSyncManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider

class SettingsFragment : PreferenceFragmentCompat() {

    @Inject
    @field: Named("default_preferences")
    lateinit var defaultKvStore: JsonKvStore

    @Inject
    lateinit var commonsLogSender: CommonsLogSender

    @Inject
    lateinit var recentLanguagesDao: RecentLanguagesDao

    @Inject
    lateinit var contributionController: ContributionController

    @Inject
    lateinit var locationManager: LocationServiceManager

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var userSettingsSyncManagerProvider: Provider<UserSettingsSyncManager>

    private val compositeDisposable = CompositeDisposable()
    private var activeProgressDialog: AlertDialog? = null

    internal val languageController = SettingsLanguageController(this)

    private var vanishAccountPreference: Preference? = null
    private var themeListPreference: ListPreference? = null
    private var showDeletionButtonPreference: Preference? = null

    private lateinit var inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>

    private val cameraPickLauncherForResult: ActivityResultLauncher<Intent> =
        registerForActivityResult(StartActivityForResult()) { result ->
        contributionController.handleActivityResultWithCallback(
            requireActivity(),
            object: FilePicker.HandleActivityResult {
                override fun onHandleActivityResult(callbacks: FilePicker.Callbacks) {
                    contributionController.onPictureReturnedFromCamera(
                        result,
                        requireActivity(),
                        callbacks
                    )
                }
        })
    }

    /**
     * to be called when the fragment creates preferences
     * @param savedInstanceState the previously saved state
     * @param rootKey the root key for preferences
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        ApplicationlessInjection
            .getInstance(requireActivity().applicationContext)
            .commonsApplicationComponent
            .inject(this)

        // Set the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey)

        themeListPreference = findPreference(Prefs.KEY_THEME_VALUE)
        prepareTheme()

        vanishAccountPreference = findPreference(Prefs.VANISHED_ACCOUNT)
        vanishAccountPreference?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.account_vanish_request_confirm_title)
                .setMessage(StringUtil.fromHtml(getString(R.string.account_vanish_request_confirm)))
                .setNegativeButton(R.string.cancel){ dialog,_ ->
                    dialog.dismiss()
                }
                .setPositiveButton(R.string.vanish_account) { dialog, _ ->
                    SingleWebViewActivity.showWebView(
                        context = requireActivity(),
                        url = VANISH_ACCOUNT_URL,
                        successUrl = VANISH_ACCOUNT_SUCCESS_URL
                    )
                    dialog.dismiss()
                }
                .show()
            true
        }

        val multiSelectListPref: MultiSelectListPreference? = findPreference(
            Prefs.MANAGED_EXIF_TAGS
        )
        multiSelectListPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is HashSet<*> && !newValue.contains(getString(R.string.exif_tag_location)))
            {
                defaultKvStore.putBoolean("has_user_manually_removed_location", true)
            }
            true
        }

        val inAppCameraLocationPref: Preference? = findPreference("inAppCameraLocationPref")
        inAppCameraLocationPref?.setOnPreferenceChangeListener { _, newValue ->
            val isInAppCameraLocationTurnedOn = newValue as Boolean
            if (isInAppCameraLocationTurnedOn) {
                createDialogsAndHandleLocationPermissions()
            }
            true
        }

        inAppCameraLocationPermissionLauncher = registerForActivityResult(
            RequestMultiplePermissions()
        ) { result ->
            var areAllGranted = true
            for (b in result.values) {
                areAllGranted = areAllGranted && b
            }
            if (
                !areAllGranted
                    &&
                shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)
            ) {
                contributionController.handleShowRationaleFlowCameraLocation(
                    requireActivity(),
                    inAppCameraLocationPermissionLauncher,
                    cameraPickLauncherForResult
                )
            }
        }

        languageController.setupLanguagePreferences()

        showDeletionButtonPreference = findPreference("displayDeletionButton")
        showDeletionButtonPreference?.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = newValue as Boolean
            // Save preference when user toggles the button
            defaultKvStore.putBoolean("displayDeletionButton", isEnabled)
            true
        }

        val betaTesterPreference: Preference? = findPreference("becomeBetaTester")
        betaTesterPreference?.setOnPreferenceClickListener {
            handleWebUrl(
                requireActivity(),
                Uri.parse(getString(R.string.beta_opt_in_link))
            )
            true
        }

        val sendLogsPreference: Preference? = findPreference("sendLogFile")
        sendLogsPreference?.setOnPreferenceClickListener {
            checkPermissionsAndSendLogs()
            true
        }

        val documentBasedPickerPreference: Preference? = findPreference(
            "openDocumentPhotoPickerPref"
        )
        documentBasedPickerPreference?.setOnPreferenceChangeListener { _, newValue ->
            val isGetContentPickerTurnedOn = newValue as Boolean
            if (!isGetContentPickerTurnedOn) {
                showLocationLossWarning()
            }
            true
        }

        setupSyncPreferences()

        // Disable some settings when not logged in.
        if (defaultKvStore.getBoolean("login_skipped", false)) {
            findPreference<Preference>("useExternalStorage")?.isEnabled = false
            findPreference<Preference>("useAuthorName")?.isEnabled = false
            findPreference<Preference>("displayNearbyCardView")?.isEnabled = false
            findPreference<Preference>("descriptionDefaultLanguagePref")?.isEnabled = false
            findPreference<Preference>("displayLocationPermissionForCardView")?.isEnabled = false
            findPreference<Preference>(CampaignView.CAMPAIGNS_DEFAULT_PREFERENCE)?.isEnabled = false
            findPreference<Preference>("managed_exif_tags")?.isEnabled = false
            findPreference<Preference>("openDocumentPhotoPickerPref")?.isEnabled = false
            findPreference<Preference>("inAppCameraLocationPref")?.isEnabled = false
            findPreference<Preference>("vanishAccount")?.isEnabled = false
            findPreference<Preference>("backupSettings")?.isEnabled = false
            findPreference<Preference>("restoreSettings")?.isEnabled = false
        }
    }

    private fun setupSyncPreferences() {
        val backupPref: Preference? = findPreference("backupSettings")
        val restorePref: Preference? = findPreference("restoreSettings")

        if (defaultKvStore.getBoolean("login_skipped", false) || !sessionManager.isUserLoggedIn) {
            backupPref?.isEnabled = false
            restorePref?.isEnabled = false
            return
        }

        backupPref?.setOnPreferenceClickListener {
            val userName = sessionManager.userName ?: ""
            showSyncConfirmationDialog(
                titleRes = R.string.sync_backup_confirm_title,
                messageRes = R.string.sync_backup_confirm_message,
                userName = userName,
                syncAction = { userSettingsSyncManagerProvider.get().backupToUserPage() },
                successMsgRes = R.string.sync_backup_success,
                failedMsgRes = R.string.sync_backup_failed,
                shouldRecreate = false,
            )
            true
        }

        restorePref?.setOnPreferenceClickListener {
            val userName = sessionManager.userName ?: ""
            showSyncConfirmationDialog(
                titleRes = R.string.sync_restore_confirm_title,
                messageRes = R.string.sync_restore_confirm_message,
                userName = userName,
                syncAction = { userSettingsSyncManagerProvider.get().restoreFromUserPage() },
                successMsgRes = R.string.sync_restore_success,
                failedMsgRes = R.string.sync_restore_failed,
                shouldRecreate = true,
            )
            true
        }
    }

    private fun showSyncConfirmationDialog(
        titleRes: Int,
        messageRes: Int,
        userName: String,
        syncAction: () -> Single<Boolean>,
        successMsgRes: Int,
        failedMsgRes: Int,
        shouldRecreate: Boolean,
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle(titleRes)
            .setMessage(getString(messageRes, userName))
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.submit) { dialog, _ ->
                dialog.dismiss()
                val dialogBinding = ProgressDialogBinding.inflate(layoutInflater)
                dialogBinding.progressDialogText.text = getString(R.string.sync_in_progress)
                activeProgressDialog = AlertDialog.Builder(requireContext())
                    .setView(dialogBinding.root)
                    .setCancelable(false)
                    .create()
                activeProgressDialog?.show()

                compositeDisposable.add(
                    syncAction()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { success ->
                                dismissProgressDialog()
                                if (!isAdded || context == null) return@subscribe
                                if (success) {
                                    ViewUtil.showLongToast(requireContext(), getString(successMsgRes))
                                    if (shouldRecreate) {
                                        requireActivity().recreate()
                                    }
                                } else {
                                    ViewUtil.showLongToast(requireContext(), getString(failedMsgRes, ""))
                                }
                            },
                            { throwable ->
                                dismissProgressDialog()
                                if (!isAdded || context == null) return@subscribe
                                val errorMsg = throwable.localizedMessage ?: ""
                                ViewUtil.showLongToast(requireContext(), getString(failedMsgRes, errorMsg))
                            },
                        ),
                )
            }
            .show()
    }

    override fun onDestroyView() {
        dismissProgressDialog()
        compositeDisposable.clear()
        super.onDestroyView()
    }

    private fun dismissProgressDialog() {
        activeProgressDialog?.dismiss()
        activeProgressDialog = null
    }

    /**
     * Asks users to provide location access.
     */
    private fun createDialogsAndHandleLocationPermissions() {
        inAppCameraLocationPermissionLauncher.launch(arrayOf(permission.ACCESS_FINE_LOCATION))
    }

    /**
     * On some devices, the new Photo Picker with GET_CONTENT takeover
     * redacts location tags from EXIF metadata
     *
     * Show warning to the user when ACTION_GET_CONTENT intent is enabled
     */
    private fun showLocationLossWarning() {
        DialogUtil.showAlertDialog(
            requireActivity(),
            null,
            getString(R.string.location_loss_warning),
            getString(R.string.ok),
            getString(R.string.read_help_link),
            { },
            { handleWebUrl(requireContext(), Uri.parse(GET_CONTENT_PICKER_HELP_URL)) },
            null
        )
    }

    // Remove the space for icons in the settings menu.
    // This uses an internal API that shouldn't be used in app code,
    // but it appears to be the most robust way to do this at the moment,
    // disable the warning.
    @SuppressLint("RestrictedApi")
    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): Adapter<PreferenceViewHolder>
    {
        return object : PreferenceGroupAdapter(preferenceScreen) {
            override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
                super.onBindViewHolder(holder, position)
                val iconFrame: View? = holder.itemView.findViewById(R.id.icon_frame)
                iconFrame?.visibility = View.GONE
            }
        }
    }

    /**
     * Sets the theme pref
     */
    private fun prepareTheme() {
        themeListPreference?.setOnPreferenceChangeListener { _, _ ->
            requireActivity().recreate()
            true
        }
    }

    /**
     * Changing the default app language with selected one and save it to SharedPreferences
     */
    fun setLocale(activity: Activity, userSelectedValue: String) {
        SettingsLanguageController.setLocale(activity, userSelectedValue)
    }

    @Suppress("LongLine")
    companion object {
        const val GET_CONTENT_PICKER_HELP_URL = "https://commons-app.github.io/docs.html#get-content"
        private const val VANISH_ACCOUNT_URL = "https://meta.m.wikimedia.org/wiki/Special:Contact/accountvanishapps"
        private const val VANISH_ACCOUNT_SUCCESS_URL =
            "https://meta.m.wikimedia.org/wiki/" +
                "Special:GlobalVanishRequest/vanished"

        /**
         * Create Locale based on different types of language codes
         * @param languageCode
         * @return Locale and throws error for invalid language codes
         */
        fun createLocale(languageCode: String): Locale =
            SettingsLanguageController.createLocale(languageCode)
    }

    /**
     * First checks for external storage permissions and then sends logs via email
     */
    private fun checkPermissionsAndSendLogs() {
        if (
            PermissionUtils.hasPermission(
                requireActivity(),
                PermissionUtils.PERMISSIONS_STORAGE
            )
        ) {
            commonsLogSender.sendWithNullable(requireActivity(), null)
        } else {
            requestExternalStoragePermissions()
        }
    }

    /**
     * Requests external storage permissions and shows a toast stating that log collection has
     * started
     */
    private fun requestExternalStoragePermissions() {
        Dexter.withActivity(requireActivity())
            .withPermissions(*PermissionUtils.PERMISSIONS_STORAGE)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    ViewUtil.showLongToast(requireActivity(), getString(R.string.log_collection_started))
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>, token: PermissionToken
                ) {
                    // No action needed
                }
            })
            .onSameThread()
            .check()
    }

}
