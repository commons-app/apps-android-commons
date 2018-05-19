package fr.free.nrw.commons.review;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;

/**
 * Created by nes on 19.05.2018.
 */

public class ReviewLicenceViolationFragment extends CommonsDaggerSupportFragment {
    int position;
    String fileName;

    static ReviewLicenceViolationFragment init(int val, String fileName) {
        ReviewLicenceViolationFragment fragment = new ReviewLicenceViolationFragment();
        // Supply val input as an argument.
        Bundle args = new Bundle();
        args.putInt("val", val);
        args.putString("fileName", fileName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        position = getArguments() != null ? getArguments().getInt("val") : 1;
        fileName = getArguments() != null ? getArguments().getString("fileName") : "";

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layoutView = inflater.inflate(R.layout.review_out_of_context, container,
                false);
        View textView = layoutView.findViewById(R.id.testingText);

        if (fileName!= null) {
            SimpleDraweeView simpleDraweeView = layoutView.findViewById(R.id.imageView);
            simpleDraweeView.setImageURI(Utils.makeThumbBaseUrl(fileName));
        }

        ((TextView) textView).setText("Fragment #" + position);
        Log.d("deneme","Fragment #" + position);
        return layoutView;
    }
}
