package fr.free.nrw.commons.navtab;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import fr.free.nrw.commons.R;

public class MoreBottomSheetFragment extends BottomSheetDialogFragment {

  @BindView(R.id.more_about)
  TextView moreAbout;
  @BindView(R.id.more_achievements)
  TextView moreAchievements;
  @BindView(R.id.more_feedback)
  TextView moreFeedback;
  @BindView(R.id.more_logout)
  TextView moreLogout;
  @BindView(R.id.more_peer_review)
  TextView morePeerReview;
  @BindView(R.id.more_settings)
  TextView moreSettings;
  @BindView(R.id.more_tutorial)
  TextView moreTutorial;

  @Nullable
  @Override
  public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    View view = inflater.inflate(R.layout.more_bottom_sheet, container, false);
    ButterKnife.bind(this, view);
    return view;
  }

  @OnClick(R.id.more_achievements)
  public void launchAchievements(View view) {
    Log.d("deneme70","1");
  }

  @OnClick(R.id.more_peer_review)
  public void launchPeerReview(View view) {
    Log.d("deneme70","2");
  }

  @OnClick(R.id.more_settings)
  public void launchMoreSettings(View view) {
    Log.d("deneme70","3");
  }

  @OnClick(R.id.more_tutorial)
  public void launchMoreTutorial(View view) {
    Log.d("deneme70","4");
  }

  @OnClick(R.id.more_feedback)
  public void launchMoreFeedback(View view) {

  }

  @OnClick(R.id.more_about)
  public void launchMoreAbout(View view) {

  }

  @OnClick(R.id.more_logout)
  public void launchMoreLogout(View view) {

  }

}
