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
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import fr.free.nrw.commons.AboutActivity;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.logging.CommonsLogSender;
import fr.free.nrw.commons.profile.ProfileActivity;
import fr.free.nrw.commons.review.ReviewActivity;
import fr.free.nrw.commons.settings.SettingsActivity;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class MoreBottomSheetFragment extends BottomSheetDialogFragment {

    @Inject
    CommonsLogSender commonsLogSender;
    @BindView(R.id.more_profile)
    TextView moreProfile;

    @BindView((R.id.more_peer_review)) TextView morePeerReview;

    @Inject @Named("default_preferences")
    JsonKvStore store;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
        @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.fragment_more_bottom_sheet, container, false);
        ButterKnife.bind(this, view);
        if(store.getBoolean(CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED)){
            morePeerReview.setVisibility(View.GONE);
        }
        setUserName();
        return view;
    }

    @Override
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);
        ApplicationlessInjection
            .getInstance(getActivity().getApplicationContext())
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


    @OnClick(R.id.more_logout)
    public void onLogoutClicked() {
        new AlertDialog.Builder(getActivity())
            .setMessage(R.string.logout_verification)
            .setCancelable(false)
            .setPositiveButton(R.string.yes, (dialog, which) -> {
                BaseLogoutListener logoutListener = new BaseLogoutListener();
                CommonsApplication app = (CommonsApplication) getContext().getApplicationContext();
                app.clearApplicationData(getContext(), logoutListener);
            })
            .setNegativeButton(R.string.no, (dialog, which) -> dialog.cancel())
            .show();
    }

    @OnClick(R.id.more_feedback)
    public void onFeedbackClicked() {
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

    @OnClick(R.id.more_about)
    public void onAboutClicked() {
        final Intent intent = new Intent(getActivity(), AboutActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        getActivity().startActivity(intent);
    }

    @OnClick(R.id.more_tutorial)
    public void onTutorialClicked() {
        WelcomeActivity.startYourself(getActivity());
    }

    @OnClick(R.id.more_settings)
    public void onSettingsClicked() {
        final Intent intent = new Intent(getActivity(), SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        getActivity().startActivity(intent);
    }

    @OnClick(R.id.more_profile)
    public void onProfileClicked() {
        ProfileActivity.startYourself(getActivity(), getUserName(), false);
    }

    @OnClick(R.id.more_peer_review)
    public void onPeerReviewClicked() {
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
            getActivity().finish();
        }
    }
}

