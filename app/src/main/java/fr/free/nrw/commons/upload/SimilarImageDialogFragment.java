package fr.free.nrw.commons.upload;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.databinding.FragmentSimilarImageDialogBinding;
import java.io.File;

/**
 * Created by harisanker on 14/2/18.
 */

public class SimilarImageDialogFragment extends DialogFragment {

    Callback callback;//Implemented interface from shareActivity
    Boolean gotResponse = false;

    private FragmentSimilarImageDialogBinding binding;

    public SimilarImageDialogFragment() {
    }
    public interface Callback {
        void onPositiveResponse();

        void onNegativeResponse();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSimilarImageDialogBinding.inflate(inflater, container, false);


        binding.orginalImage.setHierarchy(GenericDraweeHierarchyBuilder
                .newInstance(getResources())
                .setPlaceholderImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_image_black_24dp,getContext().getTheme()))
                .setFailureImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_error_outline_black_24dp, getContext().getTheme()))
                .build());
        binding.possibleImage.setHierarchy(GenericDraweeHierarchyBuilder
                .newInstance(getResources())
                .setPlaceholderImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_image_black_24dp,getContext().getTheme()))
                .setFailureImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_error_outline_black_24dp, getContext().getTheme()))
                .build());

        binding.orginalImage.setImageURI(Uri.fromFile(new File(getArguments().getString("originalImagePath"))));
        binding.possibleImage.setImageURI(Uri.fromFile(new File(getArguments().getString("possibleImagePath"))));

        binding.postiveButton.setOnClickListener(v -> onPositiveButtonClicked());
        binding.negativeButton.setOnClickListener(v -> onNegativeButtonClicked());

        return binding.getRoot();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
//        I user dismisses dialog by pressing outside the dialog.
        if (!gotResponse) {
            callback.onNegativeResponse();
        }
        super.onDismiss(dialog);
    }

    public void onNegativeButtonClicked() {
        callback.onNegativeResponse();
        gotResponse = true;
        dismiss();
    }

    public void onPositiveButtonClicked() {
        callback.onPositiveResponse();
        gotResponse = true;
        dismiss();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
