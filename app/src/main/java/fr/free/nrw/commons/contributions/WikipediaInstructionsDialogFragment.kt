package fr.free.nrw.commons.contributions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import fr.free.nrw.commons.databinding.DialogAddToWikipediaInstructionsBinding

/**
 * Dialog fragment for displaying instructions for editing wikipedia
 */
class WikipediaInstructionsDialogFragment : DialogFragment() {

    var callback: Callback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = DialogAddToWikipediaInstructionsBinding.inflate(inflater, container, false).apply {
        val contribution: Contribution? = arguments!!.getParcelable(ARG_CONTRIBUTION)
        tvWikicode.setText(contribution?.media?.wikiCode)
        instructionsCancel.setOnClickListener { dismiss() }
        instructionsConfirm.setOnClickListener {
            callback?.onConfirmClicked(contribution, checkboxCopyWikicode.isChecked)
        }
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog!!.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )
    }

    /**
     * Callback for handling confirm button clicked
     */
    interface Callback {
        fun onConfirmClicked(contribution: Contribution?, copyWikicode: Boolean)
    }

    companion object {
        const val ARG_CONTRIBUTION = "contribution"

        @JvmStatic
        fun newInstance(contribution: Contribution) = WikipediaInstructionsDialogFragment().apply {
            arguments = bundleOf(ARG_CONTRIBUTION to contribution)
        }
    }
}
