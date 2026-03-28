package fr.free.nrw.commons.upload

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import coil.load
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.FragmentSimilarImageDialogBinding
import java.io.File

/**
 * Created by harisanker on 14/2/18.
 */
class SimilarImageDialogFragment : DialogFragment() {
    var callback: Callback? = null //Implemented interface from shareActivity
    var gotResponse: Boolean = false

    private var _binding: FragmentSimilarImageDialogBinding? = null
    private val binding: FragmentSimilarImageDialogBinding get() = _binding!!

    interface Callback {
        fun onPositiveResponse()

        fun onNegativeResponse()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSimilarImageDialogBinding.inflate(inflater, container, false)

        arguments?.let {
            binding.orginalImage.load(File(it.getString("originalImagePath")!!)) {
                placeholder(R.drawable.ic_image_black_24dp)
                error(R.drawable.ic_error_outline_black_24dp)
            }
            binding.possibleImage.load(File(it.getString("possibleImagePath")!!)) {
                placeholder(R.drawable.ic_image_black_24dp)
                error(R.drawable.ic_error_outline_black_24dp)
            }
        }

        binding.postiveButton.setOnClickListener {
            callback?.onPositiveResponse()
            gotResponse = true
            dismiss()
        }

        binding.negativeButton.setOnClickListener {
            callback?.onNegativeResponse()
            gotResponse = true
            dismiss()
        }

        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        // I user dismisses dialog by pressing outside the dialog.
        if (!gotResponse) {
            callback?.onNegativeResponse()
        }
        super.onDismiss(dialog)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
