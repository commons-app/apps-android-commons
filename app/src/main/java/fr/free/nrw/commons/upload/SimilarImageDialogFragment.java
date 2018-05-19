package fr.free.nrw.commons.upload;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.listener.RequestLoggingListener;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

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
    onResponse mOnResponse;//Implemented interface from shareActivity
    Boolean gotResponse = false;

    public SimilarImageDialogFragment() {
    }
    public interface onResponse{
        public void onPositiveResponse();

        public void onNegativeResponse();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_similar_image_dialog, container, false);
        ButterKnife.bind(this,view);
        Set<RequestListener> requestListeners = new HashSet<>();
        requestListeners.add(new RequestLoggingListener());

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
        mOnResponse = (onResponse) getActivity();//Interface Implementation
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
            mOnResponse.onNegativeResponse();
        }
        super.onDismiss(dialog);
    }

    @OnClick(R.id.negative_button)
    public void onNegativeButtonClicked() {
        mOnResponse.onNegativeResponse();
        gotResponse = true;
        dismiss();
    }

    @OnClick(R.id.postive_button)
    public void onPositiveButtonClicked() {
        mOnResponse.onPositiveResponse();
        gotResponse = true;
        dismiss();
    }
}
