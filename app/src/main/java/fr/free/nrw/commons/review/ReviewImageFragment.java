package fr.free.nrw.commons.review;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wikipedia.dataclient.mwapi.MwQueryPage;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;

public class ReviewImageFragment extends CommonsDaggerSupportFragment {

    public static final int SPAM = 0;
    public static final int COPYRIGHT = 1;
    public static final int CATEGORY = 2;
    public static final int THANKS = 3;

    private int position;
    private String fileName;
    private String catString;

    private View textViewQuestionContext;
    private View textViewQuestion;

    private Button yesButton;
    private Button noButton;


    public ProgressBar progressBar;
    private MwQueryPage.Revision revision;


    public void update(int position, String fileName) {
        this.position = position;
        this.fileName = fileName;

    }

    public void updateCategories(Iterable<String> categories) {
        if (categories != null && isAdded()) {
            catString = TextUtils.join(", ", categories);
            if (catString != null && !catString.equals("") && textViewQuestionContext != null) {
                catString = "<b>" + catString + "</b>";
                String stringToConvertHtml = String.format(getResources().getString(R.string.review_category_explanation), catString);
                ((TextView) textViewQuestionContext).setText(Html.fromHtml(stringToConvertHtml));
            } else if (textViewQuestionContext != null) {
                ((TextView) textViewQuestionContext).setText(getResources().getString(R.string.review_no_category));
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
        textViewQuestion = layoutView.findViewById(R.id.reviewQuestion);
        textViewQuestionContext = layoutView.findViewById(R.id.reviewQuestionContext);
        yesButton = layoutView.findViewById(R.id.yesButton);
        noButton = layoutView.findViewById(R.id.noButton);

        String question, explanation, yesButtonText, noButtonText;
        switch (position) {
            case COPYRIGHT:
                question = getString(R.string.review_copyright);
                explanation = getString(R.string.review_copyright_explanation);
                yesButtonText = getString(R.string.review_copyright_yes_button_text);
                noButtonText = getString(R.string.review_copyright_no_button_text);
                yesButton.setOnClickListener(view -> getReviewActivity().reviewController.reportPossibleCopyRightViolation(requireActivity()));
                break;
            case CATEGORY:
                question = getString(R.string.review_category);
                explanation = getString(R.string.review_no_category);
                yesButtonText = getString(R.string.review_category_yes_button_text);
                noButtonText = getString(R.string.review_category_no_button_text);
                yesButton.setOnClickListener(view -> {
                    getReviewActivity().reviewController.reportWrongCategory(requireActivity());
                    getReviewActivity().swipeToNext();
                });
                break;
            case SPAM:
                question = getString(R.string.review_spam);
                explanation = getString(R.string.review_spam_explanation);
                yesButtonText = getString(R.string.review_spam_yes_button_text);
                noButtonText = getString(R.string.review_spam_no_button_text);
                yesButton.setOnClickListener(view -> getReviewActivity().reviewController.reportSpam(requireActivity()));
                break;
            case THANKS:
                question = getString(R.string.review_thanks);
                explanation = getString(R.string.review_thanks_explanation, getReviewActivity().reviewController.firstRevision.getUser());
                yesButtonText = getString(R.string.review_thanks_yes_button_text);
                noButtonText = getString(R.string.review_thanks_no_button_text);
                yesButton.setTextColor(Color.parseColor("#228b22"));
                noButton.setTextColor(Color.parseColor("#116aaa"));
                yesButton.setOnClickListener(view -> {
                    getReviewActivity().reviewController.sendThanks(getReviewActivity());
                    getReviewActivity().swipeToNext();
                });
                break;
            default:
                question = "How did we get here?";
                explanation = "No idea.";
                yesButtonText = "yes";
                noButtonText = "no";
        }

        noButton.setOnClickListener(view -> getReviewActivity().swipeToNext());

        ((TextView) textViewQuestion).setText(question);
        ((TextView) textViewQuestionContext).setText(explanation);
        yesButton.setText(yesButtonText);
        noButton.setText(noButtonText);

        if (position == CATEGORY) {
            updateCategories(ReviewController.categories);
        }

        return layoutView;
    }

    private ReviewActivity getReviewActivity() {
        return (ReviewActivity) requireActivity();
    }
}
