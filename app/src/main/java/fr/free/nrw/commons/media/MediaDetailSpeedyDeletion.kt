package fr.free.nrw.commons.media

import android.annotation.SuppressLint
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import fr.free.nrw.commons.R
import fr.free.nrw.commons.delete.FileInUseException
import fr.free.nrw.commons.delete.FileUsageUnknownException
import fr.free.nrw.commons.media.MediaDetailFragment.Companion.NOMINATING_FOR_DELETION_MEDIA
import fr.free.nrw.commons.utils.LangCodeUtils.getLocalizedResources
import fr.free.nrw.commons.utils.ViewUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.Locale

/**
 * Creates the list of deletion reasons for the user's own upload. A recent upload can be deleted
 * quickly under criterion G7, which is then offered first. Whether the file is used anywhere is
 * only known after asking Commons, when the reason is submitted.
 *
 * @param offerSpeedyDeletion whether to put the G7 reason above the regular reasons
 */
internal fun MediaDetailFragment.ownUploadReasonSpinner(offerSpeedyDeletion: Boolean): Spinner {
    val reasons =
        if (offerSpeedyDeletion) {
            listOf(getString(R.string.deletion_reason_speedy_g7)) + reasonList
        } else {
            reasonList
        }
    val spinner = Spinner(activity)
    spinner.layoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
    spinner.adapter = ArrayAdapter(requireActivity(), R.layout.simple_spinner_dropdown_list, reasons)
    spinner.gravity = Gravity.CENTER
    return spinner
}

/**
 * Requests the speedy deletion of the user's own recent upload under criterion G7. The reason on
 * the file page is written in English, like the reasons of regular deletion requests.
 */
@SuppressLint("CheckResult")
internal fun MediaDetailFragment.onSpeedyDeleteClicked() {
    val currentMedia = media ?: return
    applicationKvStore.putBoolean(
        String.format(NOMINATING_FOR_DELETION_MEDIA, currentMedia.imageUrl),
        true,
    )
    enableProgressBar()
    val reason =
        getLocalizedResources(requireContext(), Locale.ENGLISH)
            .getString(R.string.deletion_reason_speedy_g7)
    speedyDeletionHelper
        .requestDeletion(currentMedia, reason)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::handleDeletionResult, this::handleSpeedyDeletionError)
}

/**
 * Tells the user why the speedy deletion was not requested, then restores the controls
 */
internal fun MediaDetailFragment.handleSpeedyDeletionError(throwable: Throwable) {
    val message =
        when (throwable) {
            is FileInUseException -> R.string.speedy_deletion_file_in_use
            is FileUsageUnknownException -> R.string.speedy_deletion_usage_unknown
            else -> null
        }
    if (message != null && context != null) {
        ViewUtil.showLongToast(requireContext(), getString(message))
    }
    handleDeletionError(throwable)
}
