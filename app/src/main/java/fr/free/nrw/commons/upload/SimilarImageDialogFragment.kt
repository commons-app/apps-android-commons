package fr.free.nrw.commons.upload

import android.app.Dialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
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

        binding.orginalImage.hierarchy =
            GenericDraweeHierarchyBuilder.newInstance(resources).setPlaceholderImage(
                VectorDrawableCompat.create(
                    resources, R.drawable.ic_image_black_24dp, requireContext().theme
                )
            ).setFailureImage(
                VectorDrawableCompat.create(
                    resources, R.drawable.ic_error_outline_black_24dp, requireContext().theme
                )
            ).build()

        binding.possibleImage.hierarchy =
            GenericDraweeHierarchyBuilder.newInstance(resources).setPlaceholderImage(
                VectorDrawableCompat.create(
                    resources, R.drawable.ic_image_black_24dp, requireContext().theme
                )
            ).setFailureImage(
                VectorDrawableCompat.create(
                    resources, R.drawable.ic_error_outline_black_24dp, requireContext().theme
                )
            ).build()

        arguments?.let {
            binding.orginalImage.setImageURI(
                Uri.fromFile(File(it.getString("originalImagePath")!!))
            )
            binding.possibleImage.setImageURI(
                Uri.fromFile(File(it.getString("possibleImagePath")!!))
            )
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
