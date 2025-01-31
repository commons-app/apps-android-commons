package fr.free.nrw.commons.contributions;

import android.content.Context;
import fr.free.nrw.commons.BasePresenter;

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

    }
}
