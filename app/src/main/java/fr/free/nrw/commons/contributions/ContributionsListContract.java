package fr.free.nrw.commons.contributions;

import fr.free.nrw.commons.BasePresenter;

/**
 * The contract for Contributions list View & Presenter
 */
public class ContributionsListContract {

    public interface View {

        void showWelcomeTip(boolean numberOfUploads);

        void showProgress(boolean shouldShow);

        void showNoContributionsUI(boolean shouldShow);
    }

    public interface UserActionListener extends BasePresenter<View> {
    }
}
