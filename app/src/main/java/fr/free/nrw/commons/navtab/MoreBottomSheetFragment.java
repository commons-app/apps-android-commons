package fr.free.nrw.commons.navtab;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import fr.free.nrw.commons.AboutActivity;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.actions.PageEditClient;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.databinding.FragmentMoreBottomSheetBinding;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.feedback.FeedbackContentCreator;
import fr.free.nrw.commons.feedback.model.Feedback;
import fr.free.nrw.commons.feedback.FeedbackDialog;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.logging.CommonsLogSender;
import fr.free.nrw.commons.profile.ProfileActivity;
import fr.free.nrw.commons.review.ReviewActivity;
import fr.free.nrw.commons.settings.SettingsActivity;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class MoreBottomSheetFragment extends BottomSheetDialogFragment {

    @Inject
    CommonsLogSender commonsLogSender;

    private TextView moreProfile;

    @Inject @Named("default_preferences")
    JsonKvStore store;

    @Inject
    @Named("commons-page-edit")
    PageEditClient pageEditClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
        @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final @NonNull FragmentMoreBottomSheetBinding binding =
            FragmentMoreBottomSheetBinding.inflate(inflater, container, false);
        moreProfile = binding.moreProfile;

        if(store.getBoolean(CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED)){
            binding.morePeerReview.setVisibility(View.GONE);
        }

        binding.moreLogout.setOnClickListener(v -> onLogoutClicked());
        binding.moreFeedback.setOnClickListener(v -> onFeedbackClicked());
        binding.moreAbout.setOnClickListener(v -> onAboutClicked());
        binding.moreTutorial.setOnClickListener(v -> onTutorialClicked());
        binding.moreSettings.setOnClickListener(v -> onSettingsClicked());
        binding.moreProfile.setOnClickListener(v -> onProfileClicked());
        binding.morePeerReview.setOnClickListener(v -> onPeerReviewClicked());

        setUserName();
        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);
        ApplicationlessInjection
            .getInstance(requireActivity().getApplicationContext())
            .getCommonsApplicationComponent()
            .inject(this);
    }

    /**
     * Set the username in navigationHeader.
     */
    private void setUserName() {
        moreProfile.setText(getUserName());
    }

    private String getUserName(){
        final AccountManager accountManager = AccountManager.get(getActivity());
        final Account[] allAccounts = accountManager.getAccountsByType(BuildConfig.ACCOUNT_TYPE);
        if (allAccounts.length != 0) {
            moreProfile.setText(allAccounts[0].name);
            return allAccounts[0].name;
        }
        return "";
    }


    protected void onLogoutClicked() {
        new AlertDialog.Builder(requireActivity())
            .setMessage(R.string.logout_verification)
            .setCancelable(false)
            .setPositiveButton(R.string.yes, (dialog, which) -> {
                final CommonsApplication app = (CommonsApplication)
                    requireContext().getApplicationContext();
                app.clearApplicationData(requireContext(), new BaseLogoutListener());
            })
            .setNegativeButton(R.string.no, (dialog, which) -> dialog.cancel())
            .show();
    }

    protected void onFeedbackClicked() {
        showFeedbackDialog();
    }

    /**
     * Creates and shows a dialog asking feedback from users
     */
    private void showFeedbackDialog() {
        new FeedbackDialog(getContext(), this::uploadFeedback).show();
    }

    /**
     * uploads feedback data on the server
     */
    void uploadFeedback(final Feedback feedback) {
        final FeedbackContentCreator feedbackContentCreator = new FeedbackContentCreator(getContext(), feedback);

        Single<Boolean> single =
            pageEditClient.prependEdit("Commons:Mobile_app/Feedback", feedbackContentCreator.toString(), "Summary")
                .flatMapSingle(result -> Single.just(result))
                .firstOrError();

        Single.defer((Callable<SingleSource<Boolean>>) () -> single)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(aBoolean -> {
                if (aBoolean) {
                    Toast.makeText(getContext(), getString(R.string.thanks_feedback), Toast.LENGTH_SHORT)
                        .show();
                } else {
                    Toast.makeText(getContext(), getString(R.string.error_feedback),
                        Toast.LENGTH_SHORT).show();
                }
            });
    }

    /**
     * This method shows the alert dialog when a user wants to send feedback about the app.
     */
    private void showAlertDialog() {
        new AlertDialog.Builder(requireActivity())
            .setMessage(R.string.feedback_sharing_data_alert)
            .setCancelable(false)
            .setPositiveButton(R.string.ok, (dialog, which) -> sendFeedback())
            .show();
    }

    /**
     * This method collects the feedback message and starts the activity with implicit intent
     * to available email client.
     */
    private void sendFeedback() {
        final String technicalInfo = commonsLogSender.getExtraInfo();

        final Intent feedbackIntent = new Intent(Intent.ACTION_SENDTO);
        feedbackIntent.setType("message/rfc822");
        feedbackIntent.setData(Uri.parse("mailto:"));
        feedbackIntent.putExtra(Intent.EXTRA_EMAIL,
            new String[]{CommonsApplication.FEEDBACK_EMAIL});
        feedbackIntent.putExtra(Intent.EXTRA_SUBJECT,
            CommonsApplication.FEEDBACK_EMAIL_SUBJECT);
        feedbackIntent.putExtra(Intent.EXTRA_TEXT, String.format(
            "\n\n%s\n%s", CommonsApplication.FEEDBACK_EMAIL_TEMPLATE_HEADER, technicalInfo));
        try {
            startActivity(feedbackIntent);
        } catch (final ActivityNotFoundException e) {
            Toast.makeText(getActivity(), R.string.no_email_client, Toast.LENGTH_SHORT).show();
        }
    }

    protected void onAboutClicked() {
        final Intent intent = new Intent(getActivity(), AboutActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        requireActivity().startActivity(intent);
    }

    protected void onTutorialClicked() {
        WelcomeActivity.startYourself(getActivity());
    }

    protected void onSettingsClicked() {
        final Intent intent = new Intent(getActivity(), SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        requireActivity().startActivity(intent);
    }

    protected void onProfileClicked() {
        ProfileActivity.startYourself(getActivity(), getUserName(), false);
    }

    protected void onPeerReviewClicked() {
        ReviewActivity.startYourself(getActivity(), getString(R.string.title_activity_review));
    }

    private class BaseLogoutListener implements CommonsApplication.LogoutListener {

        @Override
        public void onLogoutComplete() {
            Timber.d("Logout complete callback received.");
            final Intent nearbyIntent = new Intent(
                getContext(), LoginActivity.class);
            nearbyIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            nearbyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(nearbyIntent);
            requireActivity().finish();
        }
    }
}

