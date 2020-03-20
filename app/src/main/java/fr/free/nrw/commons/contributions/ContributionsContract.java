package fr.free.nrw.commons.contributions;

import java.util.List;

import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.Media;

/**
 * The contract for Contributions View & Presenter
 */
public class ContributionsContract {

    public interface View {

        void showWelcomeTip(boolean numberOfUploads);

        void showProgress(boolean shouldShow);

        void showNoContributionsUI(boolean shouldShow);

        void setUploadCount(int count);

        void showContributions(List<Contribution> contributionList);

        void showMessage(String localizedMessage);
    }

    public interface UserActionListener extends BasePresenter<ContributionsContract.View> {

        Contribution getContributionsWithTitle(String uri);

        void deleteUpload(Contribution contribution);

        Media getItemAtPosition(int i);

        void updateContribution(Contribution contribution);

        void fetchMediaDetails(Contribution contribution);
    }
}
