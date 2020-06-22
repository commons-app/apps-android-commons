package fr.free.nrw.commons.contributions;

import fr.free.nrw.commons.BasePresenter;

/**
 * The contract for Contributions View & Presenter
 */
public class ContributionsContract {

    public interface View {
    }

    public interface UserActionListener extends BasePresenter<ContributionsContract.View> {

        Contribution getContributionsWithTitle(String uri);

        void deleteUpload(Contribution contribution);

    }
}
