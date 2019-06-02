package fr.free.nrw.commons.upload.depicts;

import fr.free.nrw.commons.BasePresenter;

public interface DepictsContract {

    interface View {
        void goToNextScreen();
    }

    interface UserActionListener extends BasePresenter<View> {
        void onNextButtonPressed();
    }
}
