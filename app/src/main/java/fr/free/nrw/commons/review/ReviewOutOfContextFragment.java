package fr.free.nrw.commons.review;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;

/**
 * Created by root on 19.05.2018.
 */

public class ReviewOutOfContextFragment extends CommonsDaggerSupportFragment {

        int position;

        static ReviewOutOfContextFragment init(int val) {
            ReviewOutOfContextFragment truitonFrag = new ReviewOutOfContextFragment();
            // Supply val input as an argument.
            Bundle args = new Bundle();
            args.putInt("val", val);
            truitonFrag.setArguments(args);
            return truitonFrag;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            position = getArguments() != null ? getArguments().getInt("val") : 1;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View layoutView = inflater.inflate(R.layout.out_of_context_question_layout, container,
                    false);
            View textView = layoutView.findViewById(R.id.testingText);
            ((TextView) textView).setText("Fragment #" + position);
            Log.d("deneme","Fragment #" + position);
            return layoutView;
        }
}
