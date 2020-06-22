package fr.free.nrw.commons.contributions;

import fr.free.nrw.commons.BasePresenter;

/**
 * The contract for Contributions list View & Presenter
 */
public class ContributionsListContract {

  public interface View {

  }

  public interface UserActionListener extends BasePresenter<View> {

    void deleteUpload(Contribution contribution);
  }
}
