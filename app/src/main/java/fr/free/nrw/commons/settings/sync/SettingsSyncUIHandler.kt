package fr.free.nrw.commons.settings.sync

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.databinding.ProgressDialogBinding
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.utils.ViewUtil
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

/**
 * UI context holder for sync preferences operations.
 */
data class SyncUIContext(
    val context: Context,
    val activity: FragmentActivity,
    val layoutInflater: LayoutInflater,
    val compositeDisposable: CompositeDisposable,
    val isFragmentAdded: () -> Boolean,
)

/**
 * UI delegate handling backup/restore dialog flows and preferences for SettingsFragment.
 */
@Singleton
class SettingsSyncUIHandler
    @Inject
    constructor(
        private val sessionManager: SessionManager,
        @param:Named("default_preferences") private val defaultKvStore: JsonKvStore,
        private val userSettingsSyncManagerProvider: Provider<UserSettingsSyncManager>,
    ) {
        private var activeProgressDialog: AlertDialog? = null

        fun setupSyncPreferences(
            backupPref: Preference?,
            restorePref: Preference?,
            uiContext: SyncUIContext,
        ) {
            if (defaultKvStore.getBoolean("login_skipped", false) || !sessionManager.isUserLoggedIn) {
                backupPref?.isEnabled = false
                restorePref?.isEnabled = false
                return
            }

            backupPref?.setOnPreferenceClickListener {
                val userName = sessionManager.userName ?: ""
                showSyncConfirmationDialog(
                    SyncDialogParams(
                        titleRes = R.string.sync_backup_confirm_title,
                        messageRes = R.string.sync_backup_confirm_message,
                        userName = userName,
                        syncAction = { userSettingsSyncManagerProvider.get().backupToUserPage() },
                        successMsgRes = R.string.sync_backup_success,
                        failedMsgRes = R.string.sync_backup_failed,
                        shouldRecreate = false,
                    ),
                    uiContext,
                )
                true
            }

            restorePref?.setOnPreferenceClickListener {
                val userName = sessionManager.userName ?: ""
                showSyncConfirmationDialog(
                    SyncDialogParams(
                        titleRes = R.string.sync_restore_confirm_title,
                        messageRes = R.string.sync_restore_confirm_message,
                        userName = userName,
                        syncAction = { userSettingsSyncManagerProvider.get().restoreFromUserPage() },
                        successMsgRes = R.string.sync_restore_success,
                        failedMsgRes = R.string.sync_restore_failed,
                        shouldRecreate = true,
                    ),
                    uiContext,
                )
                true
            }
        }

        fun dismissProgressDialog() {
            activeProgressDialog?.dismiss()
            activeProgressDialog = null
        }

        private fun showSyncConfirmationDialog(
            params: SyncDialogParams,
            uiContext: SyncUIContext,
        ) {
            AlertDialog
                .Builder(uiContext.context)
                .setTitle(params.titleRes)
                .setMessage(uiContext.context.getString(params.messageRes, params.userName))
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(R.string.submit) { dialog, _ ->
                    dialog.dismiss()
                    val dialogBinding = ProgressDialogBinding.inflate(uiContext.layoutInflater)
                    dialogBinding.progressDialogText.text = uiContext.context.getString(R.string.sync_in_progress)
                    activeProgressDialog =
                        AlertDialog
                            .Builder(uiContext.context)
                            .setView(dialogBinding.root)
                            .setCancelable(false)
                            .create()
                    activeProgressDialog?.show()

                    uiContext.compositeDisposable.add(
                        params
                            .syncAction()
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                { success ->
                                    dismissProgressDialog()
                                    if (!uiContext.isFragmentAdded()) return@subscribe
                                    if (success) {
                                        ViewUtil.showLongToast(
                                            uiContext.context,
                                            uiContext.context.getString(params.successMsgRes),
                                        )
                                        if (params.shouldRecreate) {
                                            uiContext.activity.recreate()
                                        }
                                    } else {
                                        ViewUtil.showLongToast(
                                            uiContext.context,
                                            uiContext.context.getString(params.failedMsgRes, ""),
                                        )
                                    }
                                },
                                { throwable ->
                                    dismissProgressDialog()
                                    if (!uiContext.isFragmentAdded()) return@subscribe
                                    val errorMsg = throwable.localizedMessage ?: ""
                                    ViewUtil.showLongToast(
                                        uiContext.context,
                                        uiContext.context.getString(params.failedMsgRes, errorMsg),
                                    )
                                },
                            ),
                    )
                }.show()
        }

        private data class SyncDialogParams(
            val titleRes: Int,
            val messageRes: Int,
            val userName: String,
            val syncAction: () -> Single<Boolean>,
            val successMsgRes: Int,
            val failedMsgRes: Int,
            val shouldRecreate: Boolean,
        )
    }
