package fr.free.nrw.commons.contributions

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import fr.free.nrw.commons.R
import fr.free.nrw.commons.Utils
import fr.free.nrw.commons.upload.SimilarImageDialogFragment
import kotlinx.android.synthetic.main.dialog_add_to_wikipedia_instructions.*
import org.wikipedia.dataclient.WikiSite
import javax.inject.Inject

class WikipedaiInstructionsDialogFragment : DialogFragment() {

    var contribution: Contribution? = null
    var callback: Callback? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_add_to_wikipedia_instructions, container)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        contribution = arguments!!.getParcelable(ARG_CONTRIBUTION)
        tv_wikicode.setText(contribution?.wikiCode)
        instructions_cancel.setOnClickListener {
            dismiss()
        }

        instructions_confirm.setOnClickListener {
            callback?.onConfirmClicked(contribution, checkbox_copy_wikicode.isChecked)
        }

        dialog!!.window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )
    }

    interface Callback {
        fun onConfirmClicked(contribution: Contribution?, copyWikicode: Boolean)
    }

    companion object {

        val ARG_CONTRIBUTION = "contribution"

        @JvmStatic
        fun newInstance(contribution: Contribution): WikipedaiInstructionsDialogFragment {
            val frag = WikipedaiInstructionsDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_CONTRIBUTION, contribution)
            frag.arguments = args
            return frag
        }
    }
}