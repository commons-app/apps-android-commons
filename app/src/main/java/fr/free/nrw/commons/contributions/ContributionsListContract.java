package fr.free.nrw.commons.contributions;

import fr.free.nrw.commons.BasePresenter;
import java.util.List;

/**
 * The contract for Contributions View & Presenter
 */
public class ContributionsListContract {

  public interface View {

    void showWelcomeTip(boolean numberOfUploads);

    void showProgress(boolean shouldShow);

    void showNoContributionsUI(boolean shouldShow);

    void showContributions(List<Contribution> contributionList);
  }

  public interface UserActionListener extends BasePresenter<View> {

    void deleteUpload(Contribution contribution);
  }
}
