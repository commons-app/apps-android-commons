package fr.free.nrw.commons.contributions;

import fr.free.nrw.commons.BasePresenter;

/**
 * The contract for Contributions View & Presenter
 */
public class ContributionsContract {

  public interface View {

    void showMessage(String localizedMessage);
  }

  public interface UserActionListener extends BasePresenter<ContributionsContract.View> {

    Contribution getContributionsWithTitle(String uri);

    void deleteUpload(Contribution contribution);

    void updateContribution(Contribution contribution);

    void fetchMediaDetails(Contribution contribution);
  }
}
