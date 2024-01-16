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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import fr.free.nrw.commons.AboutActivity;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.databinding.FragmentMoreBottomSheetLoggedOutBinding;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.logging.CommonsLogSender;
import fr.free.nrw.commons.settings.SettingsActivity;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class MoreBottomSheetLoggedOutFragment extends BottomSheetDialogFragment {

    private FragmentMoreBottomSheetLoggedOutBinding binding;
    @Inject
    CommonsLogSender commonsLogSender;
    @Inject
    @Named("default_preferences")
    JsonKvStore applicationKvStore;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
        @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        binding = FragmentMoreBottomSheetLoggedOutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        binding.moreLogin.setOnClickListener(v -> onLogoutClicked());
        binding.moreFeedback.setOnClickListener(v -> onFeedbackClicked());
        binding.moreAbout.setOnClickListener(v -> onAboutClicked());
        binding.moreSettings.setOnClickListener(v -> onSettingsClicked());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);
        ApplicationlessInjection
            .getInstance(requireActivity().getApplicationContext())
            .getCommonsApplicationComponent()
            .inject(this);
    }

    public void onLogoutClicked() {
        applicationKvStore.putBoolean("login_skipped", false);
        final Intent intent = new Intent(getContext(), LoginActivity.class);
        requireActivity().finish();  //Kill the activity from which you will go to next activity
        startActivity(intent);
    }

    public void onFeedbackClicked() {
        showAlertDialog();
    }

    /**
     * This method shows the alert dialog when a user wants to send feedback about the app.
     */
    private void showAlertDialog() {
        new AlertDialog.Builder(requireActivity())
            .setMessage(R.string.feedback_sharing_data_alert)
            .setCancelable(false)
            .setPositiveButton(R.string.ok, (dialog, which) -> {
                sendFeedback();
            })
            .show();
    }

    /**
     * This method collects the feedback message and starts and activity with implicit intent to
     * available email client.
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

    public void onAboutClicked() {
        final Intent intent = new Intent(getActivity(), AboutActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        requireActivity().startActivity(intent);
    }

    public void onSettingsClicked() {
        final Intent intent = new Intent(getActivity(), SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        requireActivity().startActivity(intent);
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

