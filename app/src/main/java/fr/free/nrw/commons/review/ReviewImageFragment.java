package fr.free.nrw.commons.review;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.auth.csrf.InvalidLoginTokenException;
import fr.free.nrw.commons.databinding.FragmentReviewImageBinding;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

public class ReviewImageFragment extends CommonsDaggerSupportFragment {

    static final int CATEGORY = 2;
    private static final int SPAM = 0;
    private static final int COPYRIGHT = 1;
    private static final int THANKS = 3;

    private int position;

    private FragmentReviewImageBinding binding;

    @Inject
    SessionManager sessionManager;


    // Constant variable used to store user's key name for onSaveInstanceState method
    private final String SAVED_USER = "saved_user";

    // Variable that stores the value of user
    private String user;

    public void update(final int position) {
        this.position = position;
    }

    private String updateCategoriesQuestion() {
        final Media media = getReviewActivity().getMedia();
        if (media != null && media.getCategoriesHiddenStatus() != null && isAdded()) {
            // Filter category name attribute from all categories
            final List<String> categories = new ArrayList<>();
            for(final String key : media.getCategoriesHiddenStatus().keySet()) {
                String value = String.valueOf(key);
                // Each category returned has a format like "Category:<some-category-name>"
                // so remove the prefix "Category:"
                final int index = key.indexOf("Category:");
                if(index == 0) {
                    value = key.substring(9);
                }
                categories.add(value);
            }
            String catString = TextUtils.join(", ", categories);
            if (catString != null && !catString.equals("") && binding.tvReviewQuestionContext != null) {
                catString = "<b>" + catString + "</b>";
                final String stringToConvertHtml = String.format(getResources().getString(R.string.review_category_explanation), catString);
                return Html.fromHtml(stringToConvertHtml).toString();
            }
        }
        return getResources().getString(R.string.review_no_category);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        position = getArguments().getInt("position");
        binding = FragmentReviewImageBinding.inflate(inflater, container, false);

        final String question;
        String explanation=null;
        String yesButtonText;
        final String noButtonText;

        binding.buttonYes.setOnClickListener(view -> onYesButtonClicked());

        switch (position) {
            case SPAM:
                question = getString(R.string.review_spam);
                explanation = getString(R.string.review_spam_explanation);
                yesButtonText = getString(R.string.yes);
                noButtonText = getString(R.string.no);
                binding.buttonNo.setOnClickListener(view -> getReviewActivity()
                        .reviewController.reportSpam(requireActivity(), getReviewCallback()));
                break;
            case COPYRIGHT:
                enableButtons();
                question = getString(R.string.review_copyright);
                explanation = getString(R.string.review_copyright_explanation);
                yesButtonText = getString(R.string.yes);
                noButtonText = getString(R.string.no);
                binding.buttonNo.setOnClickListener(view -> getReviewActivity()
                        .reviewController
                        .reportPossibleCopyRightViolation(requireActivity(), getReviewCallback()));
                break;
            case CATEGORY:
                enableButtons();
                question = getString(R.string.review_category);
                explanation = updateCategoriesQuestion();
                yesButtonText = getString(R.string.yes);
                noButtonText = getString(R.string.no);
                binding.buttonNo.setOnClickListener(view -> {
                    getReviewActivity()
                            .reviewController
                            .reportWrongCategory(requireActivity(), getReviewCallback());
                    getReviewActivity().swipeToNext();
                });
                break;
            case THANKS:
                enableButtons();
                question = getString(R.string.review_thanks);

                if (getReviewActivity().reviewController.firstRevision != null) {
                    user = getReviewActivity().reviewController.firstRevision.getUser();
                } else {
                    if(savedInstanceState != null) {
                        user = savedInstanceState.getString(SAVED_USER);
                    }
                }

                //if the user is null because of whatsoever reason, review will not be sent anyways
                if (!TextUtils.isEmpty(user)) {
                    explanation = getString(R.string.review_thanks_explanation, user);
                }

                // Note that the yes and no buttons are swapped in this section
                yesButtonText = getString(R.string.review_thanks_yes_button_text);
                noButtonText = getString(R.string.review_thanks_no_button_text);
                binding.buttonYes.setTextColor(Color.parseColor("#116aaa"));
                binding.buttonNo.setTextColor(Color.parseColor("#228b22"));
                binding.buttonNo.setOnClickListener(view -> {
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

        binding.tvReviewQuestion.setText(question);
        binding.tvReviewQuestionContext.setText(explanation);
        binding.buttonYes.setText(yesButtonText);
        binding.buttonNo.setText(noButtonText);
        return binding.getRoot();
    }


    /**
     * This method will be called when configuration changes happen
     *
     * @param outState
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save user name when configuration changes happen
        outState.putString(SAVED_USER, user);
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

            @Override
            public void onTokenException(final Exception e) {
                if (e instanceof InvalidLoginTokenException){
                    final String username = sessionManager.getUserName();
                    final CommonsApplication.BaseLogoutListener logoutListener = new CommonsApplication.BaseLogoutListener(
                        getActivity(),
                        requireActivity().getString(R.string.invalid_login_message),
                        username
                    );

                    CommonsApplication.getInstance().clearApplicationData(
                        requireActivity(), logoutListener);

                }
            }

            /**
             * This function is called when an image is being loaded
             * to disable the review buttons
             */
            @Override
            public void disableButtons() {
                ReviewImageFragment.this.disableButtons();
            }

            /**
             * This function is called when an image has
             * been loaded to enable the review buttons.
             */
            @Override
            public void enableButtons() {
                ReviewImageFragment.this.enableButtons();
            }
        };
    }

    /**
     * This function is called when an image has
     * been loaded to enable the review buttons.
     */
    public void enableButtons() {
        binding.buttonYes.setEnabled(true);
        binding.buttonYes.setAlpha(1);
        binding.buttonNo.setEnabled(true);
        binding.buttonNo.setAlpha(1);
    }

    /**
     * This function is called when an image is being loaded
     * to disable the review buttons
     */
    public void disableButtons() {
        binding.buttonYes.setEnabled(false);
        binding.buttonYes.setAlpha(0.5f);
        binding.buttonNo.setEnabled(false);
        binding.buttonNo.setAlpha(0.5f);
    }

    void onYesButtonClicked() {
        getReviewActivity().swipeToNext();
    }

    private ReviewActivity getReviewActivity() {
        return (ReviewActivity) requireActivity();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
