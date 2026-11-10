package fr.free.nrw.commons.media

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.getUserName
import fr.free.nrw.commons.media.MediaDetailFragment.Companion.NOMINATING_FOR_DELETION_MEDIA
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.ViewUtil
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.lang.String.format

/** Updates nomination controls based on the deletion request state. */
internal fun MediaDetailFragment.onDeletionPageExists(deletionPageExists: Boolean) {
    if (fileWasDeletedFromCommons) {
        binding.nominateDeletion.visibility = View.GONE
        binding.nominatedDeletionBanner.visibility = View.GONE
        return
    }
    if (getUserName(requireContext()) == null && getUserName(requireContext()) != media!!.author) {
        binding.nominateDeletion.visibility = View.GONE
        binding.nominatedDeletionBanner.visibility = View.GONE
    } else if (deletionPageExists) {
        if (applicationKvStore.getBoolean(
                String.format(NOMINATING_FOR_DELETION_MEDIA, media!!.imageUrl),
                false,
            )
        ) {
            applicationKvStore.remove(
                String.format(NOMINATING_FOR_DELETION_MEDIA, media!!.imageUrl),
            )
            binding.progressBarDeletion.visibility = View.GONE
        }
        binding.nominateDeletion.visibility = View.GONE

        binding.nominatedDeletionBanner.visibility = View.VISIBLE
    } else if (!isCategoryImage) {
        binding.nominateDeletion.visibility = View.VISIBLE
        binding.nominatedDeletionBanner.visibility = View.GONE
    }
}

/** Opens the file nomination dialog. */
@SuppressLint("StringFormatInvalid")
internal fun MediaDetailFragment.onDeleteButtonClicked() {
    if (getUserName(requireContext()) != null && getUserName(requireContext()) == media!!.author) {
        val languageAdapter: ArrayAdapter<String> =
            ArrayAdapter(
                requireActivity(),
                R.layout.simple_spinner_dropdown_list,
                reasonList,
            )
        val spinner = Spinner(activity)
        spinner.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        spinner.adapter = languageAdapter
        spinner.gravity = Gravity.CENTER

        val dialog: AlertDialog? =
            showAlertDialog(
                requireActivity(),
                getString(R.string.nominate_delete),
                null,
                getString(R.string.about_translate_proceed),
                getString(R.string.about_translate_cancel),
                { onDeleteClicked(spinner) },
                {
                    // No action needed when the user cancels
                },
                spinner,
            )
        if (isDeleted) {
            dialog!!.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        }
    } else if (getUserName(requireContext()) != null) {
        val input = EditText(activity)
        input.requestFocus()
        val d: AlertDialog? =
            showAlertDialog(
                requireActivity(),
                null,
                getString(R.string.dialog_box_text_nomination, media!!.displayTitle),
                getString(R.string.ok),
                getString(R.string.cancel),
                {
                    val reason: String = input.text.toString()
                    onDeleteClickedDialogText(reason)
                },
                {
                    // No action needed when the user cancels
                },
                input,
            )
        input.addTextChangedListener(
            object : TextWatcher {
                fun handleText() {
                    val okButton: Button = d!!.getButton(AlertDialog.BUTTON_POSITIVE)
                    if (input.text.isEmpty() || isDeleted) {
                        okButton.isEnabled = false
                    } else {
                        okButton.isEnabled = true
                    }
                }

                override fun afterTextChanged(arg0: Editable) {
                    handleText()
                }

                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int,
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence,
                    start: Int,
                    before: Int,
                    count: Int,
                ) = Unit
            },
        )
        d!!.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
    }
}

/** Submits the selected deletion reason. */
@SuppressLint("CheckResult")
internal fun MediaDetailFragment.onDeleteClicked(spinner: Spinner) {
    applicationKvStore.putBoolean(
        String.format(
            NOMINATING_FOR_DELETION_MEDIA,
            media!!.imageUrl,
        ),
        true,
    )
    enableProgressBar()
    val reason: String = reasonListEnglishMappings[spinner.selectedItemPosition]
    val finalReason: String = reason
    val resultSingle: Single<Boolean> =
        reasonBuilder
            .getReason(media, reason)
            .flatMap {
                deleteHelper.makeDeletion(
                    context,
                    media,
                    finalReason,
                )
            }
    resultSingle
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::handleDeletionResult, this::handleDeletionError)
}

/**
 * Disables Progress Bar and Update delete button text.
 */
internal fun MediaDetailFragment.disableProgressBar() {
    activity?.run {
        runOnUiThread(
            Runnable {
                binding.progressBarDeletion.visibility = View.GONE
            },
        )
    } ?: return // Prevent NullPointerException when fragment is not attached to activity
}

/** Updates the controls after the nomination completes. */
internal fun MediaDetailFragment.handleDeletionResult(success: Boolean) {
    if (success) {
        binding.nominateDeletion.text = getString(R.string.nominated_for_deletion_btn)
        ViewUtil.showLongSnackbar(requireView(), getString(R.string.nominated_for_deletion))
        disableProgressBar()
        checkAndClearDeletionFlag()
    } else {
        disableProgressBar()
    }
}

/** Restores the controls and reports a nomination failure. */
internal fun MediaDetailFragment.handleDeletionError(throwable: Throwable) {
    throwable.printStackTrace()
    disableProgressBar()
    checkAndClearDeletionFlag()
}

/** Clears the in-progress nomination flag. */
internal fun MediaDetailFragment.checkAndClearDeletionFlag() {
    if (applicationKvStore
            .getBoolean(format(NOMINATING_FOR_DELETION_MEDIA, media!!.imageUrl), false)
    ) {
        applicationKvStore.remove(format(NOMINATING_FOR_DELETION_MEDIA, media!!.imageUrl))
        callback!!.nominatingForDeletion(index)
    }
}

/** Submits a custom deletion reason. */
@SuppressLint("CheckResult")
internal fun MediaDetailFragment.onDeleteClickedDialogText(reason: String) {
    applicationKvStore.putBoolean(
        String.format(
            NOMINATING_FOR_DELETION_MEDIA,
            media!!.imageUrl,
        ),
        true,
    )
    enableProgressBar()
    val resultSingletext: Single<Boolean> =
        reasonBuilder
            .getReason(media, reason)
            .flatMap { _ ->
                deleteHelper.makeDeletion(
                    context,
                    media,
                    reason,
                )
            }
    resultSingletext
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::handleDeletionResult, this::handleDeletionError)
}

/**
 * Enable Progress Bar and Update delete button text.
 */
internal fun MediaDetailFragment.enableProgressBar() {
    binding.progressBarDeletion.visibility = View.VISIBLE
    binding.nominateDeletion.text = requireContext().getString(R.string.nominate_deletion)
    isDeleted = true
}
