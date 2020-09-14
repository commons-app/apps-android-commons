package fr.free.nrw.commons.navtab;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import fr.free.nrw.commons.AboutActivity;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.achievements.AchievementsActivity;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.logging.CommonsLogSender;
import fr.free.nrw.commons.review.ReviewActivity;
import fr.free.nrw.commons.settings.SettingsActivity;
import javax.inject.Inject;
import timber.log.Timber;

public class MoreBottomSheetLoggedOutFragment extends BottomSheetDialogFragment {

    @Inject
    CommonsLogSender commonsLogSender;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
        @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.fragment_more_bottom_sheet_logged_out, container, false);
        ButterKnife.bind(this, view);
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

    @OnClick(R.id.more_login)
    public void onLogoutClicked() {
        final Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        getActivity().startActivity(intent);
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

