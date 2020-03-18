package fr.free.nrw.commons.upload;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.R;

/**
 * Created by harisanker on 14/2/18.
 */

public class SimilarImageDialogFragment extends DialogFragment {

    @BindView(R.id.orginalImage)
    SimpleDraweeView originalImage;
    @BindView(R.id.possibleImage)
    SimpleDraweeView possibleImage;
    @BindView(R.id.postive_button)
    Button positiveButton;
    @BindView(R.id.negative_button)
    Button negativeButton;
    Callback callback;//Implemented interface from shareActivity
    Boolean gotResponse = false;

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
        View view =  inflater.inflate(R.layout.fragment_similar_image_dialog, container, false);
        ButterKnife.bind(this,view);

        originalImage.setHierarchy(GenericDraweeHierarchyBuilder
                .newInstance(getResources())
                .setPlaceholderImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_image_black_24dp,getContext().getTheme()))
                .setFailureImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_error_outline_black_24dp, getContext().getTheme()))
                .build());
        possibleImage.setHierarchy(GenericDraweeHierarchyBuilder
                .newInstance(getResources())
                .setPlaceholderImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_image_black_24dp,getContext().getTheme()))
                .setFailureImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_error_outline_black_24dp, getContext().getTheme()))
                .build());

        originalImage.setImageURI(Uri.fromFile(new File(getArguments().getString("originalImagePath"))));
        possibleImage.setImageURI(Uri.fromFile(new File(getArguments().getString("possibleImagePath"))));

        return view;
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

    @OnClick(R.id.negative_button)
    public void onNegativeButtonClicked() {
        callback.onNegativeResponse();
        gotResponse = true;
        dismiss();
    }

    @OnClick(R.id.postive_button)
    public void onPositiveButtonClicked() {
        callback.onPositiveResponse();
        gotResponse = true;
        dismiss();
    }
}
