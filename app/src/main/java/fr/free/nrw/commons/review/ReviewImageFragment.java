package fr.free.nrw.commons.review;

import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;

/**
 * Created by root on 19.05.2018.
 */

public class ReviewImageFragment extends CommonsDaggerSupportFragment {

        public static final int SPAM = 0;
        public static final int COPYRIGHT = 1;
        public static final int CATEGORY = 2;
        public static final int THANKS = 3;

        private int position;
        private String fileName;
        private String catString;

        private View textViewQuestionContext;
        private View imageCaption;
        private View textViewQuestion;
        private SimpleDraweeView simpleDraweeView;

        private Button yesButton;
        private Button noButton;

        public ProgressBar progressBar;


        public void update(int position, String fileName) {
            this.position = position;
            this.fileName = fileName;

            if (simpleDraweeView!=null) {
                simpleDraweeView.setImageURI(Utils.makeThumbBaseUrl(fileName));
                progressBar.setVisibility(View.GONE);
            }
        }

        public void updateCategories(Iterable<String> categories) {
            if (categories!=null && isAdded()) {
                catString = TextUtils.join(", ", categories);
                if (catString != null && !catString.equals("") && textViewQuestionContext != null) {
                    catString = "<b>"+catString+"</b>";
                    String stringToConvertHtml = String.format(getResources().getString(R.string.review_category_explanation), catString);
                    ((TextView) textViewQuestionContext).setText(Html.fromHtml(stringToConvertHtml));
                } else if (textViewQuestionContext != null) {
                    ((TextView)textViewQuestionContext).setText(getResources().getString(R.string.review_no_category));
                }
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            position = getArguments().getInt("position");
            View layoutView = inflater.inflate(R.layout.fragment_review_image, container,
                    false);
            progressBar = layoutView.findViewById(R.id.progressBar);
            textViewQuestion = layoutView.findViewById(R.id.reviewQuestion);
            textViewQuestionContext = layoutView.findViewById(R.id.reviewQuestionContext);
            imageCaption = layoutView.findViewById(R.id.imageCaption);
            yesButton = layoutView.findViewById(R.id.yesButton);
            noButton = layoutView.findViewById(R.id.noButton);
            String question, explanation;
            switch(position) {
                case COPYRIGHT:
                    question = getString(R.string.review_copyright);
                    explanation = getString(R.string.review_copyright_explanation);
                    yesButton.setOnClickListener(view -> {
                        ((ReviewActivity)getActivity()).reviewController.reportPossibleCopyRightViolation();
                    });
                    break;
                case CATEGORY:
                    question = getString(R.string.review_category);
                    explanation = getString(R.string.review_no_category);
                    updateCategories(ReviewController.categories);
                    yesButton.setOnClickListener(view -> {
                        ((ReviewActivity)getActivity()).reviewController.reportWrongCategory();
                    });
                    break;
                case SPAM:
                    question = getString(R.string.review_spam);
                    explanation = getString(R.string.review_spam_explanation);
                    yesButton.setOnClickListener(view -> {
                        ((ReviewActivity)getActivity()).reviewController.reportSpam();
                    });
                    break;
                case THANKS:
                    question = getString(R.string.review_thanks);
                    explanation = getString(R.string.review_thanks_explanation);
                    yesButton.setOnClickListener(view -> {
                        ((ReviewActivity)getActivity()).reviewController.sendThanks();
                    });
                    break;
                default:
                    question = "How did we get here?";
                    explanation = "No idea.";
            }

            noButton.setOnClickListener(view -> {
                ((ReviewActivity)getActivity()).reviewController.swipeToNext();
            });

            ((TextView) textViewQuestion).setText(question);
            ((TextView) textViewQuestionContext).setText(explanation);

            simpleDraweeView = layoutView.findViewById(R.id.imageView);

            if (fileName != null) {
                simpleDraweeView.setImageURI(Utils.makeThumbBaseUrl(fileName));
                progressBar.setVisibility(View.GONE);
            }
            return layoutView;
        }

        public void updateImageCaption() {
            ((TextView)imageCaption).setText(fileName+" is uploaded by: "+ReviewController.firstRevision.username);
        }
}
