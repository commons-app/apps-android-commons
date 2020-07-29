package fr.free.nrw.commons.navtab;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.WelcomeActivity;
import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.achievements.AchievementsActivity;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.category.CategoryImagesActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.logging.CommonsLogSender;
import fr.free.nrw.commons.review.ReviewActivity;
import fr.free.nrw.commons.settings.SettingsActivity;
import timber.log.Timber;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MoreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MoreFragment extends CommonsDaggerSupportFragment {

  @Inject
  CommonsLogSender commonsLogSender;

  public MoreFragment() {
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @return A new instance of fragment MoreFragment.
   */
  // TODO: Rename and change types and number of parameters
  public static MoreFragment newInstance() {
    MoreFragment fragment = new MoreFragment();
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_more, container, false);
    ButterKnife.bind(this, view);
    return view;
  }

  @Override
  public void onDetach() {
    super.onDetach();
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
    String technicalInfo = commonsLogSender.getExtraInfo();

    Intent feedbackIntent = new Intent(Intent.ACTION_SENDTO);
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
    } catch (ActivityNotFoundException e) {
      Toast.makeText(getActivity(), R.string.no_email_client, Toast.LENGTH_SHORT).show();
    }
  }

  @OnClick(R.id.more_about)
  public void onAboutClicked() {
    Intent intent = new Intent(getActivity(), CategoryImagesActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    getActivity().startActivity(intent);
  }

  @OnClick(R.id.more_tutorial)
  public void onTutorialClicked() {
    WelcomeActivity.startYourself(getActivity());
  }

  @OnClick(R.id.more_settings)
  public void onSettingsClicked() {
    Intent intent = new Intent(getActivity(), SettingsActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    getActivity().startActivity(intent);
  }

  @OnClick(R.id.more_achievements)
  public void onAchievementsClicked() {
    Intent intent = new Intent(getActivity(), AchievementsActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    getActivity().startActivity(intent);
  }

  @OnClick(R.id.more_peer_review)
  public void onPeerReviewClicked() {
    ReviewActivity.startYourself(getActivity(), getString(R.string.title_activity_review));
  }

  private class BaseLogoutListener implements CommonsApplication.LogoutListener {
    @Override
    public void onLogoutComplete() {
      Timber.d("Logout complete callback received.");
      Intent nearbyIntent = new Intent(
          getContext(), LoginActivity.class);
      nearbyIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
      nearbyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(nearbyIntent);
      getActivity().finish();
    }
  }
}

