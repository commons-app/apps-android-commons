package fr.free.nrw.commons.upload.depicts;

import fr.free.nrw.commons.BasePresenter;

public interface DepictsContract {

    interface View {
        void goToNextScreen();

        void goToPreviousScreen();
    }

    interface UserActionListener extends BasePresenter<View> {
        void onNextButtonPressed();

        void onPreviousButtonClicked();
    }
}
