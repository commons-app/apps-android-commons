package fr.free.nrw.commons.upload;

import fr.free.nrw.commons.BasePresenter;
import java.util.List;

public interface IUpload {

    public interface View {

        boolean isLoggedIn();

        void finish();

        void launchMapActivity(String decCoords);

        void askUserToLogIn();

        void showProgress(boolean shouldShow);

        void showMessage(int messageResourceId);
    }

    public interface UserActionListener extends BasePresenter<View> {

        void handleSubmit();

        List<String> getImageTitleList();
    }
}
