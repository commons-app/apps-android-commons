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

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;

public class ReviewImageFragment extends CommonsDaggerSupportFragment {

    static final int CATEGORY = 2;
    private static final int SPAM = 0;
    private static final int COPYRIGHT = 1;
    private static final int THANKS = 3;

    private int position;

    public ProgressBar progressBar;

    @BindView(R.id.tv_review_question)
    TextView textViewQuestion;
    @BindView(R.id.tv_review_question_context)
    TextView textViewQuestionContext;
    @BindView(R.id.button_yes)
    Button yesButton;
    @BindView(R.id.button_no)
    Button noButton;

    public void update(int position) {
        this.position = position;
    }

    private String updateCategoriesQuestion() {
        Media media = getReviewActivity().getMedia();
        if (media != null && media.getCategories() != null && isAdded()) {
            String catString = TextUtils.join(", ", media.getCategories());
            if (catString != null && !catString.equals("") && textViewQuestionContext != null) {
                catString = "<b>" + catString + "</b>";
                String stringToConvertHtml = String.format(getResources().getString(R.string.review_category_explanation), catString);
                return Html.fromHtml(stringToConvertHtml).toString();
            }
        }
        return getResources().getString(R.string.review_no_category);
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
        ButterKnife.bind(this,layoutView);

        String question, explanation, yesButtonText, noButtonText;
        switch (position) {
            case SPAM:
                question = getString(R.string.review_spam);
                explanation = getString(R.string.review_spam_explanation);
                yesButtonText = getString(R.string.review_spam_yes_button_text);
                noButtonText = getString(R.string.review_spam_no_button_text);
                yesButton.setOnClickListener(view -> getReviewActivity()
                        .reviewController.reportSpam(requireActivity(), getReviewCallback()));
                break;
            case COPYRIGHT:
                enableButtons();
                question = getString(R.string.review_copyright);
                explanation = getString(R.string.review_copyright_explanation);
                yesButtonText = getString(R.string.review_copyright_yes_button_text);
                noButtonText = getString(R.string.review_copyright_no_button_text);
                yesButton.setOnClickListener(view -> getReviewActivity()
                        .reviewController
                        .reportPossibleCopyRightViolation(requireActivity(), getReviewCallback()));
                break;
            case CATEGORY:
                enableButtons();
                question = getString(R.string.review_category);
                explanation = updateCategoriesQuestion();
                yesButtonText = getString(R.string.review_category_yes_button_text);
                noButtonText = getString(R.string.review_category_no_button_text);
                yesButton.setOnClickListener(view -> {
                    getReviewActivity()
                            .reviewController
                            .reportWrongCategory(requireActivity(), getReviewCallback());
                    getReviewActivity().swipeToNext();
                });
                break;
            case THANKS:
                enableButtons();
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
                enableButtons();
                question = "How did we get here?";
                explanation = "No idea.";
                yesButtonText = "yes";
                noButtonText = "no";
        }

        textViewQuestion.setText(question);
        textViewQuestionContext.setText(explanation);
        yesButton.setText(yesButtonText);
        noButton.setText(noButtonText);
        return layoutView;
    }

    private ReviewController.ReviewCallback getReviewCallback() {
        return new ReviewController
                .ReviewCallback() {
            @Override
            public void onSuccess() {
                getReviewActivity().runRandomizer();
            }

            @Override
            public void onFailure() {
                //do nothing
            }
        };
    }

    public void enableButtons() {
        yesButton.setEnabled(true);
        yesButton.setAlpha(1);
        noButton.setEnabled(true);
        noButton.setAlpha(1);
    }

    public void disableButtons() {
        yesButton.setEnabled(false);
        yesButton.setAlpha(0.5f);
        noButton.setEnabled(false);
        noButton.setAlpha(0.5f);
    }

    @OnClick(R.id.button_no)
    void onNoButtonClicked() {
        getReviewActivity().swipeToNext();
    }

    private ReviewActivity getReviewActivity() {
        return (ReviewActivity) requireActivity();
    }
}
