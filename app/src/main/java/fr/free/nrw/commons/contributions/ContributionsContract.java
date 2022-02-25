package fr.free.nrw.commons.contributions;

import android.content.Context;
import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.data.models.Contribution;

/**
 * The contract for Contributions View & Presenter
 */
public class ContributionsContract {

    public interface View {

        void showMessage(String localizedMessage);

        Context getContext();
    }

    public interface UserActionListener extends BasePresenter<ContributionsContract.View> {

        Contribution getContributionsWithTitle(String uri);

        void deleteUpload(Contribution contribution);

        void saveContribution(Contribution contribution);
    }
}
